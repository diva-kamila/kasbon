package com.example.kasbon

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.kasbon.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

// Halaman induk: menampung 3 tab navigasi bawah (Beranda, Ringkasan, Profil)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Permintaan izin notifikasi. Kalau ditolak, aplikasi tetap berjalan normal.
    private val izinNotifikasi =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kalau sesi sudah tidak ada, kembali ke halaman login
        if (FirebaseAuth.getInstance().currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Supaya konten tidak tertutup status bar dan tombol navigasi
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_daftar -> tampilkan(DaftarFragment())
                R.id.nav_ringkasan -> tampilkan(RingkasanFragment())
                R.id.nav_profil -> tampilkan(ProfilFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        // Tab pertama yang tampil
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_daftar
        }

        siapkanPengingat()
    }

    // Minta izin notifikasi (Android 13+) dan jadwalkan pengecekan harian
    private fun siapkanPengingat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Pengingat.izinDiberikan(this)) {
            izinNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Pengingat.jadwalkan(this)
    }

    private fun tampilkan(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
