# 🔔 ConnectMate Push Notifications Setup Guide

Complete guide for implementing and deploying push notifications in ConnectMate.

## 📱 What Was Implemented

### 1. Android App Components (✅ Already Configured)

**OS-Level Notifications:**
- ✅ `NotificationHelper.java` - Manages notification channels and display
- ✅ `MyFirebaseMessagingService.java` - Receives FCM messages
- ✅ Notification permission handling for Android 13+
- ✅ FCM token generation and storage
- ✅ Notification channels: Chat, Activity, Social
- ✅ Deep linking to Activity Detail, Chat Room, Profile screens

**Integration Points:**
- ✅ `FirebaseActivityManager.java` - Activity end, review request, chat join notifications
- ✅ `ParticipantAdapter.java` - Friend request notifications
- ✅ `MainActivity.java` - Permission request and FCM token retrieval

### 2. Firebase Cloud Functions (✅ Just Created)

**Server-Side Push Notification Sending:**
- ✅ `functions/index.js` - Cloud Functions for FCM push notifications
- ✅ `sendNotificationToUser` - Sends push notifications when new notifications created
- ✅ `cleanupOldNotifications` - Optional maintenance function
- ✅ `onTokenUpdate` - Token management function

## 🚀 Quick Deployment Steps

### Prerequisites

1. **Install Node.js 18+**
   ```bash
   # Check version
   node --version

   # If not installed, download from https://nodejs.org/
   ```

2. **Install Firebase CLI**
   ```bash
   npm install -g firebase-tools
   ```

3. **Upgrade Firebase Project to Blaze Plan**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your ConnectMate project
   - Go to: Project Settings → Usage and billing
   - Click "Upgrade" to Blaze (pay-as-you-go) plan
   - ⚠️ Don't worry: Free tier includes 2M function invocations/month

### Deploy in 3 Commands

```bash
# 1. Install dependencies
cd functions
npm install

# 2. Login to Firebase (if not already logged in)
firebase login

# 3. Deploy functions
firebase deploy --only functions
```

That's it! 🎉

## 📊 How It Works

### Flow Diagram

```
User Action (e.g., activity ends)
         ↓
Android App creates notification in Firebase
         ↓
Firebase Realtime Database: /userNotifications/{userId}/{notificationId}
         ↓
🔥 Cloud Function Triggered (sendNotificationToUser)
         ↓
Cloud Function reads FCM token from /userTokens/{userId}
         ↓
Cloud Function sends FCM message to user's device
         ↓
MyFirebaseMessagingService receives message
         ↓
NotificationHelper displays OS notification
         ↓
User taps notification → Opens relevant screen
```

### Database Structure

```
Firebase Realtime Database
├── userNotifications/
│   └── {userId}/
│       └── {notificationId}/
│           ├── id: "notif123"
│           ├── type: "ACTIVITY" | "CHAT_JOIN" | "FRIEND_REQUEST"
│           ├── title: "활동 종료"
│           ├── message: "축구 활동이 종료되었습니다"
│           ├── timestamp: 1702345678000
│           ├── activityId: "activity123"
│           ├── senderId: "user456"
│           ├── senderName: "홍길동"
│           └── senderProfileUrl: "https://..."
│
└── userTokens/
    └── {userId}: "FCM_TOKEN_STRING"
```

## 🧪 Testing

### Test on Emulator (Recommended First)

```bash
cd functions
npm run serve
```

Then:
1. Open emulator UI at http://localhost:4000
2. Create a test notification in your local database
3. Watch function execute in real-time

### Test in Production

**Option 1: Use the App**
1. Install app on 2 devices with different users
2. Perform an action that triggers notification (e.g., end activity)
3. Check if notification appears on other user's device

**Option 2: Manual Database Entry**
1. Go to Firebase Console → Realtime Database
2. Add test notification:
   ```json
   /userNotifications/{your-user-id}/test123: {
     "id": "test123",
     "type": "ACTIVITY",
     "title": "테스트 알림",
     "message": "테스트 메시지입니다",
     "timestamp": 1702345678000,
     "isRead": false
   }
   ```
3. Check device for notification

**Option 3: Use Firebase Console**
1. Firebase Console → Cloud Messaging
2. Click "Send your first message"
3. Enter notification text
4. Select your app
5. Send test message

## 📝 View Logs

```bash
# View all function logs
firebase functions:log

# View logs for specific function
firebase functions:log --only sendNotificationToUser

# Stream logs in real-time
firebase functions:log --follow
```

