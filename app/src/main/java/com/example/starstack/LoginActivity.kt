package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.starstack.databinding.ActivityLoginBinding
import com.example.starstack.firebase.FirebaseManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        if (firebaseManager.getCurrentUser() != null) {
            navigateToMain()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginWithEmail(email, password)
            }
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
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

        return true
    }

    private fun loginWithEmail(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                firebaseManager.signInWithEmail(email, password)
                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    showLoading(true)
                    lifecycleScope.launch {
                        try {
                            firebaseManager.signInWithGoogle(token)
                            Toast.makeText(
                                this@LoginActivity,
                                "Google sign-in successful!",
                                Toast.LENGTH_SHORT
                            ).show()
                            navigateToMain()
                        } catch (e: Exception) {
                            showLoading(false)
                            Toast.makeText(
                                this@LoginActivity,
                                "Google sign-in failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(
                    this,
                    "Google sign-in failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnGoogleSignIn.isEnabled = !show
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}