const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendChatNotification = functions.firestore
    .document("conversations/{conversationId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        const conversationId = context.params.conversationId;

        // 1. Don't notify the sender
        const senderId = message.senderId;

        // 2. Get the conversation to find the other participant
        const conversationRef = admin.firestore()
            .collection("conversations").doc(conversationId);
        const conversation = await conversationRef.get();

        if (!conversation.exists) return null;

        const participants = conversation.data().participants;
        const recipientId = participants.find(id => id !== senderId);

        if (!recipientId) return null;

        // 3. Get the recipient's FCM token
        // (Assuming you store tokens in a 'users' collection)
        const userRef = admin.firestore().collection("users").doc(recipientId);
        const userDoc = await userRef.get();
        const fcmToken = userDoc.data()?.fcmToken;

        if (!fcmToken) {
            console.log("No FCM token for user:", recipientId);
            return null;
        }

        // 4. Build and send the notification
        const payload = {
            token: fcmToken,
            data: {
                title: "New Message",
                body: message.content || "📷 Photo",
                conversationId: conversationId
            }
        };

        try {
            await admin.messaging().send(payload);
            console.log("Notification sent to:", recipientId);
        } catch (error) {
            console.error("Error sending notification:", error);
        }
    });