package com.example.kasbon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.kasbon.databinding.FragmentRingkasanBinding
import com.google.firebase.firestore.ListenerRegistration

// Tab Ringkasan: total hutang dan piutang yang belum lunas
class RingkasanFragment : Fragment() {

    private var _binding: FragmentRingkasanBinding? = null
    private val binding get() = _binding!!
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRingkasanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        listener = CatatanRepository.dengarkan(
            onHasil = { daftar -> tampilkan(daftar) },
            onGagal = {
                _binding?.let { b -> b.tvJumlah.text = getString(R.string.error_detail_gagal) }
            }
        )
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

    private fun tampilkan(daftar: List<Catatan>) {
        val b = _binding ?: return
        val totalHutang = daftar
            .filter { it.jenis == Catatan.JENIS_HUTANG && !it.lunas }
            .sumOf { it.nominal }
        val totalPiutang = daftar
            .filter { it.jenis == Catatan.JENIS_PIUTANG && !it.lunas }
            .sumOf { it.nominal }
        val jumlahLunas = daftar.count { it.lunas }

        b.tvTotalHutang.text = Format.rupiah(totalHutang)
        b.tvTotalPiutang.text = Format.rupiah(totalPiutang)
        b.tvJumlah.text = getString(R.string.ringkasan_jumlah, daftar.size, jumlahLunas)
    }
}
