# How to Clear Session and Start Fresh

## Option 1: Clear App Data (Recommended for Testing)
1. Open Android Settings
2. Go to Apps → ConnectMate
3. Tap "Storage"
4. Tap "Clear Data" and "Clear Cache"
5. Relaunch the app → Will start at Login screen ✓

## Option 2: Uninstall and Reinstall
```bash
adb uninstall com.example.connectmate
./gradlew installDebug
```

## Option 3: Add Logout Function (Permanent Solution)
- I can add a logout button in the profile/settings screen
- This will let you sign out properly

Which option would you like me to implement?
