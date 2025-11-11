# ConnectMate Multi-User Sync Testing Guide

## Overview
This guide provides comprehensive instructions for testing the multi-user sync functionality in ConnectMate. The app now uses Firebase Realtime Database for instant synchronization across all devices.

---

## Prerequisites

### 1. Firebase Setup
Before testing, ensure Firebase is properly configured:

1. **Verify Firebase Configuration**
   ```bash
   # Check that google-services.json exists
   ls app/google-services.json
   ```

2. **Deploy Security Rules**
   ```bash
   # Install Firebase CLI if not already installed
   npm install -g firebase-tools

   # Login to Firebase
   firebase login

   # Initialize Firebase in your project (if not done)
   firebase init database

   # Deploy security rules
   firebase deploy --only database
   ```

3. **Enable Firebase Realtime Database**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Select your project: `connect-mate-25`
   - Navigate to **Realtime Database**
   - If not already enabled, click **Create Database**
   - Start in **Test mode** initially (we'll apply security rules later)

### 2. Build the App
```bash
cd /Users/yoojinseon/Documents/Develop/AndroidStudioProjects/ConnectMate

# Clean and build
./gradlew clean build

# Or build debug APK
./gradlew assembleDebug
```

### 3. Install on Multiple Devices
You need at least **2 devices** or **1 device + 1 emulator** to test multi-user sync.

**Option A: Physical Devices**
```bash
# List connected devices
adb devices

# Install on specific device
adb -s <device_serial> install app/build/outputs/apk/debug/app-debug.apk
```

**Option B: Emulator + Physical Device**
```bash
# Start emulator
emulator -avd Pixel_6_API_34 &

# Install on emulator
adb -e install app/build/outputs/apk/debug/app-debug.apk

# Install on physical device
adb -d install app/build/outputs/apk/debug/app-debug.apk
```

---

## Test Scenarios

### Test 1: Real-Time Activity Creation & Sync

**Objective:** Verify activities created on one device appear instantly on another

**Steps:**

1. **Setup**
   - Device A: Login with User 1 (e.g., Google account)
   - Device B: Login with User 2 (different account)
   - Both devices: Navigate to Activity List tab

2. **Test**
   - Device A: Tap "+" button → Create activity
     - Title: "Test Sync - Soccer Match"
     - Category: 운동 (Sports)
     - Date/Time: Tomorrow, 3:00 PM
     - Location: "Seoul National Park"
     - Max Participants: 10
     - Tap "Create"

3. **Expected Result**
   - ✅ Device A: Success toast appears, activity list updates instantly
   - ✅ Device B: New activity appears at top of list **within 1-2 seconds**
   - ✅ Both devices: Activity shows same details

4. **Verify on Map**
   - Both devices: Switch to Map tab
   - ✅ New marker appears at activity location
   - ✅ Tap marker shows activity info

---

### Test 2: Real-Time Participant Tracking

**Objective:** Verify participant counts update instantly when users join

**Steps:**

1. **Setup**
   - Device A & B: Both viewing the same activity (from Test 1)
   - Device A: Open activity detail by tapping it

2. **Test**
   - Device A: Note participant count (should be 1 - creator)
   - Device B: Tap same activity → Tap "Join Activity" button
   - Device B: Joins chat room automatically

3. **Expected Result**
   - ✅ Device B: "채팅방에 참여했습니다!" toast appears
   - ✅ Device A: Participant count updates from 1 to 2 **instantly**
   - ✅ Both devices: Activity detail shows "2 / 10 participants"

---

### Test 3: Real-Time Chat Messages

**Objective:** Verify messages sync instantly across devices

**Steps:**

1. **Setup**
   - Device A & B: Both joined same activity chat room (from Test 2)
   - Both devices: Open chat room for the activity

2. **Test**
   - Device A: Type message "Hello from Device A!" → Send
   - Device B: Type message "Hi from Device B!" → Send
   - Device A: Type message "Real-time sync works!" → Send

3. **Expected Result**
   - ✅ Each message appears on both devices **within 1 second**
   - ✅ Messages show correct sender name
   - ✅ Messages appear in chronological order
   - ✅ Own messages align right, others align left
   - ✅ Auto-scroll to bottom on new message

---

### Test 4: Offline Sync & Queue

**Objective:** Verify offline changes sync when reconnected

**Steps:**

1. **Setup**
   - Device A: Connected to internet
   - Device B: Enable Airplane Mode (offline)

2. **Test**
   - Device B (offline): Create new activity
     - Title: "Offline Test Activity"
     - Category: 스터디 (Study)
     - Fill other required fields
     - Tap "Create"

   - Device B: Send message in existing chat
     - Type: "This was sent offline"
     - Tap send

3. **Intermediate Result**
   - ✅ Device B: Shows loading/pending state
   - ✅ Device A: Does NOT see new activity/message yet

4. **Reconnect**
   - Device B: Disable Airplane Mode (back online)
   - Wait 3-5 seconds

5. **Expected Result**
   - ✅ Device A: New activity appears in list
   - ✅ Device A: Message appears in chat room
   - ✅ Device B: Pending indicators disappear
   - ✅ All data synced successfully

---

### Test 5: Map Marker Real-Time Updates

**Objective:** Verify map markers update/remove instantly

**Steps:**

1. **Setup**
   - Device A & B: Both on Map tab
   - Both viewing same area with visible markers

2. **Test - Activity Update**
   - Device A: Go to Activity List → Tap an activity
   - Device A: Tap "Join Activity" button
   - Device A: Return to Map tab

3. **Expected Result**
   - ✅ Device B: Marker info updates (participant count increases)
   - ✅ Both devices: Tap marker shows updated count

4. **Test - Activity Deletion**
   - Device A: Open an activity YOU created
   - Device A: Tap "Delete Activity" → Confirm

5. **Expected Result**
   - ✅ Device A: Returns to activity list, activity removed
   - ✅ Device B: Activity disappears from map **instantly**
   - ✅ Device B: Marker removed from map
   - ✅ Both devices: Activity not in activity list

---

### Test 6: Chat Room List Updates

**Objective:** Verify chat list updates with new messages

**Steps:**

1. **Setup**
   - Device A & B: Both on Chat List tab
   - Both have joined at least 2 chat rooms

2. **Test**
   - Device A: Open chat room "Soccer Match"
   - Device A: Send message "Testing chat list update"
   - Device A: Return to Chat List

3. **Expected Result**
   - ✅ Device B: Chat list updates instantly
   - ✅ "Soccer Match" moves to top of list
   - ✅ Last message preview shows "Testing chat list update"
   - ✅ Timestamp updates to current time

---

### Test 7: Multi-Device Activity Search & Filter

**Objective:** Verify search/filter works with synced data

**Steps:**

1. **Setup**
   - Device A: Create 3 activities with different categories:
     - "Basketball Game" (운동)
     - "Java Study Group" (스터디)
     - "Coffee Meetup" (소셜)

2. **Test**
   - Device B: Go to Activity List
   - Wait for all 3 activities to appear
   - Device B: Tap search icon
   - Device B: Type "Java"

3. **Expected Result**
   - ✅ Device B: Only "Java Study Group" appears in filtered list
   - ✅ Other activities hidden

4. **Test Filter by Category**
   - Device B: Clear search
   - Device B: Tap filter icon → Select "운동" chip

5. **Expected Result**
   - ✅ Device B: Only "Basketball Game" appears
   - ✅ Activities instantly filter

---

### Test 8: Concurrent Edits & Conflict Resolution

**Objective:** Verify last-write-wins for concurrent updates

**Steps:**

1. **Setup**
   - Device A & B: Both viewing same activity detail

2. **Test**
   - Device A: Tap "Join Activity" (participant count: 1 → 2)
   - Device B: Immediately tap "Join Activity" (participant count: 1 → 2)

3. **Expected Result**
   - ✅ Both users successfully added
   - ✅ Final participant count: 3 (creator + A + B)
   - ✅ No duplicate participant entries
   - ✅ Both devices show count: 3

---

## Performance Benchmarks

Test the speed of synchronization:

### Latency Test
1. Device A: Send chat message
2. Measure time until it appears on Device B
   - **Target:** < 1 second
   - **Acceptable:** < 2 seconds

### Activity Creation Test
1. Device A: Create new activity
2. Measure time until it appears on Device B
   - **Target:** < 2 seconds
   - **Acceptable:** < 3 seconds

### Map Marker Test
1. Device A: Create activity with GPS coordinates
2. Measure time until marker appears on Device B map
   - **Target:** < 2 seconds
   - **Acceptable:** < 4 seconds

---

## Troubleshooting

### Problem: Activities/Messages Not Syncing

**Check:**
1. **Internet Connection**
   ```bash
   # On device, check internet
   adb shell ping -c 3 8.8.8.8
   ```

2. **Firebase Authentication**
   ```bash
   # Check logcat for auth errors
   adb logcat | grep "FirebaseAuth"
   ```

3. **Firebase Rules**
   - Go to Firebase Console → Realtime Database → Rules
   - Ensure rules are not blocking reads/writes

4. **App Logs**
   ```bash
   # Check for Firebase errors
   adb logcat | grep -E "Firebase|ChatManager|ActivityManager"
   ```

### Problem: Offline Sync Not Working

**Check:**
1. **Persistence Enabled**
   - Verify `FirebaseDatabase.getInstance().setPersistenceEnabled(true)` is called

2. **Disk Space**
   ```bash
   # Check available storage
   adb shell df /data
   ```

3. **Logs**
   ```bash
   adb logcat | grep "FirebaseDatabase"
   ```

### Problem: Slow Synchronization (> 5 seconds)

**Possible Causes:**
1. **Poor Network Connection**
   - Switch to WiFi
   - Check network speed: `adb shell dumpsys connectivity`

2. **Firebase Overload**
   - Check Firebase Console → Usage
   - Monitor concurrent connections

3. **Too Many Listeners**
   - Review code for listener leaks
   - Ensure `removeAllListeners()` called in `onDestroy()`

---

## Monitoring & Debugging

### View Real-Time Database in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select project: `connect-mate-25`
3. Navigate to **Realtime Database**
4. Watch data update in real-time as you test

### Enable Debug Logging

Add to your test devices:

```bash
# Enable verbose Firebase logging
adb shell setprop log.tag.FirebaseDatabase VERBOSE
adb shell setprop log.tag.FirebaseAuth VERBOSE

# View logs
adb logcat -v time | grep -E "Firebase|ConnectMate"
```

### Monitor Network Traffic

```bash
# Monitor Firebase network calls
adb logcat | grep "FirebaseDatabase: connect"
```

---

## Success Criteria

Your multi-user sync is working correctly if:

- ✅ All 8 test scenarios pass
- ✅ Sync latency < 2 seconds under normal conditions
- ✅ Offline changes sync when reconnected
- ✅ No crashes during concurrent operations
- ✅ Chat messages appear in correct order
- ✅ Participant counts accurate across all devices
- ✅ Map markers update/remove in real-time
- ✅ No duplicate data entries

---

## Next Steps

After successful testing:

1. **Deploy Security Rules**
   ```bash
   firebase deploy --only database
   ```

2. **Monitor Usage**
   - Check Firebase Console → Realtime Database → Usage
   - Set up budget alerts

3. **Performance Testing**
   - Test with 10+ concurrent users
   - Monitor database read/write operations

4. **User Acceptance Testing**
   - Beta test with real users
   - Collect feedback on sync responsiveness

---

## Support

If you encounter issues:

1. Check logs: `adb logcat | grep -E "Firebase|Error"`
2. Review Firebase Console for errors
3. Verify security rules are correctly deployed
4. Ensure all devices are authenticated

For additional help, refer to:
- [Firebase Realtime Database Documentation](https://firebase.google.com/docs/database)
- [Android Firebase Setup Guide](https://firebase.google.com/docs/android/setup)
