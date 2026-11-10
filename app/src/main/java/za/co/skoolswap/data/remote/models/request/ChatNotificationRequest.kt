package za.co.skoolswap.data.remote.models.request

import com.google.gson.annotations.SerializedName

data class ChatNotificationRequest(
    @SerializedName("recipient_id") val recipientId: String,
    @SerializedName("message_body") val messageBody: String,
    @SerializedName("conversation_id") val conversationId: String
)