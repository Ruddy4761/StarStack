package com.example.starstack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.starstack.databinding.ActivityLoginBinding
import com.example.starstack.firebase.FirebaseManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val firebaseManager = FirebaseManager.getInstance()
    private val RC_SIGN_IN = 9001
    private val TAG = "LoginActivity"

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
            binding.tilEmail.error = "Email is required"
            return false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        } else {
            binding.tilPassword.error = null
        }

        return true
    }

    private fun loginWithEmail(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                firebaseManager.signInWithEmail(email, password)
                Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } catch (e: Exception) {
                showLoading(false)
                Log.e(TAG, "Login failed", e)
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "Account not found. Please sign up."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
                    else -> e.message ?: "Login failed. Please check your connection."
                }
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun signInWithGoogle() {
        // Ensure R.string.default_web_client_id is set correctly in strings.xml!
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        // Sign out first to allow selecting a different account if needed
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    firebaseAuthWithGoogle(token)
                } ?: run {
                    Log.e(TAG, "Google Sign-In failed: ID Token is null")
                    Toast.makeText(this, "Google Sign-In error.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                // Common status code 12500 usually means wrong Web Client ID in strings.xml
                val msg = if (e.statusCode == 12500) "Configuration Error (12500). Check Web Client ID." else "Google sign-in failed: ${e.statusCode}"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                firebaseManager.signInWithGoogle(idToken)
                Toast.makeText(this@LoginActivity, "Signed in with Google!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } catch (e: Exception) {
                showLoading(false)
                Log.e(TAG, "Firebase Google Auth failed", e)
                Toast.makeText(this@LoginActivity, "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnGoogleSignIn.isEnabled = !show
        binding.tvSkip.isEnabled = !show
        binding.tvSignUp.isEnabled = !show
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}