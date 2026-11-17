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

    // Mendengarkan seluruh daftar catatan secara langsung (realtime)
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

    // Mendengarkan satu catatan (untuk halaman detail). Hasil null berarti catatan sudah dihapus.
    fun dengarkanSatu(
        id: String,
        onHasil: (Catatan?) -> Unit,
        onGagal: (Exception) -> Unit
    ): ListenerRegistration? {
        val col = koleksi()
        if (col == null) {
            onGagal(IllegalStateException("Belum login"))
            return null
        }
        return col.document(id).addSnapshotListener { doc, error ->
            if (error != null) {
                onGagal(error)
                return@addSnapshotListener
            }
            if (doc == null || !doc.exists()) {
                onHasil(null)
                return@addSnapshotListener
            }
            onHasil(doc.toObject(Catatan::class.java)?.copy(id = doc.id))
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

    // Mengubah status lunas saja
    fun ubahLunas(id: String, lunas: Boolean, onBerhasil: () -> Unit, onGagal: (Exception) -> Unit) {
        val col = koleksi()
        if (col == null) {
            onGagal(IllegalStateException("Belum login"))
            return
        }
        col.document(id).update("lunas", lunas)
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
