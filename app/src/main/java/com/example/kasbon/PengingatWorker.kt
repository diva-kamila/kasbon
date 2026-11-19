package com.example.kasbon

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Dijalankan Android di latar belakang: cari catatan belum lunas yang jatuh tempo
// hari ini, besok, atau sudah terlambat, lalu tampilkan satu notifikasi.
class PengingatWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        // Belum login (atau sudah keluar): tidak ada yang diingatkan
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val manual = inputData.getBoolean(Pengingat.KEY_MANUAL, false)

        return try {
            val hasil = Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid).collection("catatan")
                    .whereEqualTo("lunas", false)
                    .get()
            )
            val daftar = hasil.documents.mapNotNull { doc ->
                doc.toObject(Catatan::class.java)?.copy(id = doc.id)
            }

            val sehari = 24L * 60L * 60L * 1000L
            val batas = Format.hariIniUtc() + sehari // sampai akhir hari besok
            val mendesak = daftar.filter { it.jatuhTempo <= batas }.sortedBy { it.jatuhTempo }

            val ctx = applicationContext
            if (mendesak.isNotEmpty()) {
                val nama = mendesak.take(3).joinToString(", ") { it.nama }
                val tambahan = if (mendesak.size > 3) " dll" else ""
                Pengingat.tampilkan(
                    ctx,
                    ctx.getString(R.string.notif_judul),
                    ctx.getString(R.string.notif_isi, mendesak.size, nama + tambahan)
                )
            } else if (manual) {
                // Hanya saat uji coba manual, supaya jelas bahwa pengecekan berjalan
                Pengingat.tampilkan(
                    ctx,
                    ctx.getString(R.string.notif_judul),
                    ctx.getString(R.string.notif_kosong)
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
