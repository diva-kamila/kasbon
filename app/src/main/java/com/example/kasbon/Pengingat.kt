package com.example.kasbon

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Semua urusan pengingat jatuh tempo: jadwal, kirim manual, dan tampilan notifikasi
object Pengingat {

    const val CHANNEL_ID = "jatuh_tempo"
    const val KEY_MANUAL = "manual"
    private const val NAMA_KERJA = "pengingat_jatuh_tempo"
    private const val ID_NOTIFIKASI = 1001

    private fun syarat(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    // Pengecekan otomatis sekali sehari (dijalankan Android di latar belakang)
    fun jadwalkan(context: Context) {
        val request = PeriodicWorkRequest.Builder(PengingatWorker::class.java, 1, TimeUnit.DAYS)
            .setConstraints(syarat())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NAMA_KERJA,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun batalkan(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NAMA_KERJA)
    }

    // Untuk uji coba dan demo: cek sekarang juga
    fun kirimSekarang(context: Context) {
        val request = OneTimeWorkRequest.Builder(PengingatWorker::class.java)
            .setConstraints(syarat())
            .setInputData(Data.Builder().putBoolean(KEY_MANUAL, true).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    // Android 13 ke atas butuh izin notifikasi dari pengguna
    fun izinDiberikan(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun tampilkan(context: Context, judul: String, isi: String) {
        if (!izinDiberikan(context)) return // izin ditolak: lewati tanpa error

        buatChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notifikasi = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(judul)
            .setContentText(isi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(isi))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(ID_NOTIFIKASI, notifikasi)
    }

    private fun buatChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_nama),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = context.getString(R.string.notif_channel_deskripsi)
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
