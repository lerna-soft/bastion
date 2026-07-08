package com.bastion.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bastion.app.MainActivity
import com.bastion.app.R
import com.bastion.app.logging.RemoteLogger

/**
 * Foreground service (HIM-011) que mantiene el proceso vivo mientras hay sesiones SSH activas, para
 * que Android no lo mate al pasar la app a segundo plano (y así no se caigan las sesiones).
 * No aloja las sesiones; solo eleva la prioridad del proceso mediante una notificación persistente.
 */
class SessionKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val count = intent?.getIntExtra(EXTRA_COUNT, 1) ?: 1
        try {
            startForeground(NOTIF_ID, buildNotification(count))
        } catch (e: Throwable) {
            RemoteLogger.e("KeepAlive", "startForeground falló: ${e.message}", e)
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Sesiones SSH activas",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Mantiene las sesiones del terminal abiertas en segundo plano" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(count: Int): Notification {
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = android.app.PendingIntent.getActivity(
            this, 0, open,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (count == 1) "1 sesión SSH activa" else "$count sesiones SSH activas"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bastion")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "bastion_sessions"
        private const val NOTIF_ID = 1001
        private const val EXTRA_COUNT = "count"

        /** Inicia (o actualiza) el service mientras haya sesiones activas. Idempotente. */
        fun start(context: Context, count: Int) {
            try {
                val i = Intent(context, SessionKeepAliveService::class.java).putExtra(EXTRA_COUNT, count)
                ContextCompat.startForegroundService(context, i)
            } catch (e: Throwable) {
                RemoteLogger.e("KeepAlive", "start falló: ${e.message}", e)
            }
        }

        /** Detiene el service cuando ya no hay sesiones. */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, SessionKeepAliveService::class.java))
            } catch (e: Throwable) {
                RemoteLogger.e("KeepAlive", "stop falló: ${e.message}", e)
            }
        }
    }
}
