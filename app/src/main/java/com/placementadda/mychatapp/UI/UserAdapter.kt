package com.placementadda.mychatapp.UI

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.placementadda.mychatapp.R

class UserAdapter(private val userList: List<User>,private val context: Context) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userPhoneNumber: TextView = itemView.findViewById(R.id.UserPhoneNumber)
       // val userCreatedAt: TextView = itemView.findViewById(R.id.userCreatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.userPhoneNumber.text = user.phoneNumber
       // holder.userCreatedAt.text = user.createdAt

        holder.userPhoneNumber.setOnClickListener {
            Log.e(TAG, "onBindViewHolder: User = $user", )
            val intent=Intent(context,ChatActivity::class.java)
            intent.putExtra("USER_PHONE_NUMBER", user.phoneNumber)
            intent.putExtra("OTHER_USER_ID", user.id)  // Pass the other user ID
            intent.putExtra("OTHER_USER_TOKEN", user.token)
            Log.e(TAG, "onBindViewHolder: reciever token : ${user.token}", )
            // Create options for the transition animation


            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }
}