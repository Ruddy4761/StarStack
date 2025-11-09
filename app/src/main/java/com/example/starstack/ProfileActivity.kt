package com.example.starstack


import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val profileImage = findViewById<ImageView>(R.id.profileImage)
        val userName = findViewById<TextView>(R.id.userName)
        val userEmail = findViewById<TextView>(R.id.userEmail)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        userName.text = "Rudra Thacker"
        userEmail.text = "rudra@example.com"

        logoutButton.setOnClickListener { finish() }
    }
}