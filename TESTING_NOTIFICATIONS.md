# 🧪 Testing ConnectMate Notifications

Step-by-step guide to test the notification system.

## ✅ Current Status

**App Status:**
- ✅ App built successfully (debug APK)
- ✅ App installed on emulator (emulator-5554)
- ✅ Notification permission: GRANTED
- ✅ FCM token retrieved successfully

**Issue to Fix:**
- ⚠️ Database rules need to be deployed (permission denied for saving FCM token)

## 🔧 Step 1: Deploy Database Rules

The database rules have been updated to allow notifications. Deploy them:

```bash
# Login to Firebase
firebase login

# Deploy database rules
firebase deploy --only database
```

This will enable:
- `/userNotifications/{userId}` - Store notifications
- `/userTokens/{userId}` - Store FCM tokens
- `/pendingReviews/{userId}` - Store pending reviews
- `/friendships/{userId}` - Store friend relationships

## 📱 Step 2: Restart the App

After deploying database rules, restart the app:

```bash
# Force stop the app
adb shell am force-stop app.connectmate

# Relaunch the app
adb shell am start -n app.connectmate/com.example.connectmate.SplashActivity
```

Check logs to confirm FCM token is saved:

```bash
adb logcat -s MainActivity:D | grep "FCM"
```

You should see:
```
D MainActivity: FCM Token: cNIexK...
D MainActivity: FCM token saved successfully
```

## 🧪 Step 3: Test Notifications

### Option A: Test with Real Activity (Recommended)

This tests the complete flow:

1. **Create an activity:**
   - Open the app on emulator
   - Tap the "+" button
   - Create a test activity (e.g., "테스트 축구")
   - Set location, time, etc.
   - Create the activity

2. **End the activity:**
   - Open the activity details
   - Tap the menu (3 dots)
   - Select "활동 종료" (End Activity)
   - Confirm ending

3. **Check for notifications:**
   - You should see 2 notifications appear:
     - "활동 종료" - Activity ended
     - "참여자 평가 요청" - Review request
   - Pull down notification shade to see them
   - Tap a notification to test deep linking

### Option B: Test with Manual Database Entry

For quick testing without creating activities:

1. **Open Firebase Console:**
   - Go to: https://console.firebase.google.com/
   - Select project: `connect-mate-25`
   - Go to: Realtime Database

2. **Add test notification:**
   - Navigate to: `/userNotifications`
   - Find your user ID (check logs: `adb logcat -s MainActivity:D | grep "user ID"`)
   - Add a new notification:

```json
/userNotifications/YOUR_USER_ID/test123: {
  "id": "test123",
  "type": "ACTIVITY",
  "title": "테스트 알림",
  "message": "이것은 테스트 알림입니다",
  "timestamp": 1702345678000,
  "isRead": false,
  "activityId": "test_activity"
}
```

3. **Check the app:**
   - Notification should appear immediately in status bar
   - Check in-app: Tap chat icon → See notification badge
   - Tap notification to see it in the notification list

### Option C: Test Friend Request Notification

1. **Need 2 devices/emulators:**
   - Device A: Your main account
   - Device B: Create a second test account

2. **Send friend request:**
   - Device A: Create/join an activity
   - Device B: Join the same activity
   - Device B: Tap on Device A's profile in participants
   - Device B: Send friend request

3. **Check Device A:**
   - Should see "친구 요청" notification
   - Notification includes sender's profile picture
   - Tap to open sender's profile

### Option D: Test Chat Join Notification

1. **Create activity with Device A**
2. **Join activity with Device B**
3. **Device A should see:**
   - "새로운 참가자" notification
   - With Device B user's name and profile picture

## 📊 Monitor Logs

### Watch all notification-related logs:

```bash
adb logcat -s MainActivity:D NotificationHelper:D FirebaseActivityManager:D ParticipantAdapter:D
```

### Watch for specific events:

**FCM Token:**
```bash
adb logcat -s MainActivity:D | grep -i "fcm"
```

**Notification Display:**
```bash
adb logcat -s NotificationHelper:D
```

**Activity Notifications:**
```bash
adb logcat -s FirebaseActivityManager:D | grep -i "notification"
```

## ✅ Verification Checklist

After testing, verify:

- [ ] **Permission granted:** App requested and received notification permission
- [ ] **FCM token saved:** Check Firebase Console → Database → `/userTokens/{userId}`
- [ ] **Notification appears:** Visible in Android status bar
- [ ] **Notification sound:** Plays default notification sound
- [ ] **Notification vibration:** Device vibrates (if enabled)
- [ ] **Profile image:** Displays sender's image (for friend/chat notifications)
- [ ] **Badge:** Shows unread count on chat icon in app
- [ ] **Deep linking:** Tapping notification opens correct screen
- [ ] **User preferences:** Disabled notification types don't show
- [ ] **Notification channels:** Long-press notification → Channel settings work

