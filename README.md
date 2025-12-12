# ConnectMate

> 새로운 친구를 만나고, 함께 활동하고, 일상을 더 즐겁게 만들어보세요

혼자 하기 아쉬운 활동, 새로운 취미를 함께할 친구를 찾고 계신가요? ConnectMate는 같은 관심사를 가진 사람들을 쉽게 만나고 다양한 활동을 함께 즐길 수 있는 소셜 플랫폼입니다.

---

## 목차

- [주요 기능](#-주요-기능)
- [타겟 사용자](#-타겟-사용자)
- [기술 스택 및 SDK 버전](#-기술-스택-및-sdk-버전)
- [프로젝트 구조](#-프로젝트-구조)
- [설치 및 설정](#-설치-및-설정)
- [앱 실행 시 고려사항](#-앱-실행-시-고려사항)
- [권한 설명](#-권한-설명)
- [보안 및 개인정보](#-보안-및-개인정보)
- [연락처](#-연락처)
- [감사의 말](#-감사의-말)

---

## 주요 기능

### 지도 기반 활동 탐색
- **카카오맵 통합**: 실시간으로 주변 활동을 지도에서 확인
- **위치 기반 매칭**: 내 주변에서 진행되는 활동 자동 탐색
- **POI 정보**: 장소 클릭 시 상세 정보 (주소, 전화번호, 거리) 표시
- **커스텀 마커**: 활동별 카테고리에 맞는 마커 표시

### 활동 생성 및 참여
- **간편한 활동 만들기**: 지도에서 장소 선택 후 바로 활동 생성
- **다양한 카테고리**: 식사, 카페, 운동, 스터디, 취미 등
- **실시간 업데이트**: Firebase를 통한 실시간 활동 현황 반영
- **참여자 관리**: 활동 참여 신청 및 수락 기능
- **리뷰 시스템**: 활동 종료 후 참여자 리뷰 작성

### 실시간 채팅
- **그룹 채팅**: 활동 참여자들과 실시간 대화
- **미디어 공유**: 사진, 이미지, 파일 공유 기능
- **채팅방 알림**: 새 메시지 및 읽지 않은 메시지 표시
- **채팅 히스토리**: 이전 대화 내용 보관

### 소셜 기능
- **친구 관리**: 친구 추가 요청 및 수락
- **프로필 커스터마이징**: 프로필 사진, 닉네임, 소개, MBTI 설정
- **활동 히스토리**: 내가 참여한 활동 기록 확인
- **사용자 평점**: 활동 참여 후 상호 평가 시스템
- **배지 시스템**: 활동 참여에 따른 배지 획득

### 다양한 로그인 옵션
- Google 계정으로 로그인
- 카카오 계정으로 로그인
- 네이버 계정으로 로그인
- 이메일/비밀번호 로그인

### 추가 기능
- **다크 모드**: 라이트/다크 테마 전환
- **검색 기능**: 사용자 및 장소 검색
- **알림 시스템**: 활동 및 친구 요청 알림
- **계정 삭제**: CCPA 준수 계정 삭제 기능

---

## 타겟 사용자

ConnectMate는 다음과 같은 분들을 위해 만들어졌습니다:

- **대학생**: 캠퍼스에서 같이 식사하거나 공부할, 또는 같이 놀 친구를 찾는 학생들
- **직장인**: 점심/저녁 식사 메이트, 다양한 활동을 공유할 사람을 찾는 직장인
- **새로운 지역 거주자**: 새로운 동네에서 친구를 만들고 싶은 분들
- **취미 공유자**: 같은 취미를 가진 사람들과 만나고 싶은 분들
- **맛집 탐방가**: 혼자 가기 아까운 맛집을 함께 갈 사람을 찾는 분들

---

## 기술 스택 및 SDK 버전

### Android 빌드 설정

| 항목 | 버전 |
|------|------|
| Compile SDK | 35 (Android 15) |
| Target SDK | 35 (Android 15) |
| Min SDK | 24 (Android 7.0 Nougat) |
| Java Version | 11 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.1 |
| Kotlin | 1.9.23 |

### 앱 버전 정보

| 항목 | 값 |
|------|-----|
| Version Name | 1.0.6 |
| Version Code | 7 |
| Application ID | app.connectmate |

### AndroidX 및 Core 라이브러리

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| androidx.appcompat:appcompat | 1.6.1 | 하위 호환성 지원 |
| androidx.activity:activity | 1.8.0 | Activity 컴포넌트 |
| androidx.core:core-ktx | 1.13.1 | Kotlin 확장 함수 |
| androidx.core:core-splashscreen | 1.0.1 | 스플래시 화면 |
| androidx.constraintlayout:constraintlayout | 2.1.4 | 레이아웃 관리 |
| com.google.android.material:material | 1.10.0 | Material Design UI |

### Firebase (BOM 32.7.0)

| 서비스 | 용도 |
|--------|------|
| firebase-auth | 사용자 인증 |
| firebase-database | Realtime Database |
| firebase-firestore | Cloud Firestore |
| firebase-storage | 파일 저장소 |

### 카카오 SDK

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| com.kakao.sdk:v2-user | 2.22.0 | 카카오 로그인 |
| com.kakao.maps.open:android | 2.12.18 | 카카오맵 |
| Kakao Local API | - | 장소 검색 |

### 네이버 SDK

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| com.navercorp.nid:oauth | 5.9.1 | 네이버 로그인 |

### Google Play Services

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| play-services-auth | 20.7.0 | Google 로그인 |
| play-services-location | 21.0.1 | 위치 서비스 |

### 이미지 및 네트워크

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| com.github.bumptech.glide:glide | 4.16.0 | 이미지 로딩/캐싱 |
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP 클라이언트 |
| com.google.code.gson:gson | 2.10.1 | JSON 파싱 |

### UI 컴포넌트

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| de.hdodenhof:circleimageview | 3.1.0 | 원형 프로필 이미지 |

### 테스트

| 라이브러리 | 버전 |
|-----------|------|
| junit:junit | 4.13.2 |
| androidx.test.ext:junit | 1.1.5 |
| androidx.test.espresso:espresso-core | 3.5.1 |

---

## 프로젝트 구조

```
ConnectMate/
├── app/                                    # Android 네이티브 앱
│   ├── src/main/
│   │   ├── java/com/example/connectmate/
│   │   │   ├── Activities/                 # Activity 클래스들
│   │   │   │   ├── SplashActivity.java     # 스플래시 화면
│   │   │   │   ├── LoginActivity.java      # 로그인 화면
│   │   │   │   ├── SignUpActivity.java     # 회원가입
│   │   │   │   ├── ProfileSetupActivity.java # 프로필 설정
│   │   │   │   ├── MainActivity.java       # 메인 화면
│   │   │   │   ├── MapActivity.java        # 지도 화면
│   │   │   │   ├── CreateActivityActivity.java # 활동 생성
│   │   │   │   ├── ActivityDetailActivity.java # 활동 상세
│   │   │   │   ├── ChatRoomActivity.java   # 채팅방
│   │   │   │   ├── ProfileActivity.java    # 프로필 보기
│   │   │   │   ├── EditProfileActivity.java # 프로필 수정
│   │   │   │   └── FriendsActivity.java    # 친구 관리
│   │   │   │
│   │   │   ├── Fragments/                  # Fragment 클래스들
│   │   │   │   ├── MapFragment.java        # 지도 프래그먼트
│   │   │   │   ├── ProfileFragment.java    # 프로필 프래그먼트
│   │   │   │   ├── ChatListFragment.java   # 채팅 목록
│   │   │   │   └── SettingsFragment.java   # 설정
│   │   │   │
│   │   │   ├── Adapters/                   # RecyclerView 어댑터
│   │   │   │   ├── ActivityAdapter.java    # 활동 목록
│   │   │   │   ├── ChatRoomAdapter.java    # 채팅방 목록
│   │   │   │   ├── ChatMessageAdapter.java # 채팅 메시지
│   │   │   │   ├── FriendRequestAdapter.java # 친구 요청
│   │   │   │   ├── UserAdapter.java        # 사용자 목록
│   │   │   │   ├── ParticipantAdapter.java # 참여자 목록
│   │   │   │   └── PlaceSearchAdapter.java # 장소 검색 결과
│   │   │   │
│   │   │   ├── models/                     # 데이터 모델
│   │   │   │   ├── Activity.java           # 활동 모델
│   │   │   │   ├── ChatMessage.java        # 채팅 메시지 모델
│   │   │   │   ├── ChatRoom.java           # 채팅방 모델
│   │   │   │   ├── Place.java              # 장소 모델
│   │   │   │   ├── NotificationItem.java   # 알림 모델
│   │   │   │   └── UserReview.java         # 리뷰 모델
│   │   │   │
│   │   │   ├── utils/                      # 유틸리티 클래스
│   │   │   │   ├── FirebaseActivityManager.java # Firebase 활동 관리
│   │   │   │   ├── FirebaseChatManager.java # Firebase 채팅 관리
│   │   │   │   ├── ThemeManager.java       # 테마(다크모드) 관리
│   │   │   │   └── CategoryMapper.java     # 카테고리 매핑
│   │   │   │
│   │   │   ├── User.java                   # 사용자 모델
│   │   │   └── ConnectMateApplication.java # Application 클래스
│   │   │
│   │   ├── res/
│   │   │   ├── layout/                     # XML 레이아웃 파일 (20+)
│   │   │   ├── drawable/                   # 이미지, 아이콘, 벡터
│   │   │   ├── color/                      # 라이트 테마 색상
│   │   │   ├── color-night/                # 다크 테마 색상
│   │   │   ├── values/                     # strings, colors, styles
│   │   │   └── mipmap-*/                   # 앱 아이콘 (다양한 해상도)
│   │   │
│   │   └── AndroidManifest.xml             # 앱 매니페스트
│   │
│   ├── build.gradle.kts                    # 앱 빌드 설정
│   ├── proguard-rules.pro                  # ProGuard 난독화 규칙
│   └── google-services.json                # Firebase 설정
│
├── connectmate-web/                        # React 웹 앱
│   ├── public/
│   ├── src/
│   │   ├── components/                     # React 컴포넌트
│   │   └── App.js
│   └── package.json
│
├── gradle/
│   ├── libs.versions.toml                  # Gradle 버전 카탈로그
│   └── wrapper/
│       └── gradle-wrapper.properties       # Gradle 8.13
│
├── build.gradle.kts                        # 루트 빌드 설정
├── settings.gradle.kts                     # 프로젝트 설정
├── firebase.json                           # Firebase Hosting 설정
├── database.rules.json                     # Firebase DB 보안 규칙
├── local.properties.example                # API 키 설정 예시
└── release-key.jks                         # 릴리스 서명 키 (별도 생성 필요)
```

---

## 설치 및 설정

### 사전 요구사항

- **Android Studio**: Arctic Fox 이상 (Hedgehog 2023.1.1+ 권장)
- **JDK**: 11 이상
- **Android SDK**: API 35
- **Git**: 버전 관리용

### 1. 프로젝트 클론

```bash
git clone https://github.com/your-username/ConnectMate.git
cd ConnectMate
```

### 2. local.properties 설정

`local.properties.example` 파일을 복사하여 `local.properties` 파일을 생성하고 API 키를 입력합니다:

```bash
cp local.properties.example local.properties
```

`local.properties` 파일을 열어 다음 항목들을 설정합니다:

```properties
# Android SDK 경로 (Android Studio가 자동 설정)
sdk.dir=/path/to/Android/sdk

# Kakao API Keys (https://developers.kakao.com)
KAKAO_APP_KEY=your_native_app_key_here
KAKAO_REST_API_KEY=your_rest_api_key_here

# Naver API Keys (https://developers.naver.com)
NAVER_CLIENT_ID=your_naver_client_id_here
NAVER_CLIENT_SECRET=your_naver_client_secret_here

# T Map API Key (https://openapi.sk.com) - 도보 경로 안내용
TMAP_APP_KEY=your_tmap_app_key_here

# 릴리스 빌드용 서명 키 설정
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

### 3. API 키 발급 방법

#### 카카오 API 키
1. [Kakao Developers](https://developers.kakao.com) 접속
2. 애플리케이션 추가
3. **앱 키** 탭에서 **네이티브 앱 키** 복사 → `KAKAO_APP_KEY`
4. **앱 키** 탭에서 **REST API 키** 복사 → `KAKAO_REST_API_KEY`
5. **플랫폼** 탭에서 Android 플랫폼 등록 (패키지명: `app.connectmate`)
6. **카카오 로그인** 활성화
7. **카카오맵** API 사용 설정

#### 네이버 API 키
1. [Naver Developers](https://developers.naver.com) 접속
2. 애플리케이션 등록
3. **네이버 로그인** API 사용 설정
4. **Client ID** → `NAVER_CLIENT_ID`
5. **Client Secret** → `NAVER_CLIENT_SECRET`

#### T Map API 키 (선택)
1. [SK Open API](https://openapi.sk.com) 접속
2. 회원가입 및 앱 생성
3. **appKey** 발급 → `TMAP_APP_KEY`

### 4. Firebase 설정

프로젝트에는 이미 `google-services.json` 파일이 포함되어 있습니다. 새로운 Firebase 프로젝트를 사용하려면:

1. [Firebase Console](https://console.firebase.google.com) 접속
2. 새 프로젝트 생성
3. Android 앱 추가 (패키지명: `app.connectmate`)
4. `google-services.json` 다운로드 후 `app/` 디렉토리에 배치
5. Firebase Authentication에서 필요한 로그인 제공업체 활성화
6. Realtime Database 생성 및 규칙 설정

### 5. 빌드 및 실행

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리스 빌드 (서명 키 필요)
./gradlew assembleRelease

# 앱 설치 및 실행
./gradlew installDebug
```

또는 Android Studio에서:
1. 프로젝트 열기
2. Gradle Sync 완료 대기
3. Run 버튼 클릭 (또는 Shift + F10)

---

## 앱 실행 시 고려사항

### 필수 확인 사항

#### 1. API 키 설정 확인
- `local.properties` 파일에 모든 API 키가 올바르게 입력되어 있는지 확인
- 빈 값이 있으면 해당 기능이 작동하지 않음
- 특히 **카카오 앱 키**가 없으면 지도와 카카오 로그인이 작동하지 않음

#### 2. 카카오 개발자 콘솔 설정
- **키 해시 등록 필수**: 카카오 로그인을 사용하려면 앱의 키 해시를 등록해야 함
  ```bash
  # 디버그 키 해시 확인 (Mac/Linux)
  keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64

  # 디버그 키 해시 확인 (Windows)
  keytool -exportcert -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64
  ```
- **패키지명 등록**: 카카오 개발자 콘솔에서 패키지명 `app.connectmate` 등록
- **카카오맵 API 활성화**: 지도 기능 사용을 위해 카카오맵 API 활성화 필요

#### 3. Firebase 설정 확인
- `google-services.json` 파일이 `app/` 디렉토리에 있는지 확인
- Firebase Console에서 다음 서비스 활성화 확인:
  - Authentication (Google, 이메일/비밀번호 로그인 제공업체)
  - Realtime Database
  - Storage
- Database 규칙이 올바르게 설정되어 있는지 확인

#### 4. 위치 권한
- 앱 첫 실행 시 위치 권한 요청됨
- 권한을 거부하면 지도 기반 기능이 제한됨
- 테스트 시 에뮬레이터에서 위치 설정 필요 (Extended controls → Location)

#### 5. 네트워크 연결
- 앱은 인터넷 연결이 필수
- Firebase와의 실시간 동기화를 위해 안정적인 네트워크 필요
- 오프라인 상태에서는 캐시된 데이터만 표시됨

### 디버그 모드 특이사항

- 디버그 빌드에서는 버전명 뒤에 `-debug` 접미사가 붙음
- ProGuard 난독화가 비활성화되어 있어 빌드 속도가 빠름
- 로그캣에서 상세한 디버그 로그 확인 가능

### 릴리스 빌드 주의사항

- `release-key.jks` 키스토어 파일이 필요
- `local.properties`에 서명 키 정보 입력 필요:
  ```properties
  KEYSTORE_PASSWORD=your_password
  KEY_ALIAS=your_alias
  KEY_PASSWORD=your_password
  ```
- ProGuard 난독화가 활성화됨
- 리소스 축소(shrinkResources)가 활성화됨

### 에뮬레이터 vs 실제 기기

| 기능 | 에뮬레이터 | 실제 기기 |
|------|------------|-----------|
| 카카오맵 | O (x86/x86_64 지원) | O |
| GPS 위치 | 수동 설정 필요 | 자동 |
| 카카오 로그인 | 제한적 (웹뷰 방식) | 완전 지원 |
| 네이버 로그인 | 제한적 | 완전 지원 |
| Google 로그인 | O | O |
| 카메라/갤러리 | 제한적 | 완전 지원 |

### 알려진 이슈

1. **카카오맵 소스 JAR 누락 경고**: Android Studio에서 카카오맵 SDK 소스를 찾을 수 없다는 경고가 뜰 수 있으나, 빌드에는 영향 없음
2. **에뮬레이터 위치**: 에뮬레이터에서는 수동으로 위치를 설정해야 함
3. **소셜 로그인**: 일부 에뮬레이터에서 소셜 로그인이 웹뷰로 대체될 수 있음

### 문제 해결

#### Gradle Sync 실패
```bash
# Gradle 캐시 정리
./gradlew clean
./gradlew --refresh-dependencies
```

#### 카카오맵이 표시되지 않음
1. `KAKAO_APP_KEY`가 올바른지 확인
2. 카카오 개발자 콘솔에서 키 해시 등록 확인
3. 카카오맵 API가 활성화되어 있는지 확인

#### Firebase 연결 오류
1. `google-services.json` 파일 위치 확인 (`app/` 디렉토리)
2. 패키지명이 Firebase Console 설정과 일치하는지 확인
3. Firebase 서비스들이 활성화되어 있는지 확인

#### 로그인 실패
1. 각 소셜 로그인 제공업체의 개발자 콘솔에서 앱 설정 확인
2. OAuth 리다이렉트 URI 설정 확인
3. 키 해시 또는 SHA-1 인증서 지문 등록 확인

---

## 권한 설명

앱에서 요청하는 권한과 그 용도입니다:

| 권한 | 용도 |
|------|------|
| `INTERNET` | Firebase 통신, API 호출, 지도 데이터 로드 |
| `ACCESS_NETWORK_STATE` | 네트워크 연결 상태 확인 |
| `ACCESS_FINE_LOCATION` | 정밀한 GPS 위치 (지도 기능) |
| `ACCESS_COARSE_LOCATION` | 대략적인 위치 (지도 기능) |
| `READ_MEDIA_IMAGES` (Android 13+) | 갤러리에서 프로필 사진 선택 |
| `READ_EXTERNAL_STORAGE` (Android 12 이하) | 갤러리에서 프로필 사진 선택 |

---

## 보안 및 개인정보

ConnectMate는 사용자의 개인정보 보호를 최우선으로 합니다:

- **최소 권한 원칙**: 필요한 권한만 요청
- **데이터 암호화**: 전송 중 데이터 암호화 (HTTPS/TLS)
- **광고 없음**: 광고 SDK 미사용
- **분석 도구 미사용**: 사용자 추적 없음
- **안전한 인증**: Firebase Authentication 사용
- **계정 삭제**: CCPA 준수 완전한 계정 삭제 기능 제공

### Firebase 보안 규칙
- 인증된 사용자만 데이터 접근 가능
- 사용자는 자신의 데이터만 수정 가능
- 활동 및 채팅 데이터는 참여자만 접근 가능

---

## 연락처

**개발자**: 유진선, 석지효, 이동열, 조성민

- Email: jinseony0622@gmail.com

버그 리포트나 기능 제안은 이메일로 연락해 주세요.

---

## 감사의 말

ConnectMate는 다음 오픈소스 프로젝트 및 서비스를 사용합니다:

- [Firebase](https://firebase.google.com/) - 백엔드 서비스
- [Kakao SDK](https://developers.kakao.com/) - 카카오 로그인 및 지도
- [Naver Login SDK](https://developers.naver.com/) - 네이버 로그인
- [Glide](https://github.com/bumptech/glide) - 이미지 로딩
- [OkHttp](https://square.github.io/okhttp/) - HTTP 클라이언트
- [Material Design Components](https://material.io/develop/android) - UI 컴포넌트
- [CircleImageView](https://github.com/hdodenhof/CircleImageView) - 원형 이미지 뷰

---

> **ConnectMate** - 혼자가 아닌 함께, 더 즐거운 일상을 만듭니다.
