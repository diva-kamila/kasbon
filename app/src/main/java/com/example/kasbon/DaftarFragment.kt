package com.example.kasbon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kasbon.databinding.FragmentDaftarBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration

// Tab Beranda: daftar catatan + pencarian + filter jenis dan status
class DaftarFragment : Fragment() {

    private var _binding: FragmentDaftarBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CatatanAdapter
    private var listener: ListenerRegistration? = null
    private var semuaData: List<Catatan> = emptyList()
    private var sudahMuat = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDaftarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CatatanAdapter(
            onKlik = { catatan ->
                startActivity(DetailCatatanActivity.buatIntent(requireContext(), catatan.id))
            },
            onHapus = { catatan -> konfirmasiHapus(catatan) }
        )
        binding.rvCatatan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCatatan.adapter = adapter

        binding.fabTambah.setOnClickListener {
            startActivity(FormCatatanActivity.buatIntent(requireContext(), null))
        }
        binding.btnCobaLagi.setOnClickListener { muatData() }

        // Setiap kali kata kunci atau filter berubah, daftar disaring ulang
        binding.etCari.doAfterTextChanged { if (sudahMuat) terapkanFilter() }
        binding.chipGroupJenis.setOnCheckedStateChangeListener { _, _ -> if (sudahMuat) terapkanFilter() }
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, _ -> if (sudahMuat) terapkanFilter() }
    }

    override fun onStart() {
        super.onStart()
        muatData()
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun muatData() {
        val b = _binding ?: return
        b.tvOffline.visibility = if (Jaringan.isOnline(requireContext())) View.GONE else View.VISIBLE
        if (!sudahMuat) tampilkanLoading()
        listener?.remove()
        listener = CatatanRepository.dengarkan(
            onHasil = { daftar ->
                semuaData = daftar
                sudahMuat = true
                terapkanFilter()
            },
            onGagal = { tampilkanError() }
        )
    }

    private fun tampilkanLoading() {
        val b = _binding ?: return
        b.progressBar.visibility = View.VISIBLE
        b.rvCatatan.visibility = View.GONE
        b.tvKosong.visibility = View.GONE
        b.tvTidakAda.visibility = View.GONE
        b.layoutError.visibility = View.GONE
    }

    private fun tampilkanError() {
        val b = _binding ?: return
        sudahMuat = false
        b.progressBar.visibility = View.GONE
        b.rvCatatan.visibility = View.GONE
        b.tvKosong.visibility = View.GONE
        b.tvTidakAda.visibility = View.GONE
        b.layoutError.visibility = View.VISIBLE
    }

    // Menyaring daftar berdasarkan kata kunci, jenis, dan status
    private fun terapkanFilter() {
        val b = _binding ?: return

        val kata = b.etCari.text.toString().trim().lowercase()
        val jenis: String? = when (b.chipGroupJenis.checkedChipId) {
            R.id.chipHutang -> Catatan.JENIS_HUTANG
            R.id.chipPiutang -> Catatan.JENIS_PIUTANG
            else -> null
        }
        val status: Boolean? = when (b.chipGroupStatus.checkedChipId) {
            R.id.chipBelumLunas -> false
            R.id.chipLunas -> true
            else -> null
        }

        val hasil = semuaData.filter { c ->
            val cocokKata = kata.isEmpty() ||
                c.nama.lowercase().contains(kata) ||
                c.catatan.lowercase().contains(kata)
            val cocokJenis = jenis == null || c.jenis == jenis
            val cocokStatus = status == null || c.lunas == status
            cocokKata && cocokJenis && cocokStatus
        }

        b.tvOffline.visibility = if (Jaringan.isOnline(requireContext())) View.GONE else View.VISIBLE
        b.progressBar.visibility = View.GONE
        b.layoutError.visibility = View.GONE
        when {
            semuaData.isEmpty() -> {
                b.tvKosong.visibility = View.VISIBLE
                b.tvTidakAda.visibility = View.GONE
                b.rvCatatan.visibility = View.GONE
            }
            hasil.isEmpty() -> {
                b.tvKosong.visibility = View.GONE
                b.tvTidakAda.visibility = View.VISIBLE
                b.rvCatatan.visibility = View.GONE
            }
            else -> {
                b.tvKosong.visibility = View.GONE
                b.tvTidakAda.visibility = View.GONE
                b.rvCatatan.visibility = View.VISIBLE
            }
        }
        adapter.submitList(hasil)
    }

    private fun konfirmasiHapus(catatan: Catatan) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_hapus_judul)
            .setMessage(getString(R.string.dialog_hapus_pesan, catatan.nama))
            .setNegativeButton(R.string.btn_batal, null)
            .setPositiveButton(R.string.btn_hapus) { _, _ -> hapus(catatan) }
            .show()
    }

    private fun hapus(catatan: Catatan) {
        val ctx = requireContext()
        if (!Jaringan.isOnline(ctx)) {
            Toast.makeText(ctx, R.string.error_offline, Toast.LENGTH_LONG).show()
            return
        }
        CatatanRepository.hapus(
            catatan.id,
            onBerhasil = { Toast.makeText(ctx, R.string.toast_dihapus, Toast.LENGTH_SHORT).show() },
            onGagal = { Toast.makeText(ctx, R.string.error_gagal_hapus, Toast.LENGTH_LONG).show() }
        )
    }
}
