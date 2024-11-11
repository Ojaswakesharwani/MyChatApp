package com.placementadda.mychatapp.UI

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.google.firebase.auth.FirebaseAuth
import com.placementadda.mychatapp.HomeActivity
import com.placementadda.mychatapp.R
import java.util.Timer
import java.util.TimerTask

class SplashScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        Timer().schedule(object : TimerTask() {
            override fun run() {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // User is signed in, navigate to the HomeActivity
                    startActivity(Intent(this@SplashScreen, HomeActivity::class.java))
                } else {
                    // No user is signed in, navigate to the LoginActivity
                    startActivity(Intent(this@SplashScreen, LoginActivity::class.java))
                }
                finish() // Close the splash activity
            }
        },3000)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

    }
}