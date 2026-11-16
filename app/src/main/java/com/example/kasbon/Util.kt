package com.example.kasbon

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Pembantu untuk menampilkan angka dan tanggal
object Format {
    private val localeId: Locale = Locale.forLanguageTag("id-ID")

    fun rupiah(nominal: Long): String =
        "Rp " + NumberFormat.getInstance(localeId).format(nominal)

    // Tanggal disimpan sebagai milidetik UTC (sama dengan hasil MaterialDatePicker)
    fun tanggal(utcMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", localeId)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(utcMillis))
    }

    fun hariIniUtc(): Long = MaterialDatePicker.todayInUtcMilliseconds()
}

// Cek koneksi internet
object Jaringan {
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
