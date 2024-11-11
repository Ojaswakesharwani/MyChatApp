package com.placementadda.mychatapp.UI

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.placementadda.mychatapp.R

class ViewImageActivity : AppCompatActivity() {
    private lateinit var imageUrl: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_image)

        // Get the image URL from the intent
        imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""

        // Set up your ImageView to display the image
        val imageView: ImageView = findViewById(R.id.fullImage)
        Glide.with(this)
            .load(imageUrl)
            .into(imageView)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}