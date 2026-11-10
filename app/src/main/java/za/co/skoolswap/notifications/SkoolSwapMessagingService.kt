package za.co.skoolswap.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import za.co.skoolswap.R
import za.co.skoolswap.data.local.datastore.AppPreferences
import za.co.skoolswap.ui.main.MainActivity
import javax.inject.Inject
import za.co.skoolswap.data.remote.api.UserApiService
import za.co.skoolswap.data.remote.models.request.FcmTokenRequest
import za.co.skoolswap.domain.repository.AuthRepositoryInterface

@AndroidEntryPoint
class SkoolSwapMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var appPreferences: AppPreferences
    @Inject
    lateinit var authRepository: AuthRepositoryInterface

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("🔥 New FCM Token: $token")
        saveTokenToFirestoreAndRails(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("📩 FCM message received: ${remoteMessage.data}")

        val title = remoteMessage.data["title"] ?: "New Message"
        val body = remoteMessage.data["body"] ?: "You have a new message"
        val conversationId = remoteMessage.data["conversationId"]

        showNotification(title, body, conversationId)
    }

    /**
     * ✅ Save FCM token to Firestore
     *
     * Stores under: users/{userId}/fcmToken
     * Cloud Function reads this to send notifications.
     */
    private fun saveTokenToFirestoreAndRails(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = appPreferences.getUserId()?.toString() ?: return@launch

                // 1. Save to Firestore
                firestore.collection("users")
                    .document(userId)
                    .set(
                        mapOf(
                            "fcmToken" to token,
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()

                Timber.d("✅ FCM token saved to Firestore for user: $userId")

                // 2. Save to Rails via repository (clean architecture)
                authRepository.saveFcmToken(token)
                    .onSuccess {
                        Timber.d("✅ FCM token saved to Rails")
                    }
                    .onFailure { error ->
                        Timber.w("⚠️ Rails FCM save failed: ${error.message}")
                    }

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to save FCM token")
            }
        }
    }

    private fun showNotification(title: String, body: String, conversationId: String?) {
        val channelId = "chat_messages"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("conversationId", conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}