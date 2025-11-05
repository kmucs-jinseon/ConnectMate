#!/bin/bash

# Upload ConnectMate APK to Firebase App Distribution
# Make sure you've run 'firebase login' first!

echo "========================================="
echo "Uploading ConnectMate APK to Firebase"
echo "========================================="
echo ""
echo "Project: connect-mate-25"
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
echo ""

# Upload to Firebase App Distribution
firebase appdistribution:distribute \
  app/build/outputs/apk/debug/app-debug.apk \
  --app 1:697385814271:android:4384d77104a8c259dba4ca \
  --release-notes "Debug build - $(date '+%Y-%m-%d %H:%M:%S')

Features:
- Map view with Kakao Maps integration
- User authentication (Google, Kakao, Naver)
- Activity listings
- Chat functionality
- Profile management

Note: Map requires real Android device (won't render on emulator)" \
  --groups "testers"

echo ""
echo "========================================="
echo "Upload complete!"
echo "========================================="
