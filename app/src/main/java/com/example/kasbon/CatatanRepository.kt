package com.example.kasbon

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

// Semua urusan baca/tulis Firestore ada di sini.
// Data disimpan di: users/{uid}/catatan/{id}  (tiap akun hanya melihat datanya sendiri)
object CatatanRepository {

    private fun koleksi(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("catatan")
    }

    // Mendengarkan perubahan data secara langsung (realtime)
    fun dengarkan(
        onHasil: (List<Catatan>) -> Unit,
        onGagal: (Exception) -> Unit
    ): ListenerRegistration? {
        val col = koleksi()
        if (col == null) {
            onGagal(IllegalStateException("Belum login"))
            return null
        }
        return col.orderBy("jatuhTempo", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onGagal(error)
                    return@addSnapshotListener
                }
                val daftar = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Catatan::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                onHasil(daftar)
            }
    }

    // Tambah baru (id kosong) atau ubah (id terisi)
    fun simpan(catatan: Catatan, onBerhasil: () -> Unit, onGagal: (Exception) -> Unit) {
        val col = koleksi()
        if (col == null) {
            onGagal(IllegalStateException("Belum login"))
            return
        }
        val ref = if (catatan.id.isEmpty()) col.document() else col.document(catatan.id)
        ref.set(catatan.copy(id = ref.id))
            .addOnSuccessListener { onBerhasil() }
            .addOnFailureListener { e -> onGagal(e) }
    }

    fun hapus(id: String, onBerhasil: () -> Unit, onGagal: (Exception) -> Unit) {
        val col = koleksi()
        if (col == null) {
            onGagal(IllegalStateException("Belum login"))
            return
        }
        col.document(id).delete()
            .addOnSuccessListener { onBerhasil() }
            .addOnFailureListener { e -> onGagal(e) }
    }
}
