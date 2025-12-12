# ConnectMate Firebase Cloud Functions

This directory contains Firebase Cloud Functions for sending push notifications to ConnectMate app users.

## 📋 Overview

The Cloud Functions automatically send FCM (Firebase Cloud Messaging) push notifications to users when certain events occur, even when they're offline or not actively using the app.

### Implemented Functions

1. **sendNotificationToUser** - Main notification function
   - Triggers when: New notification is added to `/userNotifications/{userId}/{notificationId}`
   - What it does: Sends FCM push notification to the user's device
   - Supports: Activity notifications, chat join notifications, friend requests

2. **cleanupOldNotifications** - Maintenance function (optional)
   - Triggers: Daily at midnight KST
   - What it does: Removes notifications older than 30 days
   - Purpose: Keeps database clean and reduces storage costs

3. **onTokenUpdate** - Token management function
   - Triggers when: FCM token is updated at `/userTokens/{userId}`
   - What it does: Logs token changes for debugging
   - Purpose: Monitor token updates and manage subscriptions

## 🚀 Deployment Instructions

### Prerequisites

1. **Node.js 18 or later** installed on your system
   ```bash
   node --version  # Should show v18.x.x or higher
   ```

2. **Firebase CLI** installed globally
   ```bash
   npm install -g firebase-tools
   ```

3. **Firebase project** with Blaze (pay-as-you-go) plan
   - Cloud Functions require the Blaze plan
   - Note: Firebase provides generous free tier limits

### Step 1: Install Dependencies

Navigate to the functions directory and install dependencies:

```bash
cd functions
npm install
```

This will install:
- `firebase-admin` - Firebase Admin SDK for server-side operations
- `firebase-functions` - Cloud Functions runtime

### Step 2: Authenticate with Firebase

If you haven't already, log in to Firebase:

```bash
firebase login
```

This will open a browser window for authentication.

### Step 3: Select Your Firebase Project

Make sure you're using the correct Firebase project:

```bash
firebase use --add
```

Select your ConnectMate project from the list.

Or if already configured:

```bash
firebase use default
```

### Step 4: Deploy Functions

Deploy all functions to Firebase:

```bash
# From the project root directory
firebase deploy --only functions
```

Or deploy a specific function:

```bash
firebase deploy --only functions:sendNotificationToUser
```

### Step 5: Verify Deployment

After deployment, you should see output similar to:

```
✔  functions: Finished running predeploy script.
i  functions: preparing functions directory for uploading...
i  functions: packaged functions (xx.xx KB) for uploading
✔  functions: functions folder uploaded successfully
i  functions: creating Node.js 18 function sendNotificationToUser...
✔  functions[sendNotificationToUser]: Successful create operation.
Function URL (sendNotificationToUser): https://us-central1-your-project.cloudfunctions.net/sendNotificationToUser

✔  Deploy complete!
```

## 🧪 Testing

### Test with Firebase Emulator (Local Testing)

Before deploying to production, test functions locally:

```bash
cd functions
npm run serve
```

This starts the Firebase emulator suite. You can then:
1. Create test notifications in your local database
2. Monitor function execution in the emulator UI at http://localhost:4000

### Test in Production

1. **Create a test notification** in your Firebase Realtime Database:
   - Go to Firebase Console → Realtime Database
   - Navigate to `/userNotifications/{your-user-id}/`
   - Add a new notification with this structure:
     ```json
     {
       "id": "test123",
       "type": "ACTIVITY",
       "title": "테스트 알림",
       "message": "이것은 테스트 메시지입니다",
       "timestamp": 1702345678000,
       "isRead": false,
       "activityId": "activity123"
     }
     ```

2. **Check Firebase Console Logs**:
   - Go to Firebase Console → Functions → Logs
   - You should see log entries for the function execution

3. **Verify on Device**:
   - Make sure your test device has the app installed
   - The notification should appear in the status bar

## 📊 Monitoring & Logs

### View Function Logs

In Firebase Console:
```
Firebase Console → Functions → Logs
```

Or via CLI:
```bash
firebase functions:log
```

To see logs for a specific function:
```bash
firebase functions:log --only sendNotificationToUser
```

### Monitor Function Performance

Firebase Console → Functions → Dashboard shows:
- Invocation count
- Execution time
- Error rate
- Memory usage

## 💰 Cost Considerations

Firebase Cloud Functions are billed based on:
1. **Invocations** - Number of times functions are called
2. **Compute time** - How long functions run
3. **Outbound networking** - Data sent from functions

**Free tier includes:**
- 2 million invocations per month
- 400,000 GB-seconds of compute time
- 5GB outbound networking

For ConnectMate's typical usage, you'll likely stay within the free tier.

## 🔧 Configuration

### Environment Variables (Optional)

If you need to add environment variables:

```bash
firebase functions:config:set someservice.key="THE API KEY"
```

Then access in code:
```javascript
const apiKey = functions.config().someservice.key;
```

### Function Timeouts

By default, functions timeout after 60 seconds. To change:

```javascript
exports.myFunction = functions
  .runWith({ timeoutSeconds: 300 }) // 5 minutes
  .database.ref('/path')
  .onCreate(async (snapshot, context) => {
    // ...
  });
```

## 🐛 Troubleshooting

### "Billing account not configured"
- Cloud Functions require Blaze (pay-as-you-go) plan
- Upgrade in Firebase Console → Project Settings → Usage and billing

### "EACCES: permission denied"
- Run with sudo: `sudo npm install -g firebase-tools`
- Or fix npm permissions: https://docs.npmjs.com/resolving-eacces-permissions-errors

### "Function deployment failed"
- Check Node.js version: `node --version` (must be 18+)
- Clear npm cache: `npm cache clean --force`
- Delete node_modules and reinstall: `rm -rf node_modules && npm install`

### Notifications not being sent
1. **Check FCM token exists** in `/userTokens/{userId}`
2. **Check function logs** for errors
3. **Verify notification data** has all required fields
4. **Check device permission** - User must grant notification permission on Android 13+

### Function runs but no notification appears
1. **Check NotificationHelper** in Android app - may be blocking based on user preferences
2. **Verify FCM token** is up to date
3. **Check Android notification channels** are created properly
4. **Test with a simple FCM message** using Firebase Console → Cloud Messaging

## 📱 Integration with Android App

The Android app (ConnectMate) is already configured to:
1. ✅ Request notification permission (Android 13+)
2. ✅ Generate and save FCM tokens to `/userTokens/{userId}`
3. ✅ Receive and display FCM notifications via `MyFirebaseMessagingService`
4. ✅ Create notification channels for different types
5. ✅ Handle notification taps with deep linking

## 📚 Additional Resources

- [Firebase Cloud Functions Documentation](https://firebase.google.com/docs/functions)
- [FCM Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Admin SDK for Node.js](https://firebase.google.com/docs/admin/setup)
- [Cloud Functions Pricing](https://firebase.google.com/pricing)

## 🔄 Updates & Maintenance

To update the functions after making changes:

```bash
cd functions
firebase deploy --only functions
```

To delete a function:
```bash
firebase functions:delete functionName
```

## 📞 Support

For issues or questions:
1. Check Firebase Console logs
2. Review function code in `index.js`
3. Test with Firebase emulator
4. Check FCM token validity
