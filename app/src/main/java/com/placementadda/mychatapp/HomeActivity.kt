package com.placementadda.mychatapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.placementadda.mychatapp.UI.LoginActivity
import com.placementadda.mychatapp.UI.User
import com.placementadda.mychatapp.UI.UserAdapter

class HomeActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var firestore: FirebaseFirestore
    lateinit var refreshLayout: SwipeRefreshLayout
    private var userList: MutableList<User> = mutableListOf()
    private lateinit var currentUserPhoneNumber: String
    private lateinit var logout:ImageView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logout=findViewById(R.id.ivLogout)
        recyclerView = findViewById(R.id.chatRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        refreshLayout=findViewById(R.id.swipeRefreshLayout)

        firestore = FirebaseFirestore.getInstance()
        currentUserPhoneNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""

        fetchUsers()

        logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        refreshLayout.setOnRefreshListener {
            recreate()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut().also {
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchUsers() {
        refreshLayout.isRefreshing=true
        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                userList.clear() // Clear the list before adding new users

                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    // Only add users that are not the current user
                    if (user.phoneNumber != currentUserPhoneNumber) {
                        userList.add(user)
                    }
                }
                userAdapter = UserAdapter(userList,this)
                recyclerView.adapter = userAdapter

                // Stop the refreshing animation
                refreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                // Handle any errors here
                // Stop the refreshing animation
                refreshLayout.isRefreshing = false
            }
    }

}