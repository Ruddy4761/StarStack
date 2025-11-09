package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.starstack.databinding.ActivitySignUpBinding
import com.example.starstack.firebase.FirebaseManager
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val firebaseManager = FirebaseManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                signUp(name, email, password)
            }
        }

        binding.tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun signUp(name: String, email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                firebaseManager.signUpWithEmail(email, password, name)
                Toast.makeText(
                    this@SignUpActivity,
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToMain()
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@SignUpActivity,
                    "Sign up failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !show
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}