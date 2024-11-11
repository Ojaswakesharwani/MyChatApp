package com.placementadda.mychatapp.UI

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.placementadda.mychatapp.R
import com.placementadda.mychatapp.databinding.ActivityChatBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.log


@Suppress("DEPRECATION")
class ChatActivity : AppCompatActivity() {
    lateinit var binding: ActivityChatBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageListener: ListenerRegistration
    private lateinit var currentUserId: String
    private lateinit var otherUserId: String
    private lateinit var currentUserName: String
    private lateinit var currentUserToken: String
    private lateinit var otherUserToken: String
    private lateinit var conversationId: String
    private var imageUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""
    private var isRecording=false

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startRecording()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("SuspiciousIndentation")
    private val galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data?.data
                imageUri?.let {
                    uploadImageAndSendMessage(it)  // Upload the image and send the message
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        currentUserName = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""

        // Retrieve the FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                currentUserToken = task.result ?: ""
                Log.d(TAG, "FCM Token: $currentUserToken")
            } else {
                Log.e(TAG, "Failed to get FCM Token", task.exception)
            }
        }

        // Get the other user's ID and Token from the intent
        otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: ""
        otherUserToken = intent.getStringExtra("OTHER_USER_TOKEN") ?: ""

        // Generate a unique conversation ID
        conversationId = generateConversationId(currentUserId, otherUserId)


        Log.e(TAG, "onCreate: $currentUserId , $otherUserId")

        val phoneNumber = intent.getStringExtra("USER_PHONE_NUMBER")
        binding.PhoneNumber.text = phoneNumber

        messageAdapter = MessageAdapter()
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMessages.adapter = messageAdapter

        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.ivSendImage.setOnClickListener {
            openGallery()
        }

        binding.ivSendMessage.setOnClickListener {
            //send message
            sendMessage()
            binding.ivSendImage.isEnabled = false
        }
        loadMessages()


        val progressDialogBox = Dialog(this)
        val dialogView = layoutInflater.inflate(R.layout.item_custom_progress_bar, null)
        progressDialogBox.setContentView(dialogView)
        progressDialogBox.setCancelable(true) // Optional, to prevent user from canceling it
        progressDialogBox.window?.setBackgroundDrawableResource(android.R.color.transparent) // To remove default dialog background



            binding.ivmic.setOnClickListener {
                Toast.makeText(this,"Hold to record audio",Toast.LENGTH_SHORT).show()
                isRecording=false
                progressDialogBox.dismiss()
            }

           binding.ivmic.setOnLongClickListener {
               progressDialogBox.show()
               if (mediaRecorder == null){
                   checkAndRequestAudioPermission()
               }
               else{
                   isRecording=true
                   startRecording()
               }

               if (isRecording) {
                   binding.ivmic.setOnTouchListener { v, event ->
                       when (event.action) {
                           MotionEvent.ACTION_UP -> {
                               // Add your action here when the button is released
                               isRecording=false
                               stopRecording()
                               progressDialogBox.dismiss()
                               true
                           }

                           else -> false
                       }
                   }
               }


               false
           }

        /* if (isRecording){
             binding.ivmic.setOnTouchListener{v , event ->
                 when (event.action) {
                     MotionEvent.ACTION_UP -> {
                         // Add your action here when the button is released
                         isRecording=false
                         stopRecording()
                         progressDialogBox.dismiss()
                         true
                     }
                     else -> false
                 }
             }
         }*/


    }

    private fun generateConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString()
        Log.e(TAG, "sendMessage: $message")
        binding.etMessage.text.clear()
        CoroutineScope(Dispatchers.Default).launch {
            if (message.isNotBlank()) {
                val messageData = hashMapOf(
                    "text" to message,
                    "timestamp" to System.currentTimeMillis(),
                    "senderId" to currentUserId,  // Include the sender's ID
                    "receiverId" to otherUserId   // Include the receiver's ID
                )

                firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .add(messageData)
                    .addOnSuccessListener {
                        // binding.etMessage.text.clear()
                        // Send notification to recipient
                        GlobalScope.launch {
                            sendNotificationToRecipient(message, otherUserToken, currentUserName)
                        }
                        binding.ivSendImage.isEnabled = true
                    }
                    .addOnFailureListener { e ->
                        // Handle the error
                    }
            }
        }
    }

    private fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {

            try {
                messageListener = firestore.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestamp")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            // Handle the error
                            Log.e(TAG, "loadMessages: ", e)
                            return@addSnapshotListener
                        }


                        if (snapshot != null) {
                            val messages = snapshot.documents.map { doc ->
                                val text = doc.getString("text") ?: ""
                                val imageUrl = doc.getString("imageUrl") ?: ""
                                val senderId = doc.getString("senderId") ?: ""
                                val audioUrl = doc.getString("audioUrl") ?: ""

                                // Determine the type of message and return appropriate content
                                val message = when {
                                    audioUrl.isNotEmpty() -> "Audio: $audioUrl"
                                    imageUrl.isNotEmpty() -> imageUrl
                                    else -> text
                                }
                                // Determine if the message is sent or received
                                Pair(message, senderId == currentUserId)
                            }

                            messageAdapter.submitList(messages)
                            // Scroll to the last message
                            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                        }

                    }
            } catch (e: IOException) {
                Log.e(TAG, "loadMessages: ${e.message}")
            }
        }

    }

    fun sendNotificationToRecipient(message: String, recipientToken: String, title: String) {
        val url = URL("https://fcm.googleapis.com/v1/projects/mychatapp-187d3/messages:send")
            val accessToken = AccessTokenUtil().getAccessToken()


            Log.e(TAG, "sendNotificationToRecipient: started $recipientToken")

            val json = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", recipientToken)
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", message)
                    })
                })
            }

            Log.e(TAG, "sendNotificationToRecipient: json: $json")

            Thread {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json; UTF-8")
                    doOutput = true

                    outputStream.use { os ->
                        val input = json.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        inputStream.bufferedReader().use {
                            val response = it.readText()
                            Log.d(TAG, "Response: $response")
                        }
                    } else {
                        errorStream.bufferedReader().use {
                            val errorResponse = it.readText()
                            Log.e(TAG, "Error Response: $errorResponse")
                        }
                    }
                }
            }.start()

    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        messageListener.remove()
    }

    private fun uploadImageAndSendMessage(uri: Uri) {
        Log.e(TAG, "uploadImageAndSendMessage: $uri")
        val progressDialogBox = ProgressDialog(this)
        progressDialogBox.setTitle("Uploading...")
        progressDialogBox.setMessage("Uploading your image...")
        progressDialogBox.show()
        progressDialogBox.setCancelable(false)

        // Create a reference to Firebase Storage
        val storageReference = FirebaseStorage.getInstance().reference
        val fileReference = storageReference.child("images/${System.currentTimeMillis()}.jpg")

        // Upload the image to Firebase Storage
        fileReference.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                // Get the download URL after successful upload
                progressDialogBox.dismiss()
                fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    Log.e(TAG, "Image URL: $imageUrl")
                    // Send the image message with the download URL
                    sendImageMessage(imageUrl)
                }.addOnFailureListener { e ->
                    // Handle failure to get download URL
                    Log.e(TAG, "Failed to get download URL: ${e.message}")
                    Toast.makeText(
                        this,
                        "Failed to get download URL: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                // Handle failed upload
                progressDialogBox.dismiss()
                Log.e(TAG, "Failed to upload image: ${e.message}")
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun sendImageMessage(imageUrl: String) {
        Log.e(TAG, "sendImageMessage: $imageUrl")
        val messageData = hashMapOf(
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis(),
            "senderId" to currentUserId,
            "receiverId" to otherUserId
        )

        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                binding.ivSendImage.isEnabled = true
                GlobalScope.launch {
                    Log.e(TAG, "sendImageMessage: Url :$imageUrl")
                    sendNotificationToRecipient(imageUrl, otherUserToken, currentUserName)
                }
                Log.d(TAG, "Image message sent successfully")
            }
            .addOnFailureListener { e ->
                // Handle the error
                Log.e(TAG, "sendImageMessage: ${e.message}")
            }
    }


    private fun sendAudioMessage(audioUrl: String) {
        Log.e(TAG, "sendAudioMessage: $audioUrl")
        val messageData = hashMapOf(
            "audioUrl" to audioUrl,
            "timestamp" to System.currentTimeMillis(),
            "senderId" to currentUserId,
            "receiverId" to otherUserId
        )

        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                GlobalScope.launch {
                    sendNotificationToRecipient("Audio message", otherUserToken, currentUserName)
                }
                Log.d(TAG, "Audio message sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send audio message: ${e.message}")
            }
    }

    private fun uploadAudioAndSendMessage() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Uploading...")
        progressDialog.setMessage("Uploading your audio...")
        progressDialog.show()

        val storageReference = FirebaseStorage.getInstance().reference
        val audioRef =
            storageReference.child("audios/${System.currentTimeMillis()}.mp3")  // or .mpeg

        val fileUri = Uri.fromFile(File(audioFilePath))
        val metadata = storageMetadata {
            contentType = "audio/mp3"  // Correct MIME type for audio
        }

        audioRef.putFile(fileUri, metadata)
            .addOnSuccessListener {
                progressDialog.dismiss()
                audioRef.downloadUrl.addOnSuccessListener { downloadUri ->

                    sendAudioMessage(downloadUri.toString())
                    audioFilePath = ""
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get audio download URL: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e(TAG, "Failed to upload audio: ${e.message}")
            }
    }


    private fun startRecording() {
        val fileName = "${UUID.randomUUID()}.mp3"
        audioFilePath = "${externalCacheDir?.absolutePath}/$fileName"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(TAG, "startRecording: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                Log.d(TAG, "Recording stopped")
            } catch (e: RuntimeException) {
                Log.e(TAG, "Error stopping media recorder: ${e.message}")
            } finally {
                reset()
                release()
                mediaRecorder = null
            }
        }

        if (audioFilePath.isNotEmpty()) {
            uploadAudioAndSendMessage()
        } else {
            Log.e(TAG, "No audio file to upload")
        }
    }


    private fun checkAndRequestAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
              }

            else -> {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
        }
    }
}