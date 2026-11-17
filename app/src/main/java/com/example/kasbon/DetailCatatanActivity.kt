package com.example.kasbon

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.kasbon.databinding.ActivityDetailCatatanBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration

// Halaman detail satu catatan: lihat, ubah, tandai lunas, hapus
class DetailCatatanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailCatatanBinding
    private var listener: ListenerRegistration? = null
    private var catatan: Catatan? = null
    private var catatanId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailCatatanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        catatanId = intent.getStringExtra(EXTRA_ID) ?: ""
        if (catatanId.isEmpty()) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnUbah.setOnClickListener {
            catatan?.let { startActivity(FormCatatanActivity.buatIntent(this, it)) }
        }
        binding.btnLunas.setOnClickListener { catatan?.let { ubahStatus(it) } }
        binding.btnHapus.setOnClickListener { catatan?.let { konfirmasiHapus(it) } }
    }

    override fun onStart() {
        super.onStart()
        binding.progressBar.visibility = View.VISIBLE
        binding.contentDetail.visibility = View.GONE
        listener = CatatanRepository.dengarkanSatu(
            catatanId,
            onHasil = { hasil ->
                if (hasil == null) {
                    finish() // catatan sudah dihapus
                } else {
                    catatan = hasil
                    tampilkan(hasil)
                }
            },
            onGagal = {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, R.string.error_detail_gagal, Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun tampilkan(c: Catatan) {
        binding.progressBar.visibility = View.GONE
        binding.contentDetail.visibility = View.VISIBLE

        binding.tvNama.text = c.nama
        binding.tvJenis.text = if (c.jenis == Catatan.JENIS_PIUTANG) {
            getString(R.string.jenis_piutang)
        } else {
            getString(R.string.jenis_hutang)
        }
        binding.tvNominal.text = Format.rupiah(c.nominal)
        binding.tvTanggalPinjam.text = getString(R.string.detail_tanggal_pinjam, Format.tanggal(c.tanggalPinjam))
        binding.tvJatuhTempo.text = getString(R.string.label_jatuh_tempo, Format.tanggal(c.jatuhTempo))
        binding.tvCatatan.text = if (c.catatan.isBlank()) {
            getString(R.string.detail_tanpa_catatan)
        } else {
            getString(R.string.detail_catatan, c.catatan)
        }

        if (c.lunas) {
            binding.tvStatus.text = getString(R.string.status_lunas)
            binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
            binding.btnLunas.text = getString(R.string.btn_tandai_belum)
        } else {
            binding.tvStatus.text = getString(R.string.status_belum_lunas)
            binding.tvStatus.setTextColor(Color.parseColor("#B3261B"))
            binding.btnLunas.text = getString(R.string.btn_tandai_lunas)
        }
    }

    private fun ubahStatus(c: Catatan) {
        if (!Jaringan.isOnline(this)) {
            Toast.makeText(this, R.string.error_offline, Toast.LENGTH_LONG).show()
            return
        }
        CatatanRepository.ubahLunas(
            c.id,
            !c.lunas,
            onBerhasil = { },
            onGagal = { Toast.makeText(this, R.string.error_gagal_ubah, Toast.LENGTH_LONG).show() }
        )
    }

    private fun konfirmasiHapus(c: Catatan) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_hapus_judul)
            .setMessage(getString(R.string.dialog_hapus_pesan, c.nama))
            .setNegativeButton(R.string.btn_batal, null)
            .setPositiveButton(R.string.btn_hapus) { _, _ -> hapus(c) }
            .show()
    }

    private fun hapus(c: Catatan) {
        if (!Jaringan.isOnline(this)) {
            Toast.makeText(this, R.string.error_offline, Toast.LENGTH_LONG).show()
            return
        }
        CatatanRepository.hapus(
            c.id,
            onBerhasil = {
                Toast.makeText(this, R.string.toast_dihapus, Toast.LENGTH_SHORT).show()
                finish()
            },
            onGagal = { Toast.makeText(this, R.string.error_gagal_hapus, Toast.LENGTH_LONG).show() }
        )
    }

    companion object {
        private const val EXTRA_ID = "id"

        fun buatIntent(context: Context, id: String): Intent {
            val intent = Intent(context, DetailCatatanActivity::class.java)
            intent.putExtra(EXTRA_ID, id)
            return intent
        }
    }
}
