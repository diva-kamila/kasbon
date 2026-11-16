package com.example.kasbon // ganti sesuai package proyekmu

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kasbon.databinding.ActivityRegisterBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvToLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirm.text.toString()

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirm.error = null

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_name_empty)
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            return
        }
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            return
        }
        if (password != confirm) {
            binding.tilConfirm.error = getString(R.string.error_password_mismatch)
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                // Simpan nama tampilan, lalu masuk ke halaman utama
                val profile = userProfileChangeRequest { displayName = name }
                result.user?.updateProfile(profile)?.addOnCompleteListener { goToMain() }
                    ?: goToMain()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, errorMessage(e), Toast.LENGTH_LONG).show()
            }
    }

    private fun errorMessage(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> getString(R.string.error_email_used)
        is FirebaseNetworkException -> getString(R.string.error_network)
        else -> getString(R.string.error_register_failed)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
