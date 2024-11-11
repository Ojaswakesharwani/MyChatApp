package com.placementadda.mychatapp.UI

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.placementadda.mychatapp.R
import com.placementadda.mychatapp.databinding.ActivityLoginBinding
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding.mcvSendOtp.setOnClickListener {
            val phoneNumber = binding.etNumber.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                sendOtp(phoneNumber)
            } else {
                Toast.makeText(this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun sendOtp(num: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            num,
            60,
            TimeUnit.SECONDS,
            this,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Sign in with the credential
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Sign-in successful
                                Toast.makeText(this@LoginActivity, "Sign in successful", Toast.LENGTH_SHORT).show()
                                // Proceed to next activity
                                startActivity(Intent(this@LoginActivity, OtpActivity::class.java))
                                finish()
                            } else {
                                // Sign-in failed
                                Toast.makeText(this@LoginActivity, "Sign-in failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Verification failed
                    Toast.makeText(this@LoginActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "onVerificationFailed: ${e.message}", )
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verificationId, token)
                    // Save verification ID and resending token for later use
                    val intent = Intent(this@LoginActivity, OtpActivity::class.java).apply {
                        putExtra("verificationId", verificationId)
                        putExtra("resendToken", token)
                        putExtra("phoneNumber", num)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        )
    }

}