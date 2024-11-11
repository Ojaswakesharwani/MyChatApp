package com.placementadda.mychatapp.UI

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.messaging.FirebaseMessaging
import com.placementadda.mychatapp.HomeActivity
import com.placementadda.mychatapp.R
import com.placementadda.mychatapp.databinding.ActivityOtpBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var  registrationToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Assuming you pass the resend token from the previous activity
        resendToken = intent.getParcelableExtra("resendToken")

        // Get verification ID from the intent
        verificationId = intent.getStringExtra("verificationId")

        // Start countdown timer
        startCountdownTimer()

        val otpFields = listOf(
            binding.Otp1,
            binding.Otp2,
            binding.Otp3,
            binding.Otp4,
            binding.Otp5,
            binding.Otp6
        )

        otpFields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < otpFields.size - 1) {
                        otpFields[index + 1].requestFocus()
                    } else if (s?.isEmpty() == true && index > 0) {
                        otpFields[index - 1].requestFocus()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }


        binding.ConfirmOtpBtn.setOnClickListener {
            val otpCode = otpFields.joinToString("") { it.text.toString() }
            if (otpCode.length == 6 && verificationId != null) {
                verifyOtp(verificationId!!, otpCode)
            } else {
                Toast.makeText(this, "Please enter a valid OTP.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ResendOtp.setOnClickListener {
            resendVerificationCode()
        }

    }

    private fun startCountdownTimer() {
        val timer = object : CountDownTimer(30000, 1000) { // 30 seconds timer
            override fun onTick(millisUntilFinished: Long) {
                binding.ResendOtp.text = "Resend OTP after ${millisUntilFinished / 1000} seconds"
                binding.ResendOtp.isEnabled = false
            }

            override fun onFinish() {
                binding.ResendOtp.text = "Resend OTP"
                binding.ResendOtp.isEnabled = true
            }
        }
        timer.start()
    }

    private fun resendVerificationCode() {
        if (resendToken != null) {
            // Use the resend token to request another verification code
            val phoneNumber =
                intent.getStringExtra("phoneNumber") // Get phone number from intent or saved state
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber!!,
                60,
                java.util.concurrent.TimeUnit.SECONDS,
                this,
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Sign in with the credential
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        this@OtpActivity,
                                        "Sign in successful",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Proceed to next activity
                                    startActivity(
                                        Intent(
                                            this@OtpActivity,
                                            HomeActivity::class.java
                                        )
                                    )
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@OtpActivity,
                                        "Sign-in failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Toast.makeText(
                            this@OtpActivity,
                            "Verification failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        super.onCodeSent(verificationId, token)
                        this@OtpActivity.verificationId = verificationId
                        resendToken = token
                        startCountdownTimer()
                    }
                },
                resendToken
            )
        } else {
            Toast.makeText(
                this,
                "Resend token is null. Please wait or retry later.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun verifyOtp(verificationId: String, otpCode: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
                    //sign in successfull
                    // Retrieve the registration token
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            registrationToken = tokenTask.result
                            // Add user to Firestore
                            addUserToFirestore(phoneNumber)
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to retrieve registration token",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Sign-in failed
                    Toast.makeText(
                        this,
                        "Sign-in failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun addUserToFirestore(phoneNumber: String) {
        val userId = auth.currentUser?.uid ?: return // Ensure you get the current user ID
        val userRef = firestore.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val currentTime = System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentTimeString = sdf.format(Date(currentTime))

                val user = User(
                    id = userId,
                    phoneNumber = phoneNumber,
                    createdAt = currentTimeString,
                    token = registrationToken?:"Token not found"
                )

                /*val user = hashMapOf(
                    "id" to userId,
                    "phoneNumber" to phoneNumber,
                    "createdAt" to currentTimeString,
                    "registrationToken" to (registrationToken ?: "Token not available")
                )*/


                userRef.set(user)
                    .addOnSuccessListener {
                        // User added successfully
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error adding user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // User already exists, proceed to next activity
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }

}