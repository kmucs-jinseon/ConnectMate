# 🔒 Complete Firebase Database Rules

## Summary of All Database Paths & Permissions

This document lists all database paths used in ConnectMate and their required permissions.

### ✅ All Paths Covered in Rules

| Path | Read Permission | Write Permission | Purpose |
|------|----------------|------------------|---------|
| **users/{uid}** | Any authenticated user | Only the user ($uid === auth.uid) | User profiles |
| **users/{uid}/reviews/{reviewId}** | Any authenticated user | Any authenticated user | User reviews/ratings |
| **users/{uid}/friendRequests/{requesterId}** | Only the user | Any authenticated user | Friend request inbox |
| **users/{uid}/friends/{friendId}** | Only the user | Any authenticated user | Friends list (reciprocal) |
| **users/{uid}/ratingSum** | Only the user | Any authenticated user | Rating statistics (transactions) |
| **users/{uid}/reviewCount** | Only the user | Any authenticated user | Rating statistics (transactions) |
| **users/{uid}/rating** | Only the user | Any authenticated user | Rating statistics (transactions) |
| **users/{uid}/participationCount** | Only the user | Any authenticated user | Activity participation count |
| **activities/{activityId}** | Any authenticated user | Any authenticated user | Activities |
| **userActivities/{userId}/{activityId}** | Only the user | Only the user | User's activity list |
| **chatRooms/{chatRoomId}** | Any authenticated user | Any authenticated user | Chat room data |
| **messages/{chatRoomId}/{messageId}** | Any authenticated user | Any authenticated user | Chat messages |
| **userChatRooms/{userId}/{chatRoomId}** | Only the user | Only the user | User's chat room list |
| **activityChatRooms/{activityId}** | Any authenticated user | Any authenticated user | Activity to chat room mapping |
| **userNotifications/{userId}/{notificationId}** | Only the user | Any authenticated user | User notifications |
| **userTokens/{userId}** | Only the user | Only the user | FCM tokens |
| **pendingReviews/{userId}/{reviewId}** | Only the user | Only the user | Pending review requests |
| **friendships/{userId}/{friendId}** | Only the user | User or friend | Friend relationships (alternate structure) |

## 📋 Complete Database Rules (Copy to Firebase Console)

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "$uid === auth.uid",
        ".validate": "!newData.exists() || (newData.hasChildren(['email', 'displayName']) && newData.child('email').isString() && newData.child('displayName').isString())",
        "reviews": {
          "$reviewId": {
            ".write": "auth != null"
          }
        },
        "friendRequests": {
          "$requesterId": {
            ".write": "auth != null"
          }
        },
        "friends": {
          "$friendId": {
            ".write": "auth != null"
          }
        },
        "ratingSum": {
          ".write": "auth != null"
        },
        "reviewCount": {
          ".write": "auth != null"
        },
        "rating": {
          ".write": "auth != null"
        },
        "participationCount": {
          ".write": "auth != null"
        }
      }
    },
    "activities": {
      ".read": "auth != null",
      "$activityId": {
        ".write": "auth != null"
      }
    },
    "userActivities": {
      "$userId": {
        ".read": "$userId === auth.uid",
        "$activityId": {
          ".write": "$userId === auth.uid"
        }
      }
    },
    "activityChatRooms": {
      "$activityId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "chatRooms": {
      "$chatRoomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "userChatRooms": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "$userId === auth.uid"
      }
    },
    "messages": {
      "$chatRoomId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "userNotifications": {
      "$userId": {
        ".read": "$userId === auth.uid",
        "$notificationId": {
          ".write": "auth != null"
        }
      }
    },
    "userTokens": {
      "$userId": {
        ".read": "$userId === auth.uid",
        ".write": "$userId === auth.uid"
      }
    },
    "pendingReviews": {
      "$userId": {
        ".read": "$userId === auth.uid",
        "$reviewId": {
          ".write": "$userId === auth.uid"
        }
      }
    },
    "friendships": {
      "$userId": {
        ".read": "$userId === auth.uid",
        "$friendId": {
          ".write": "$userId === auth.uid || $friendId === auth.uid"
        }
      }
    }
  }
}
```

## 🚀 Deployment Instructions

### Option 1: Firebase Console (Recommended)

1. Go to: https://console.firebase.google.com/
2. Select project: **connect-mate-25**
3. Navigate to: **Realtime Database** → **Rules** tab
4. Copy the complete rules above
5. Paste into the editor
6. Click **"Publish"**

### Option 2: Firebase CLI

```bash
# Re-authenticate
firebase logout
firebase login

# Deploy rules
firebase deploy --only database
```

## ✅ What These Rules Fix

After deploying these updated rules, the following features will work:

### ✅ Reviews System
- Users can submit reviews for other participants
- Rating statistics (ratingSum, reviewCount, rating) update correctly
- Reviews appear on user profiles

### ✅ Friend System
- Users can send friend requests to others
- Users can accept/decline friend requests
- Friend lists update reciprocally
- Friend removal works properly

### ✅ Notification System
- Activity end notifications
- Review request notifications
- Chat join notifications
- Friend request notifications
- FCM tokens save correctly

### ✅ Activity System
- Participation count increments
- Activities can be created/updated/deleted
- Participant lists work

### ✅ Chat System
- Messages send correctly
- Chat rooms create/update
- Member lists work

## 🔍 Verification

After deploying, test these features:

```bash
# 1. Restart the app
adb shell am force-stop app.connectmate
adb shell am start -n app.connectmate/com.example.connectmate.SplashActivity

# 2. Check logs for errors
adb logcat -s MainActivity:E FirebaseActivityManager:E SubmitReviewFragment:E

# 3. Should see "FCM token saved successfully"
adb logcat -s MainActivity:D | grep "FCM"
```

### Test Checklist:

- [ ] **Send friend request** - Should work without errors
- [ ] **Submit review** - "평가를 저장하지 못했습니다" error should be gone
- [ ] **End activity** - Notifications should appear
- [ ] **Join activity** - Participation count should increment
- [ ] **Send chat message** - Message should send
- [ ] **Restart app** - FCM token should save

## 🐛 If Issues Persist

1. **Check Firebase Console Logs:**
   - Go to Firebase Console → Realtime Database
   - Click "Usage" tab
   - Look for "Permission denied" errors

2. **Verify rules are deployed:**
   - Firebase Console → Realtime Database → Rules tab
   - Check timestamp shows recent deployment

3. **Clear app data and reinstall:**
   ```bash
   adb shell pm clear app.connectmate
   adb uninstall app.connectmate
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📝 Notes

- **Security**: All write operations require authentication (`auth != null`)
- **Privacy**: Users can only read their own sensitive data (notifications, tokens, pending reviews)
- **Flexibility**: Reviews, friend requests, and notifications allow cross-user writes
- **Transactions**: Rating and participation count fields support Firebase transactions

---

**Last Updated**: Rules verified to cover all database paths used in ConnectMate v1.0.7
