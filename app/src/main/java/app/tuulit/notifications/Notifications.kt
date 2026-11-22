package app.tuulit.notifications

import android.Manifest
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
import app.tuulit.MainActivity
import app.tuulit.R

/** Notification channels and posting. All copy is discreet by default (no cycle words on the lock screen). */
object Notifications {
    const val CH_REMINDERS = "reminders"
    const val CH_CARE = "care"
    const val CH_CONNECT = "connect"

    const val ID_PERIOD = 1001
    const val ID_CHECKIN = 1002
    const val ID_CARE = 1003
    const val ID_CONNECT = 1004

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(listOf(
            NotificationChannel(CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Period heads-up and daily check-in"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
            NotificationChannel(CH_CARE, "Care suggestions", NotificationManager.IMPORTANCE_LOW).apply {
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
            NotificationChannel(CH_CONNECT, "Trusted people", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Messages from people you're connected with"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        ))
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Posts a notification. When [discreet] is true the public (lock-screen) version shows only
     * "Tuulit · Take a quick moment for yourself", whatever the private text says.
     */
    fun post(context: Context, id: Int, channel: String, title: String, text: String, discreet: Boolean, route: String? = null) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            route?.let { putExtra(MainActivity.EXTRA_ROUTE, it) }
        }
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val public = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_tuulit)
            .setContentTitle("Tuulit")
            .setContentText("Take a quick moment for yourself.")
            .build()
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_tuulit)
            .setContentTitle(if (discreet) "Tuulit" else title)
            .setContentText(if (discreet) "Take a quick moment for yourself." else text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (discreet) "Take a quick moment for yourself." else text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
    }
}
