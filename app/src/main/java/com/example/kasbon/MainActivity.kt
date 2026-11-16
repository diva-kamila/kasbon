package com.example.kasbon

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kasbon.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

// Halaman utama: daftar catatan hutang/piutang
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CatatanAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kalau sesi sudah tidak ada, kembali ke halaman login
        if (FirebaseAuth.getInstance().currentUser == null) {
            keLogin()
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

        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                FirebaseAuth.getInstance().signOut()
                keLogin()
                true
            } else {
                false
            }
        }

        adapter = CatatanAdapter(
            onKlik = { catatan -> startActivity(FormCatatanActivity.buatIntent(this, catatan)) },
            onHapus = { catatan -> konfirmasiHapus(catatan) }
        )
        binding.rvCatatan.layoutManager = LinearLayoutManager(this)
        binding.rvCatatan.adapter = adapter

        binding.fabTambah.setOnClickListener {
            startActivity(FormCatatanActivity.buatIntent(this, null))
        }
        binding.btnCobaLagi.setOnClickListener { muatData() }
    }

    override fun onStart() {
        super.onStart()
        if (::binding.isInitialized) muatData()
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun muatData() {
        tampilkanLoading()
        listener?.remove()
        listener = CatatanRepository.dengarkan(
            onHasil = { daftar -> tampilkanData(daftar) },
            onGagal = { tampilkanError() }
        )
    }

    private fun tampilkanLoading() {
        binding.tvOffline.visibility = if (Jaringan.isOnline(this)) View.GONE else View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
        binding.rvCatatan.visibility = View.GONE
        binding.tvKosong.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    private fun tampilkanData(daftar: List<Catatan>) {
        binding.tvOffline.visibility = if (Jaringan.isOnline(this)) View.GONE else View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        if (daftar.isEmpty()) {
            binding.tvKosong.visibility = View.VISIBLE
            binding.rvCatatan.visibility = View.GONE
        } else {
            binding.tvKosong.visibility = View.GONE
            binding.rvCatatan.visibility = View.VISIBLE
        }
        adapter.submitList(daftar)
    }

    private fun tampilkanError() {
        binding.tvOffline.visibility = if (Jaringan.isOnline(this)) View.GONE else View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.rvCatatan.visibility = View.GONE
        binding.tvKosong.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
    }

    private fun konfirmasiHapus(catatan: Catatan) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_hapus_judul)
            .setMessage(getString(R.string.dialog_hapus_pesan, catatan.nama))
            .setNegativeButton(R.string.btn_batal, null)
            .setPositiveButton(R.string.btn_hapus) { _, _ -> hapus(catatan) }
            .show()
    }

    private fun hapus(catatan: Catatan) {
        if (!Jaringan.isOnline(this)) {
            Toast.makeText(this, R.string.error_offline, Toast.LENGTH_LONG).show()
            return
        }
        CatatanRepository.hapus(
            catatan.id,
            onBerhasil = { Toast.makeText(this, R.string.toast_dihapus, Toast.LENGTH_SHORT).show() },
            onGagal = { Toast.makeText(this, R.string.error_gagal_hapus, Toast.LENGTH_LONG).show() }
        )
    }

    private fun keLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
