# Firebase Setup Guide - Enable Multi-User Sync

## Step 1: Enable Firebase Realtime Database

### Option A: Using Firebase Console (Recommended)

1. **Go to Firebase Console**
   - Open: https://console.firebase.google.com
   - Select your project: **connect-mate-25**

2. **Navigate to Realtime Database**
   - In the left sidebar, click **Build** → **Realtime Database**

3. **Create Database**
   - If you see "Get Started", click it
   - Choose a location:
     - **asia-southeast1** (Singapore) - Recommended for Korea
     - Or **us-central1** (US) if you prefer
   - Click **Next**

4. **Choose Security Rules Mode**
   - Select **"Start in test mode"** (for now)
   - This allows read/write access for 30 days
   - Click **Enable**

5. **Your database is now created!**
   - You should see: `https://connect-mate-25-default-rtdb.firebaseio.com/`

---

## Step 2: Deploy Security Rules (Important!)

Test mode expires in 30 days. Deploy proper security rules now:

### Using Firebase CLI

```bash
# 1. Install Firebase CLI (if not installed)
npm install -g firebase-tools

# 2. Login to Firebase
firebase login

# 3. Initialize Firebase in your project
cd /Users/yoojinseon/Documents/Develop/AndroidStudioProjects/ConnectMate
firebase init database

# When prompted:
# - "What file should be used for Realtime Database Security Rules?"
#   → Press Enter (use default: database.rules.json)
# - "File database.rules.json already exists. Do you want to overwrite it?"
#   → Type "N" and press Enter (we already have the rules file)

# 4. Deploy the security rules
firebase deploy --only database
```

### Manual Upload (Alternative)

If you don't want to use CLI:

1. Go to Firebase Console → Realtime Database → **Rules** tab
2. Copy the content from `database.rules.json` file
3. Paste it into the Firebase Console
4. Click **Publish**

---

## Step 3: Verify Database Structure

After deploying, your Firebase Realtime Database should have this structure:

```json
connect-mate-25-default-rtdb
├── users/
├── activities/
├── userActivities/
├── chatRooms/
└── messages/
```

Initially, all will be empty until users start creating activities and sending messages.

---

## Step 4: Test the Connection

### Quick Test in Firebase Console

1. Go to Firebase Console → Realtime Database → **Data** tab
2. Click the **+** icon next to your database URL
3. Add a test value:
   - Name: `test`
   - Value: `hello`
4. Click **Add**
5. If you see the data appear, your database is working! ✅
6. Delete the test data (click the `x` next to `test`)

### Test from Your App

1. **Install the updated APK:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Open the app and create an activity:**
   - Login to the app
   - Tap the **+** button
   - Fill in:
     - Title: "Test Activity"
     - Category: Select any category
   - Tap **"Create"** button

3. **Check Firebase Console:**
   - Go to Realtime Database → Data tab
   - You should see new data under `activities/`!
   - Look for your "Test Activity"

---

## Step 5: Monitor in Real-Time

### Watch Multi-Device Sync

1. **Install on Device 2:**
   ```bash
   # Find device serials
   adb devices

   # Install on specific device
   adb -s <device2_serial> install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Test Sync:**
   - Device 1: Login, create activity
   - Device 2: Login, **should see activity appear within 1-2 seconds!** 🎉

3. **Watch Firebase Console:**
   - Keep Firebase Console open on **Data** tab
   - Watch data appear in real-time as you use the app!

---

## Current Security Rules Preview

Your `database.rules.json` includes:

✅ **Users** - Can read/write their own profile
✅ **Activities** - Anyone can read, authenticated users can create
✅ **Chat Rooms** - Members-only access
✅ **Messages** - Chat room members only
✅ **Participants** - Users can add/remove themselves

---

## Troubleshooting

### Error: "Permission Denied"

**Cause:** Database not enabled or rules too restrictive

**Fix:**
1. Check Firebase Console → Realtime Database is enabled
2. Temporarily use test mode rules:
   ```json
   {
     "rules": {
       ".read": true,
       ".write": true
     }
   }
   ```
3. **IMPORTANT:** Change back to secure rules after testing!

### Error: "Database not found"

**Cause:** Database URL mismatch

**Fix:**
1. Check `google-services.json` has correct database URL
2. Verify Firebase project is `connect-mate-25`

### No Data Appearing

**Cause:** App not connected to Firebase

**Fix:**
```bash
# Check logs for Firebase errors
adb logcat | grep -E "Firebase|FIREBASE"

# Look for connection errors or auth issues
```

### Rules Not Working

**Cause:** Rules not deployed

**Fix:**
```bash
# Redeploy rules
firebase deploy --only database --force
```

---

## Next Steps After Setup

Once Firebase is enabled:

1. ✅ Create activities on one device → appear on another
2. ✅ Send messages in chat → appear instantly for all members
3. ✅ Join activities → participant count updates in real-time
4. ✅ Works offline → syncs when reconnected

---

## Quick Commands Reference

```bash
# Login to Firebase
firebase login

# Initialize database (first time only)
firebase init database

# Deploy security rules
firebase deploy --only database

# Check Firebase project
firebase projects:list

# View current rules
firebase database:get /.settings/rules

# Test from command line
firebase database:get /

# Install app on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Watch logs
adb logcat | grep -E "Firebase|CreateActivity|ChatRoom"
```

---

## Security Best Practices

**For Production:**

1. ✅ Use the security rules in `database.rules.json` (NOT test mode)
2. ✅ Enable Firebase App Check
3. ✅ Monitor Firebase Console → Usage → Database
4. ✅ Set up budget alerts
5. ✅ Enable Firebase Authentication (already done!)

**Never do this in production:**
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```
This allows anyone to read/write your database! ❌

---

## Support

If you encounter issues:

1. Check Firebase Console → Realtime Database → Usage tab
2. View logs: `adb logcat | grep Firebase`
3. Verify `google-services.json` is up to date
4. Check network connectivity

Your app is ready for multi-user sync once Firebase is enabled! 🚀
