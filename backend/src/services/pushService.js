let admin;
try {
  admin = require('firebase-admin');
  const serviceAccount = require(process.env.FIREBASE_SERVICE_ACCOUNT_PATH || './firebase-service-account.json');
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
} catch {
  console.warn('Firebase Admin not initialized — FCM push disabled. Add firebase-service-account.json to enable.');
}

async function sendPushToUsers(fcmTokens, data) {
  if (!admin || !fcmTokens.length) return;

  const message = {
    data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v)])),
    tokens: fcmTokens,
    android: {
      priority: 'high',
      notification: {
        channelId: `chat_${data.chatId || 'default'}`
      }
    }
  };

  try {
    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(`Push sent: ${response.successCount}/${fcmTokens.length}`);
  } catch (err) {
    console.error('Push failed:', err.message);
  }
}

module.exports = { sendPushToUsers };
