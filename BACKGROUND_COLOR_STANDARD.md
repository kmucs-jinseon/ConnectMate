# Background Color Standards

This document outlines the consistent background color usage across all pages in the ConnectMate app.

## Standard Background Pattern

Following the style from `fragment_activity_list.xml`, all pages now use:

### Main Containers
- Use: `android:background="?attr/colorSurface"`
- Examples: CoordinatorLayout, LinearLayout, ScrollView (main containers)

### Headers/Toolbars
- Use: `android:background="@color/primary_blue"`
- Examples: Toolbar, AppBarLayout headers

### App Bars (without blue header)
- Use: `android:background="?attr/colorSurface"`
- Examples: AppBarLayout containers

## Why `?attr/colorSurface` instead of hardcoded colors?

Using `?attr/colorSurface` allows automatic theme switching:
- **Light Mode**: Resolves to white (#FFFFFF)
- **Dark Mode**: Resolves to gray_50 (#2C2C2C)

## Files Updated

All layout files have been updated to follow this standard:

### Activity Layouts
- activity_about.xml
- activity_chat_room.xml
- activity_detail.xml
- activity_edit_profile.xml
- activity_friends.xml
- activity_login.xml
- activity_map.xml
- activity_oauth_webview.xml
- activity_profile_setup.xml
- activity_signup.xml

### Fragment Layouts
- fragment_activity_list.xml (reference)
- fragment_chat.xml
- fragment_create_activity.xml
- fragment_map.xml
- fragment_notification_settings.xml
- fragment_profile.xml
- fragment_profile_setup.xml
- fragment_settings.xml
- fragment_user_reviews.xml

## Special Cases

### Map Background
- `activity_map.xml` main container: Uses `@color/gray_500` (intentional for map view)
- App bar still uses `?attr/colorSurface`

### Drawable Backgrounds
Some UI elements use drawable backgrounds for specific styling:
- `@drawable/rounded_background`
- `@drawable/bg_close_button_light`
- `@drawable/notification_badge_background`

These are intentional and should remain as-is.

## Theme-Aware Colors

| Color Name | Light Mode | Dark Mode |
|------------|------------|-----------|
| ?attr/colorSurface | #FFFFFF | #2C2C2C |
| @color/primary_blue | #307CD4 | #5A9CFF |
| @color/text_primary | #111827 | #E0E0E0 |
| @color/text_secondary | #6B7280 | #A0A0A0 |
| @color/background | #F9FAFB | #1E1E1E |

## Future Development

When creating new layouts:
1. Always use `?attr/colorSurface` for main containers
2. Use `@color/primary_blue` for headers/toolbars
3. Use `@color/text_primary` and `@color/text_secondary` for text
4. Avoid hardcoded white (#FFFFFF) or black (#000000) except for specific design elements

## Testing

To verify the changes work correctly:
1. Run the app in light mode - all pages should have white/light backgrounds
2. Switch to dark mode via Profile → Theme Settings
3. All pages should automatically adapt to dark backgrounds
4. Headers should remain blue in both modes
