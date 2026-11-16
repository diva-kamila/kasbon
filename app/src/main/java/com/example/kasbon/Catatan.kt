package com.example.kasbon

// Model data satu catatan hutang/piutang.
// Semua parameter punya nilai awal supaya Firestore bisa membuat objek dari dokumen.
data class Catatan(
    val id: String = "",
    val nama: String = "",
    val jenis: String = JENIS_HUTANG,
    val nominal: Long = 0L,
    val tanggalPinjam: Long = 0L, // milidetik UTC
    val jatuhTempo: Long = 0L,    // milidetik UTC
    val lunas: Boolean = false,
    val catatan: String = "",
    val dibuatPada: Long = 0L
) {
    companion object {
        const val JENIS_HUTANG = "HUTANG"
        const val JENIS_PIUTANG = "PIUTANG"
    }
}
