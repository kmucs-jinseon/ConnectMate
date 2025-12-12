# Native Debug Symbols - Structure Fix

## Issue
Play Console rejected the initial native debug symbols with error:
```
The native debug symbols contain an invalid directory lib.
Only Android ABIs are supported.
```

## Root Cause
The zip file had an incorrect structure with ABI directories inside a "lib/" folder:

**❌ Incorrect Structure:**
```
native-debug-symbols-1.0.8.zip
└── lib/
    ├── armeabi-v7a/
    │   └── libK3fAndroid.so
    └── arm64-v8a/
        └── libK3fAndroid.so
```

## Fix Applied
Recreated the zip file with ABI directories at the root level:

**✅ Correct Structure:**
```
native-debug-symbols-1.0.8.zip
├── armeabi-v7a/
│   └── libK3fAndroid.so
└── arm64-v8a/
    └── libK3fAndroid.so
```

## How It Was Fixed

```bash
# Remove incorrect zip
rm -f app/build/outputs/native-debug-symbols-1.0.8.zip

# Create correct zip (from inside lib directory)
cd app/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib
zip -r /path/to/native-debug-symbols-1.0.8.zip armeabi-v7a arm64-v8a
```

## Verification

```bash
# Check zip structure
unzip -l app/build/outputs/native-debug-symbols-1.0.8.zip
```

**Output (Correct):**
```
Archive:  native-debug-symbols-1.0.8.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  12-12-2025 12:42   armeabi-v7a/
 16476716  12-12-2025 12:42   armeabi-v7a/libK3fAndroid.so
        0  12-12-2025 12:42   arm64-v8a/
 18787432  12-12-2025 12:42   arm64-v8a/libK3fAndroid.so
---------                     -------
 35264148                     4 files
```

✅ ABI directories are at the root level (no "lib/" parent directory)

## File Information

- **File**: `app/build/outputs/native-debug-symbols-1.0.8.zip`
- **Size**: 12 MB
- **SHA-256**: `f8649f043717f4bc4d15e414045a7a2f832aa749a0cde42703258597a09e369c`
- **Contents**: Kakao Maps SDK native libraries for crash analysis

## Upload to Play Console

1. Go to: https://play.google.com/console/
2. Select: ConnectMate app
3. Navigate to: Release → Production/Testing → Edit release
4. Scroll to: "Native debug symbols" section
5. Upload: `native-debug-symbols-1.0.8.zip`
6. Verify: No error message appears
7. Continue with release

## Why This Matters

Native debug symbols help Google Play Console:
- Deobfuscate native crash stack traces
- Provide detailed crash reports for Kakao Maps SDK crashes
- Improve crash analysis and debugging capabilities

---

**Status**: ✅ Fixed - Ready for Play Console upload
**Last Updated**: 2025-12-12
