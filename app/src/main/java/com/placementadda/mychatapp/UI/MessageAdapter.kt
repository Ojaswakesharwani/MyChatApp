package com.placementadda.mychatapp.UI

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.placementadda.mychatapp.R
import com.placementadda.mychatapp.databinding.ItemMessageBinding
import java.io.IOException


@Suppress("DEPRECATION")
class MessageAdapter() : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messages =
        mutableListOf<Pair<String, Boolean>>() // Pair<String, Boolean> to store message and isSent flag
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentUrl = ""
    private var currentPlayingViewHolder: MessageViewHolder? = null
    lateinit var progressBar: ProgressDialog

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val (message, isSent) = messages[position]
        holder.bind(message, isSent)
    }

    override fun getItemCount(): Int = messages.size

    fun submitList(newMessages: List<Pair<String, Boolean>>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val handler = Handler(Looper.getMainLooper())


        fun bind(message: String, isSent: Boolean) {
            val context = binding.root.context

            if (message.contains("http")) { // Assuming URL is provided for image or audio messages
                //TODO :if message is image
                if (message.contains("jpg")) {
                    binding.messageTextView.visibility = View.GONE
                    binding.ivImage.visibility = View.VISIBLE
                    binding.audioContainer.visibility = View.GONE
                    Glide.with(binding.root.context)
                        .load(message)
                        .into(binding.ivImage)
                }
                //TODO :if message is audio
                else {
                    binding.messageTextView.visibility = View.GONE
                    binding.ivImage.visibility = View.GONE
                    binding.audioContainer.visibility = View.VISIBLE

                    //val audioUrl = message.substring(7)
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setAudioStreamType(AudioManager.STREAM_MUSIC)
                        }
                    }

                    val audioUrl = message.substring(7)
                    mediaPlayer!!.reset()
                    mediaPlayer!!.setDataSource(audioUrl)
                    mediaPlayer!!.prepareAsync()

                    mediaPlayer!!.setOnPreparedListener {
                        val totalDuration = mediaPlayer!!.duration / 1000
                        val minutes = totalDuration / 60
                        val seconds = totalDuration % 60
                        binding.tvAudioDuration.text = String.format("%d:%02d", minutes, seconds)

                        binding.seekBarAudio.max =
                            mediaPlayer!!.duration // Set SeekBar max to audio duration
                        binding.seekBarAudio.progress = 0 // Reset SeekBar
                         }


                    binding.ivplayPause.setOnClickListener {
                        if (currentUrl != audioUrl) {
                            // Stop the currently playing audio
                            currentPlayingViewHolder?.stopAudioPlayback()

                            // Play the new audio
                            currentUrl = audioUrl
                            currentPlayingViewHolder = this@MessageViewHolder
                            // progressBar.show()
                            playAudio(audioUrl)

                        } else {
                            // Toggle play/pause for the same audio
                            if (isPlaying) {
                                pauseAudio()
                            } else {
                                // progressBar.show()
                                playAudio(audioUrl)
                            }
                        }
                    }
                }

                //TODO :Open image
                binding.ivImage.setOnClickListener {
                    val intent = Intent(context, ViewImageActivity::class.java).apply {
                        putExtra("IMAGE_URL", message)
                    }
                    context.startActivity(intent)
                }

            }
            //TODO :If message is text
            else {
                binding.messageTextView.visibility = View.VISIBLE
                binding.ivImage.visibility = View.GONE
                binding.audioContainer.visibility = View.GONE
                binding.messageTextView.text = message
            }

            //TODO :Set layout params
            val layoutParams = binding.parentLayout.layoutParams as ViewGroup.MarginLayoutParams
            if (isSent) {
                layoutParams.setMargins(80, 0, 0, 0)
            } else {
                layoutParams.setMargins(0, 0, 0, 50)
            }
            binding.parentLayout.layoutParams = layoutParams

            //TODO :Set message Background programmatically
            val background = if (isSent) {
                context.getDrawable(R.drawable.sent_message_background)
            } else {
                context.getDrawable(R.drawable.recieved_message_background)
            }
            binding.messageContainer.background = background

            //TODO : Set Audio Background programmatically
            val audioBackground = if (isSent) {
                context.getDrawable(R.drawable.sent_message_background)
            } else {
                context.getDrawable(R.drawable.recieved_message_background)
            }
            binding.audioContainer.background = audioBackground

            // Fixme : Set Gravity programmatically
            val gravity = if (isSent) Gravity.END else Gravity.START
            binding.parentLayout.gravity = gravity
        }

        private fun playAudio(audioUrl: String) {
            val preparingAudio = ProgressDialog(binding.root.context)
            preparingAudio.setMessage("Preparing audio...")
            preparingAudio.setCancelable(false)
            try {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioStreamType(AudioManager.STREAM_MUSIC)
                    }
                }
                // Show the ProgressDialog while the audio is preparing
                preparingAudio.show()

                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(audioUrl)
                mediaPlayer?.setOnPreparedListener {
                    preparingAudio.dismiss()
                    mediaPlayer?.start()

                    // Set the duration after preparation
                    val totalDuration = mediaPlayer!!.duration / 1000
                    val minutes = totalDuration / 60
                    val seconds = totalDuration % 60
                    binding.tvAudioDuration.text = String.format("%d:%02d", minutes, seconds)

                    binding.ivplayPause.setImageResource(R.drawable.icon_pause)
                    isPlaying = true
                    //Toast.makeText(binding.root.context, "Audio start playing", Toast.LENGTH_SHORT).show()

                    initializeSeekBar()

                    mediaPlayer?.setOnCompletionListener {

                        binding.ivplayPause.setImageResource(R.drawable.icon_play)
                        isPlaying = false
                        handler.removeCallbacksAndMessages(null)
                        binding.seekBarAudio.progress = 0
                        // Toast.makeText(binding.root.context, "Audio played completely", Toast.LENGTH_SHORT).show()
                    }
                }
                mediaPlayer?.prepareAsync() // Prepare asynchronously
            } catch (e: IOException) {
                Log.e(TAG, "playAudio: ${e.message}")
            }
        }

        private fun pauseAudio() {
            mediaPlayer?.pause()
            binding.ivplayPause.setImageResource(R.drawable.icon_play)
            isPlaying = false
            // Toast.makeText(binding.root.context, "Audio paused", Toast.LENGTH_SHORT).show()
        }

        fun stopAudioPlayback() {
            mediaPlayer?.stop()
            binding.ivplayPause.setImageResource(R.drawable.icon_play)
            isPlaying = false
            handler.removeCallbacksAndMessages(null)
            binding.seekBarAudio.progress = 0
            currentUrl = ""
            // Toast.makeText(binding.root.context, "Audio Stopped", Toast.LENGTH_SHORT).show()
        }

        //TODO: Initialize seekbar
        private fun initializeSeekBar() {
            val updateSeekBar = object : Runnable {
                override fun run() {
                    mediaPlayer?.let {
                        binding.seekBarAudio.progress = it.currentPosition
                        if (it.isPlaying) {
                            handler.postDelayed(this, 1000)
                        }
                    }
                }
            }
            handler.post(updateSeekBar)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

