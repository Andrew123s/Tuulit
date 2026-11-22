package app.tuulit.notifications

import app.tuulit.TuulitApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives Connect pushes sent by the Worker. Messages are data-only, so the app decides what
 * appears on the lock screen and honours "Discreet notifications".
 */
class TuulitMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val c = (application as TuulitApp).container
        scope.launch { runCatching { c.connect.registerDevice(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val c = (application as TuulitApp).container
        val d = message.data
        val from = d["fromName"].orEmpty().ifEmpty { "Your person" }
        val (title, text) = when (d["type"]) {
            "sos" -> "$from could use some support" to d["message"].orEmpty().ifEmpty { "Open Tuulit to see how to help." }
            "reply" -> "$from sent you something" to d["message"].orEmpty()
            "accepted" -> "$from accepted your invite" to "Choose what they can see in Connect."
            "status" -> "An update from $from" to "Open Tuulit to see it."
            else -> return
        }
        scope.launch {
            val discreet = c.prefs.current().discreetNotifications
            Notifications.post(applicationContext, Notifications.ID_CONNECT + (d["type"]?.hashCode() ?: 0) % 50,
                Notifications.CH_CONNECT, title, text, discreet, route = "connect")
        }
    }
}
