package com.example.connectmate.utils;

import com.example.connectmate.R;

/**
 * Utility class to map Kakao place categories to ConnectMate activity categories
 */
public class CategoryMapper {

    // Activity categories used in the app
    public static final String CATEGORY_SPORTS = "운동";
    public static final String CATEGORY_OUTDOOR = "야외활동";
    public static final String CATEGORY_STUDY = "스터디";
    public static final String CATEGORY_CULTURE = "문화";
    public static final String CATEGORY_SOCIAL = "소셜";
    public static final String CATEGORY_FOOD = "맛집";
    public static final String CATEGORY_TRAVEL = "여행";
    public static final String CATEGORY_GAME = "게임";
    public static final String CATEGORY_HOBBY = "취미";
    public static final String CATEGORY_VOLUNTEER = "봉사";
    public static final String CATEGORY_OTHER = "기타";

    /**
     * Map Kakao category to ConnectMate activity category
     * Kakao categories format: "음식점 > 카페", "문화,예술 > 공원" etc.
     */
    public static String mapKakaoCategoryToActivity(String kakaoCategory) {
        if (kakaoCategory == null || kakaoCategory.isEmpty()) {
            return CATEGORY_OTHER;
        }

        String lower = kakaoCategory.toLowerCase();

        // Sports & Fitness - 운동
        if (lower.contains("운동") || lower.contains("체육") || lower.contains("헬스") ||
            lower.contains("fitness") || lower.contains("gym") || lower.contains("스포츠") ||
            lower.contains("요가") || lower.contains("필라테스") || lower.contains("수영") ||
            lower.contains("볼링") || lower.contains("당구") || lower.contains("골프")) {
            return CATEGORY_SPORTS;
        }

        // Outdoor Activities - 야외활동
        if (lower.contains("공원") || lower.contains("park") || lower.contains("자연") ||
            lower.contains("등산") || lower.contains("캠핑") || lower.contains("산") ||
            lower.contains("강") || lower.contains("바다") || lower.contains("해변") ||
            lower.contains("낚시") || lower.contains("야외")) {
            return CATEGORY_OUTDOOR;
        }

        // Study & Education - 스터디
        if (lower.contains("학교") || lower.contains("교육") || lower.contains("학원") ||
            lower.contains("도서관") || lower.contains("library") || lower.contains("서점") ||
            lower.contains("독서실") || lower.contains("스터디") || lower.contains("study")) {
            return CATEGORY_STUDY;
        }

        // Culture & Arts - 문화
        if (lower.contains("문화") || lower.contains("예술") || lower.contains("미술관") ||
            lower.contains("박물관") || lower.contains("museum") || lower.contains("gallery") ||
            lower.contains("영화") || lower.contains("theater") || lower.contains("극장") ||
            lower.contains("공연") || lower.contains("전시")) {
            return CATEGORY_CULTURE;
        }

        // Food & Dining - 맛집
        if (lower.contains("음식점") || lower.contains("restaurant") || lower.contains("카페") ||
            lower.contains("cafe") || lower.contains("디저트") || lower.contains("dessert") ||
            lower.contains("베이커리") || lower.contains("주점") || lower.contains("술집") ||
            lower.contains("bar") || lower.contains("한식") || lower.contains("중식") ||
            lower.contains("일식") || lower.contains("양식") || lower.contains("분식")) {
            return CATEGORY_FOOD;
        }

        // Travel & Accommodation - 여행
        if (lower.contains("숙박") || lower.contains("호텔") || lower.contains("hotel") ||
            lower.contains("모텔") || lower.contains("펜션") || lower.contains("리조트") ||
            lower.contains("게스트하우스") || lower.contains("관광") || lower.contains("여행") ||
            lower.contains("tourist") || lower.contains("명소")) {
            return CATEGORY_TRAVEL;
        }

        // Game & Entertainment - 게임
        if (lower.contains("게임") || lower.contains("game") || lower.contains("pc방") ||
            lower.contains("오락") || lower.contains("vr") || lower.contains("플레이스테이션") ||
            lower.contains("노래방") || lower.contains("karaoke") || lower.contains("방탈출")) {
            return CATEGORY_GAME;
        }

        // Hobby - 취미
        if (lower.contains("취미") || lower.contains("hobby") || lower.contains("공방") ||
            lower.contains("workshop") || lower.contains("클래스") || lower.contains("만들기") ||
            lower.contains("수제") || lower.contains("도예") || lower.contains("그림") ||
            lower.contains("음악") || lower.contains("댄스") || lower.contains("dance")) {
            return CATEGORY_HOBBY;
        }

        // Volunteer & Community Service - 봉사
        if (lower.contains("봉사") || lower.contains("volunteer") || lower.contains("복지") ||
            lower.contains("자원봉사") || lower.contains("기부") || lower.contains("나눔") ||
            lower.contains("charity") || lower.contains("welfare")) {
            return CATEGORY_VOLUNTEER;
        }

        // Social & Community - 소셜
        if (lower.contains("커뮤니티") || lower.contains("community") || lower.contains("모임") ||
            lower.contains("meeting") || lower.contains("동호회") || lower.contains("club") ||
            lower.contains("파티") || lower.contains("party") || lower.contains("이벤트")) {
            return CATEGORY_SOCIAL;
        }

        // Default to Other
        return CATEGORY_OTHER;
    }

    /**
     * Get icon resource ID for a category
     */
    public static int getCategoryIcon(String category) {
        if (category == null) return R.drawable.ic_category_other;

        switch (category) {
            case CATEGORY_SPORTS:
                return R.drawable.ic_category_sports;
            case CATEGORY_OUTDOOR:
                return R.drawable.ic_category_outdoor;
            case CATEGORY_STUDY:
                return R.drawable.ic_category_study;
            case CATEGORY_CULTURE:
                return R.drawable.ic_category_culture;
            case CATEGORY_SOCIAL:
                return R.drawable.ic_category_social;
            case CATEGORY_FOOD:
                return R.drawable.ic_category_food;
            case CATEGORY_TRAVEL:
                return R.drawable.ic_category_travel;
            case CATEGORY_GAME:
                return R.drawable.ic_category_game;
            case CATEGORY_HOBBY:
                return R.drawable.ic_category_hobby;
            case CATEGORY_VOLUNTEER:
                return R.drawable.ic_category_volunteer;
            case CATEGORY_OTHER:
            default:
                return R.drawable.ic_category_other;
        }
    }

    /**
     * Get color resource ID for a category
     */
    public static int getCategoryColor(String category) {
        if (category == null) return R.color.gray_500;

        switch (category) {
            case CATEGORY_SPORTS:
                return R.color.green_500;
            case CATEGORY_OUTDOOR:
                return R.color.teal_500;
            case CATEGORY_STUDY:
                return R.color.indigo_500;
            case CATEGORY_CULTURE:
                return R.color.pink_500;
            case CATEGORY_SOCIAL:
                return R.color.amber_500;
            case CATEGORY_FOOD:
                return R.color.orange_500;
            case CATEGORY_TRAVEL:
                return R.color.cyan_500;
            case CATEGORY_GAME:
                return R.color.purple_500;
            case CATEGORY_HOBBY:
                return R.color.yellow_500;
            case CATEGORY_VOLUNTEER:
                return R.color.red_500;
            case CATEGORY_OTHER:
            default:
                return R.color.gray_500;
        }
    }
}
