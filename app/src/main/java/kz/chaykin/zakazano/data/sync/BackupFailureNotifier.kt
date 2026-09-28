package kz.chaykin.zakazano.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kz.chaykin.zakazano.MainActivity
import kz.chaykin.zakazano.R

/**
 * Локальное уведомление о том, что копия не уехала на Диск. Никакого сервера: его
 * показывает сама фоновая задача. Уведомление одно — каждое новое заменяет старое,
 * а удачная выгрузка его убирает.
 */
class BackupFailureNotifier(context: Context) {

    private val appContext = context.applicationContext

    /** Канал заводится при старте: без него на Android 8+ уведомление просто не покажется. */
    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notification_channel_backup),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.notification_channel_backup_description)
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Разрешены ли уведомления вообще — и системным разрешением, и переключателем в настройках. */
    fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    fun show(reason: String) {
        // Нет разрешения — молча обходимся без уведомления: выгрузка от этого не зависит.
        if (!canNotify()) return

        val openSettings = PendingIntent.getActivity(
            appContext,
            0,
            MainActivity.openSettingsIntent(appContext),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_mono)
            .setContentTitle(appContext.getString(R.string.notification_backup_failed_title))
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setContentIntent(openSettings)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Разрешение отозвали между проверкой и показом — не повод ронять фоновую задачу.
        }
    }

    fun cancel() {
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
    }

    private companion object {
        const val CHANNEL_ID = "backup"
        const val NOTIFICATION_ID = 1001
    }
}