## 🎯 Expected Results

### Activity End Notification

**What you see:**
```
📱 Notification Bar:
┌─────────────────────────────┐
│ 🔔 활동 종료                  │
│ 테스트 축구 활동이 종료되었습니다│
│ Just now                     │
└─────────────────────────────┘
```

**When tapped:** Opens ActivityDetailActivity

### Review Request Notification

**What you see:**
```
📱 Notification Bar:
┌─────────────────────────────┐
│ 🔔 참여자 평가 요청            │
│ 함께한 멤버를 평가해주세요      │
│ Just now                     │
└─────────────────────────────┘
```

**When tapped:** Opens ActivityDetailActivity (review section)

### Chat Join Notification

**What you see:**
```
📱 Notification Bar:
┌─────────────────────────────┐
│ 👤 새로운 참가자               │
│ 홍길동님이 축구 채팅방에        │
│ 참가했습니다                   │
│ Just now                     │
└─────────────────────────────┘
```

**When tapped:** Opens ChatRoomActivity

### Friend Request Notification

**What you see:**
```
📱 Notification Bar:
┌─────────────────────────────┐
│ 👤 친구 요청                  │
│ 김철수님이 친구 요청을 보냈습니다│
│ Just now                     │
└─────────────────────────────┘
```

**When tapped:** Opens ProfileActivity (sender's profile)

## 🐛 Troubleshooting

### No notification appears

**Check:**
1. ✅ Database rules deployed: `firebase deploy --only database`
2. ✅ FCM token saved: Check Firebase Console
3. ✅ Permission granted: Settings → Apps → ConnectMate → Notifications
4. ✅ Channel enabled: Long-press any notification → Settings
5. ✅ User preferences: App → Settings → Notification Settings
6. ✅ Do Not Disturb: Disabled in device settings

**Debug:**
```bash
# Check if notification was created in database
adb logcat -s FirebaseActivityManager:D | grep "notification created"

# Check if NotificationHelper received the data
adb logcat -s NotificationHelper:D

# Check for errors
adb logcat -s MainActivity:E NotificationHelper:E
```

### "Permission denied" error persists

**Solution:**
```bash
# Re-deploy database rules
firebase deploy --only database

# Restart app
adb shell am force-stop app.connectmate
adb shell am start -n app.connectmate/com.example.connectmate.SplashActivity

# Check logs
adb logcat -s MainActivity:D | grep "FCM token saved"
```

### Notification appears but can't tap

**Check:**
- PendingIntent flags are correct (immutable for Android 12+)
- Activity is registered in AndroidManifest.xml
- Intent extras are properly passed

**Debug:**
```bash
adb logcat -s NotificationHelper:D | grep "PendingIntent"
```

### No profile image in notification

**Check:**
- `senderProfileUrl` is not null/empty
- Internet connection available
- Glide can load the image URL

## 📸 Screenshot Testing

1. **Capture notification:**
   ```bash
   adb shell screencap /sdcard/notification.png
   adb pull /sdcard/notification.png
   ```

2. **Record notification flow:**
   ```bash
   adb shell screenrecord /sdcard/notification_test.mp4
   # Perform actions
   # Press Ctrl+C to stop
   adb pull /sdcard/notification_test.mp4
   ```

## 🎉 Success Criteria

The notification system is working correctly when:

✅ **All 4 notification types appear correctly**
- Activity End
- Review Request
- Chat Join
- Friend Request

✅ **Notifications have all features:**
- Sound and vibration
- Profile images (where applicable)
- Correct Korean text
- Proper channel assignment

✅ **User interactions work:**
- Tapping opens correct screen
- In-app notification list shows all notifications
- Badge count is accurate
- Mark as read/delete works

✅ **User controls work:**
- Notification settings in app affect what shows
- Android channel settings work
- Permissions can be granted/revoked

## 📝 Next Steps After Testing

Once testing is complete:

1. **Build release APK** for production
2. **Test on real device** (not just emulator)
3. **Test with multiple users** (friend requests, chat joins)
4. **Test notification preferences** (disable types and verify)
5. **Test across Android versions** (8.0, 13.0, 14.0)

---

**Ready to test?** Start with deploying database rules, then try Option A (real activity flow) for the most comprehensive test!
