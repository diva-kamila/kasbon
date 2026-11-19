package com.example.kasbon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kasbon.databinding.FragmentProfilBinding
import com.google.firebase.auth.FirebaseAuth

// Tab Profil: info akun, tes pengingat, dan tombol Keluar
class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        binding.tvNama.text = user?.displayName?.takeIf { it.isNotBlank() } ?: "-"
        binding.tvEmail.text = user?.email ?: "-"

        binding.btnTesNotif.setOnClickListener {
            val ctx = requireContext()
            if (!Pengingat.izinDiberikan(ctx)) {
                Toast.makeText(ctx, R.string.toast_izin_notif, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!Jaringan.isOnline(ctx)) {
                Toast.makeText(ctx, R.string.error_offline, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Pengingat.kirimSekarang(ctx)
            Toast.makeText(ctx, R.string.toast_tes_notif, Toast.LENGTH_SHORT).show()
        }

        binding.btnKeluar.setOnClickListener {
            Pengingat.batalkan(requireContext())
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