Or in Firebase Console:
- Functions → Logs
- See execution count, errors, duration

## 🎯 Supported Notification Types

| Type | Korean Title | When Triggered | Deep Link |
|------|--------------|----------------|-----------|
| **ACTIVITY** | 활동 종료 / 참여자 평가 요청 | Activity ends | ActivityDetailActivity |
| **CHAT_JOIN** | 새로운 참가자 | User joins chat | ChatRoomActivity |
| **FRIEND_REQUEST** | 친구 요청 | Friend request sent | ProfileActivity |

## 🔧 Configuration Files

### Project Structure
```
ConnectMate/
├── app/                                    # Android app
│   └── src/main/java/com/example/connectmate/
│       ├── MyFirebaseMessagingService.java  # FCM receiver
│       └── utils/
│           └── NotificationHelper.java      # Notification manager
├── functions/                              # Cloud Functions
│   ├── index.js                            # Main functions file
│   ├── package.json                        # Dependencies
│   ├── .gitignore                          # Git ignore
│   └── README.md                           # Detailed docs
└── firebase.json                           # Firebase config
```

## 💰 Cost Estimate

**Free Tier (per month):**
- 2,000,000 function invocations
- 400,000 GB-seconds compute time
- 5 GB outbound networking

**Typical ConnectMate Usage:**
- ~50-200 notifications/day per user
- ~10-100 active users
- **Estimated: 1,000-20,000 invocations/month**
- **Cost: FREE (well within free tier)**

Even with 1000 users and 100 notifications/day:
- ~3,000,000 invocations/month
- Exceeds free tier by 1M
- **Extra cost: ~$0.40/month**

## ⚠️ Important Notes

1. **Billing Required**: Cloud Functions require Blaze plan (but free tier is generous)
2. **Token Management**: FCM tokens automatically refresh and are saved by the app
3. **User Preferences**: Notifications respect user's NotificationSettings preferences
4. **Offline Support**: Notifications work even when app is closed
5. **Korean Language**: All notification text is in Korean
6. **Security**: Cloud Functions run server-side, can't be tampered with

## 🐛 Troubleshooting

### No Notifications Appearing

**Check:**
1. ✅ Functions deployed: `firebase deploy --only functions`
2. ✅ Blaze plan enabled: Firebase Console → Settings → Usage and billing
3. ✅ FCM token exists: Firebase Console → Database → `/userTokens/{userId}`
4. ✅ Permission granted: Device Settings → Apps → ConnectMate → Notifications
5. ✅ Function logs: `firebase functions:log`

### Deployment Errors

**"Billing account not configured"**
→ Upgrade to Blaze plan in Firebase Console

**"Node version mismatch"**
→ Install Node.js 18: https://nodejs.org/

**"EACCES permission denied"**
→ Run: `sudo npm install -g firebase-tools`

## 📞 Next Steps

### Optional Enhancements

1. **Chat Message Notifications**
   - Uncomment `sendChatNotification` function in `functions/index.js`
   - Modify to match your chat structure

2. **Friend Accepted Notifications**
   - Add Cloud Function listening to `/friendships` path
   - Send notification when friend request accepted

3. **Scheduled Notifications**
   - Activity reminders 1 hour before
   - Daily/weekly activity recommendations

4. **Notification Actions**
   - "Accept" / "Decline" buttons for friend requests
   - "Reply" action for chat messages

5. **Notification Grouping**
   - Group multiple notifications of same type
   - Show notification count

## 📚 Resources

- **Detailed Documentation**: `functions/README.md`
- **Firebase Console**: https://console.firebase.google.com/
- **Cloud Functions Docs**: https://firebase.google.com/docs/functions
- **FCM Docs**: https://firebase.google.com/docs/cloud-messaging

---

## ✅ Deployment Checklist

- [ ] Node.js 18+ installed
- [ ] Firebase CLI installed (`npm install -g firebase-tools`)
- [ ] Logged into Firebase (`firebase login`)
- [ ] Firebase project on Blaze plan
- [ ] Dependencies installed (`cd functions && npm install`)
- [ ] Functions deployed (`firebase deploy --only functions`)
- [ ] Tested with real notification
- [ ] Checked Firebase Console logs
- [ ] Verified notification appears on device

---

**Need help?** Check the troubleshooting section or review `functions/README.md` for detailed documentation.
