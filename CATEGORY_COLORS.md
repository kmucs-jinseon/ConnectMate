# Category Color Mappings

This file documents the category colors used throughout the app for your reference when making modifications.

## Category Color Definitions

### Light Mode (values/colors.xml)
```xml
<!-- Pastel Category Colors (Light & Less Saturated) -->
<color name="pastel_green">#A7F4D8</color>       <!-- Sports - Pastel Mint Green -->
<color name="pastel_teal">#99E2DA</color>        <!-- Outdoor - Pastel Teal -->
<color name="pastel_blue">#9FC9F8</color>        <!-- Study - Pastel Blue -->
<color name="pastel_pink">#FAA4CE</color>        <!-- Culture - Pastel Pink -->
<color name="pastel_amber">#FFE5A0</color>       <!-- Social - Pastel Yellow/Amber -->
<color name="pastel_peach">#FFC386</color>       <!-- Food - Pastel Peach -->
<color name="pastel_sky">#A2D9E6</color>         <!-- Travel - Pastel Sky Blue -->
<color name="pastel_lavender">#D5AAFF</color>    <!-- Game - Pastel Lavender -->
<color name="pastel_lemon">#FFFAAC</color>       <!-- Hobby - Pastel Lemon -->
<color name="pastel_rose">#F69DA5</color>        <!-- Volunteer - Pastel Rose -->
<color name="pastel_gray">#D4D4D4</color>        <!-- Other - Pastel Gray -->
```

### Dark Mode (values-night/colors.xml)
```xml
<!-- Dark Mode Category Colors (Pretty pastels that work with dark text) -->
<color name="pastel_green">#7FD4B8</color>       <!-- Sports - Soft Mint -->
<color name="pastel_teal">#80CBC4</color>        <!-- Outdoor - Soft Teal -->
<color name="pastel_blue">#90CAF9</color>        <!-- Study - Soft Blue -->
<color name="pastel_pink">#F48FB1</color>        <!-- Culture - Soft Pink -->
<color name="pastel_amber">#FFD54F</color>       <!-- Social - Soft Yellow -->
<color name="pastel_peach">#FFAB91</color>       <!-- Food - Soft Peach -->
<color name="pastel_sky">#81D4FA</color>         <!-- Travel - Soft Sky Blue -->
<color name="pastel_lavender">#CE93D8</color>    <!-- Game - Soft Lavender -->
<color name="pastel_lemon">#FFF59D</color>       <!-- Hobby - Soft Lemon -->
<color name="pastel_rose">#F48FB1</color>        <!-- Volunteer - Soft Rose -->
<color name="pastel_gray">#B0BEC5</color>        <!-- Other - Soft Gray -->
```

## Category Mapping

| Category | Korean | Light Color | Dark Color |
|----------|--------|-------------|------------|
| Sports | 운동 | #A7F4D8 | #7FD4B8 |
| Outdoor | 야외활동 | #99E2DA | #80CBC4 |
| Study | 스터디 | #9FC9F8 | #90CAF9 |
| Culture | 문화 | #FAA4CE | #F48FB1 |
| Social | 소셜 | #FFE5A0 | #FFD54F |
| Food | 맛집 | #FFC386 | #FFAB91 |
| Travel | 여행 | #A2D9E6 | #81D4FA |
| Game | 게임 | #D5AAFF | #CE93D8 |
| Hobby | 취미 | #FFFAAC | #FFF59D |
| Volunteer | 봉사 | #F69DA5 | #F48FB1 |
| Other | 기타 | #D4D4D4 | #B0BEC5 |

## Files to Modify

To change category colors, update these files:
- `/app/src/main/res/values/colors.xml` (Light mode colors)
- `/app/src/main/res/values-night/colors.xml` (Dark mode colors)

## Color Guidelines

**Light Mode:**
- Use lighter, more saturated pastels
- Text on these backgrounds uses dark text (#111827 or #374151)

**Dark Mode:**
- Use slightly darker, softer pastels
- Text on these backgrounds uses black text (#000000)
- Both modes use 2dp outline strokes for better visibility
