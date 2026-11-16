package com.example.kasbon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.kasbon.databinding.ActivityFormCatatanBinding
import com.google.android.material.datepicker.MaterialDatePicker

// Form untuk menambah atau mengubah catatan
class FormCatatanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormCatatanBinding
    private var editId: String = ""
    private var tanggalPinjam: Long = 0L
    private var jatuhTempo: Long = 0L
    private var dibuatPada: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormCatatanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Mode ubah: isi form dengan data yang dikirim dari daftar
        editId = intent.getStringExtra(EXTRA_ID) ?: ""
        if (editId.isNotEmpty()) {
            binding.toolbar.title = getString(R.string.judul_edit)
            binding.etNama.setText(intent.getStringExtra(EXTRA_NAMA) ?: "")
            binding.etNominal.setText(intent.getLongExtra(EXTRA_NOMINAL, 0L).toString())
            if (intent.getStringExtra(EXTRA_JENIS) == Catatan.JENIS_PIUTANG) {
                binding.toggleJenis.check(R.id.btnPiutang)
            } else {
                binding.toggleJenis.check(R.id.btnHutang)
            }
            tanggalPinjam = intent.getLongExtra(EXTRA_PINJAM, 0L)
            jatuhTempo = intent.getLongExtra(EXTRA_TEMPO, 0L)
            binding.switchLunas.isChecked = intent.getBooleanExtra(EXTRA_LUNAS, false)
            binding.etCatatan.setText(intent.getStringExtra(EXTRA_CATATAN) ?: "")
            dibuatPada = intent.getLongExtra(EXTRA_DIBUAT, 0L)
        } else {
            binding.toggleJenis.check(R.id.btnHutang)
            tanggalPinjam = Format.hariIniUtc()
        }
        tampilkanTanggal()

        binding.etTanggalPinjam.setOnClickListener {
            pilihTanggal(tanggalPinjam) { tanggal ->
                tanggalPinjam = tanggal
                tampilkanTanggal()
            }
        }
        binding.etJatuhTempo.setOnClickListener {
            pilihTanggal(jatuhTempo) { tanggal ->
                jatuhTempo = tanggal
                tampilkanTanggal()
            }
        }
        binding.btnSimpan.setOnClickListener { simpan() }
    }

    private fun tampilkanTanggal() {
        binding.etTanggalPinjam.setText(if (tanggalPinjam == 0L) "" else Format.tanggal(tanggalPinjam))
        binding.etJatuhTempo.setText(if (jatuhTempo == 0L) "" else Format.tanggal(jatuhTempo))
    }

    private fun pilihTanggal(awal: Long, onPilih: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(if (awal == 0L) MaterialDatePicker.todayInUtcMilliseconds() else awal)
            .build()
        picker.addOnPositiveButtonClickListener { hasil -> onPilih(hasil) }
        picker.show(supportFragmentManager, "tanggal")
    }

    private fun simpan() {
        binding.tilNama.error = null
        binding.tilNominal.error = null
        binding.tilTanggalPinjam.error = null
        binding.tilJatuhTempo.error = null

        val nama = binding.etNama.text.toString().trim()
        val nominal = binding.etNominal.text.toString().trim().toLongOrNull()

        if (nama.isEmpty()) {
            binding.tilNama.error = getString(R.string.error_nama_kosong)
            return
        }
        if (nominal == null || nominal <= 0L) {
            binding.tilNominal.error = getString(R.string.error_nominal)
            return
        }
        if (tanggalPinjam == 0L) {
            binding.tilTanggalPinjam.error = getString(R.string.error_tanggal_kosong)
            return
        }
        if (jatuhTempo == 0L) {
            binding.tilJatuhTempo.error = getString(R.string.error_tanggal_kosong)
            return
        }
        if (jatuhTempo < tanggalPinjam) {
            binding.tilJatuhTempo.error = getString(R.string.error_tanggal_urutan)
            return
        }
        if (!Jaringan.isOnline(this)) {
            Toast.makeText(this, R.string.error_offline, Toast.LENGTH_LONG).show()
            return
        }

        val jenis = if (binding.toggleJenis.checkedButtonId == R.id.btnPiutang) {
            Catatan.JENIS_PIUTANG
        } else {
            Catatan.JENIS_HUTANG
        }
        val data = Catatan(
            id = editId,
            nama = nama,
            jenis = jenis,
            nominal = nominal,
            tanggalPinjam = tanggalPinjam,
            jatuhTempo = jatuhTempo,
            lunas = binding.switchLunas.isChecked,
            catatan = binding.etCatatan.text.toString().trim(),
            dibuatPada = if (dibuatPada == 0L) System.currentTimeMillis() else dibuatPada
        )

        setLoading(true)
        CatatanRepository.simpan(
            data,
            onBerhasil = {
                Toast.makeText(this, R.string.toast_tersimpan, Toast.LENGTH_SHORT).show()
                finish()
            },
            onGagal = {
                setLoading(false)
                Toast.makeText(this, R.string.error_gagal_simpan, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !loading
    }

    companion object {
        private const val EXTRA_ID = "id"
        private const val EXTRA_NAMA = "nama"
        private const val EXTRA_JENIS = "jenis"
        private const val EXTRA_NOMINAL = "nominal"
        private const val EXTRA_PINJAM = "pinjam"
        private const val EXTRA_TEMPO = "tempo"
        private const val EXTRA_LUNAS = "lunas"
        private const val EXTRA_CATATAN = "catatan"
        private const val EXTRA_DIBUAT = "dibuat"

        // catatan = null berarti mode tambah, selain itu mode ubah
        fun buatIntent(context: Context, catatan: Catatan?): Intent {
            val intent = Intent(context, FormCatatanActivity::class.java)
            if (catatan != null) {
                intent.putExtra(EXTRA_ID, catatan.id)
                intent.putExtra(EXTRA_NAMA, catatan.nama)
                intent.putExtra(EXTRA_JENIS, catatan.jenis)
                intent.putExtra(EXTRA_NOMINAL, catatan.nominal)
                intent.putExtra(EXTRA_PINJAM, catatan.tanggalPinjam)
                intent.putExtra(EXTRA_TEMPO, catatan.jatuhTempo)
                intent.putExtra(EXTRA_LUNAS, catatan.lunas)
                intent.putExtra(EXTRA_CATATAN, catatan.catatan)
                intent.putExtra(EXTRA_DIBUAT, catatan.dibuatPada)
            }
            return intent
        }
    }
}
