# Firebase Storage Setup Instructions

## Issue: Image Upload Failed

The profile photo upload is failing because Firebase Storage rules need to be configured.

## Solution: Configure Firebase Storage Rules

### Step 1: Access Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **ConnectMate** project
3. Click **Storage** in the left sidebar
4. Click **Rules** tab at the top

### Step 2: Update Storage Rules

Replace the current rules with the following:

```
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {
    // Allow authenticated users to read all files
    match /{allPaths=**} {
      allow read: if request.auth != null;
    }
    
    // Allow users to upload their own profile images
    match /profile_images/{userId}.jpg {
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Alternative: Allow all authenticated users to write (less secure)
    // Uncomment this if you want to allow any logged-in user to upload
    // match /{allPaths=**} {
    //   allow write: if request.auth != null;
    // }
  }
}
```

### Step 3: Publish Rules

1. Click **Publish** button
2. Confirm the changes

## What These Rules Do

### Current Rules (Secure)
- ✅ Any authenticated user can **read** all files
- ✅ Users can **only upload their own profile photo**
- ✅ File name must match their user ID: `profile_images/{userId}.jpg`
- ❌ Users cannot upload other users' photos

### Alternative Rules (Less Secure)
- ✅ Any authenticated user can read/write any file
- ⚠️ Less secure but easier for testing

## Testing After Setup

1. **Build and run** the app
2. Go to **Settings → Edit Profile**
3. **Select a new photo**
4. Click **Save**
5. Watch LogCat for:
   ```
   EditProfileActivity: Uploading new profile image to Firebase Storage...
   EditProfileActivity: Upload progress: 50%
   EditProfileActivity: Upload progress: 100%
   EditProfileActivity: Image uploaded successfully to Storage
   EditProfileActivity: Download URL obtained: https://firebasestorage...
   ```

## Troubleshooting

### Still getting "Image upload failed"?

Check LogCat for specific error messages:

```bash
adb logcat | grep EditProfileActivity
```

Common errors:

1. **Permission Denied**
   - Solution: Make sure Storage rules allow write access
   - Check that user is authenticated

2. **Unauthorized**
   - Solution: User is not logged in with Firebase Auth
   - For social login (Kakao/Naver), rules might need adjustment

3. **Network Error**
   - Solution: Check internet connection
   - Try again

## Firebase Storage Structure

After upload, files will be stored at:

```
gs://your-project.appspot.com/
  └── profile_images/
      ├── firebase_abc123.jpg
      ├── kakao_def456.jpg
      └── naver_ghi789.jpg
```

## Security Notes

- Profile images are stored with userId as filename
- Only authenticated users can upload
- Users can only modify their own profile image
- All users can view all profile images (needed for chat)

---

**After configuring these rules, the profile photo upload should work!** 🎉
