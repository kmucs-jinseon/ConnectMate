# ConnectMate

> 함께 식사하고, 활동하고, 소통하는 소셜 매칭 플랫폼

ConnectMate는 혼자 식사하거나 활동하기 싫은 사람들을 위한 소셜 매칭 앱입니다. 근처에서 같이 밥 먹을 사람, 카페 갈 사람, 운동할 사람을 쉽게 찾고 연결될 수 있습니다.

## 📱 주요 기능

### 🗺️ 지도 기반 활동 탐색
- **카카오맵 통합**: 실시간으로 주변 활동을 지도에서 확인
- **위치 기반 매칭**: 내 주변에서 진행되는 활동 자동 탐색
- **POI 정보**: 장소 클릭 시 상세 정보 (주소, 전화번호, 거리) 표시

### 🎯 활동 생성 및 참여
- **간편한 활동 만들기**: 지도에서 장소 선택 후 바로 활동 생성
- **다양한 카테고리**: 식사, 카페, 운동, 스터디, 취미 등
- **실시간 업데이트**: Firebase를 통한 실시간 활동 현황 반영
- **참여자 관리**: 활동 참여 신청 및 수락 기능

### 💬 실시간 채팅
- **그룹 채팅**: 활동 참여자들과 실시간 대화
- **채팅방 알림**: 새 메시지 및 활동 업데이트 알림
- **미디어 공유**: 사진 및 이미지 공유 기능

### 👥 소셜 기능
- **친구 관리**: 친구 추가 및 관리
- **프로필 커스터마이징**: 프로필 사진, 닉네임, 소개 설정
- **활동 히스토리**: 내가 참여한 활동 기록 확인

### 🔐 다양한 로그인 옵션
- Google 계정으로 로그인
- 카카오 계정으로 로그인
- 네이버 계정으로 로그인
- 이메일/비밀번호 로그인

## 🎯 타겟 사용자

ConnectMate는 다음과 같은 분들을 위해 만들어졌습니다:

- 🏫 **대학생**: 캠퍼스에서 같이 밥 먹을 친구를 찾는 학생들
- 🏢 **직장인**: 점심 또는 저녁 식사 메이트를 찾는 직장인
- 🆕 **새로운 지역 거주자**: 새로운 동네에서 친구를 만들고 싶은 분들
- 🤝 **취미 공유자**: 같은 취미를 가진 사람들과 만나고 싶은 분들
- 🍽️ **맛집 탐방가**: 혼자 가기 아까운 맛집을 함께 갈 사람을 찾는 분들

## 🛠️ 기술 스택

### Android
- **언어**: Java, Kotlin
- **최소 SDK**: 24 (Android 7.0)
- **타겟 SDK**: 35 (Android 15)
- **Architecture**: MVVM 패턴

### Backend & Services
- **Firebase Authentication**: 사용자 인증 및 관리
- **Firebase Realtime Database**: 실시간 데이터 동기화
- **Firebase Firestore**: 구조화된 데이터 저장
- **Firebase Storage**: 이미지 및 미디어 파일 저장

### SDK 및 라이브러리
- **카카오 SDK**
  - Kakao Login SDK (v2.22.0)
  - Kakao Maps SDK (v2.12.18)
  - Kakao Local API (장소 검색)
- **네이버 로그인 SDK** (v5.9.1)
- **Google Play Services**
  - Google Sign-In
  - Location Services
- **Glide**: 이미지 로딩 및 캐싱
- **OkHttp**: 네트워크 통신
- **Gson**: JSON 파싱

### UI/UX
- **Material Design 3**: 최신 머티리얼 디자인 가이드라인 적용
- **AndroidX 라이브러리**: 최신 Android 컴포넌트 사용
- **CircleImageView**: 프로필 이미지 표시
- **RecyclerView**: 효율적인 리스트 표시

## 🚀 시작하기

### 필수 요구사항

- Android Studio (최신 버전 권장)
- JDK 11 이상
- Android SDK 24 이상
- 실제 Android 기기 (에뮬레이터에서는 지도 기능이 제한될 수 있음)

### API 키 설정

프로젝트를 빌드하기 전에 필요한 API 키들을 설정해야 합니다.

1. **`local.properties` 파일 생성**

   프로젝트 루트 디렉토리에 `local.properties` 파일을 생성하고 다음 내용을 추가합니다:

   ```properties
   # Android SDK 경로 (Android Studio가 자동으로 추가)
   sdk.dir=/path/to/Android/sdk

   # Kakao API Keys
   # https://developers.kakao.com 에서 발급
   KAKAO_APP_KEY=your_kakao_native_app_key
   KAKAO_REST_API_KEY=your_kakao_rest_api_key

   # Naver API Keys
   # https://developers.naver.com 에서 발급
   NAVER_CLIENT_ID=your_naver_client_id
   NAVER_CLIENT_SECRET=your_naver_client_secret

   # T Map API Key (선택사항)
   # https://openapi.sk.com 에서 발급
   TMAP_APP_KEY=your_tmap_app_key

   # Release Signing (배포용)
   KEYSTORE_PASSWORD=your_keystore_password
   KEY_ALIAS=your_key_alias
   KEY_PASSWORD=your_key_password
   ```

2. **카카오 개발자 콘솔 설정**

   https://developers.kakao.com/console/app 에서:

   - 앱 생성 및 Native App Key 발급
   - **플랫폼 > Android 설정**:
     - 패키지명: `app.connectmate`
     - 키 해시 등록 (디버그 및 릴리즈)
   - **제품 설정 > 카카오 로그인** 활성화

3. **Firebase 프로젝트 설정**

   - Firebase 콘솔에서 프로젝트 생성
   - Android 앱 추가 (패키지명: `app.connectmate`)
   - `google-services.json` 다운로드 후 `app/` 디렉토리에 배치
   - Authentication, Realtime Database, Firestore, Storage 활성화

4. **네이버 개발자 센터 설정**

   https://developers.naver.com 에서:

   - 애플리케이션 등록
   - 네이버 로그인 API 사용 설정
   - Client ID 및 Client Secret 발급

### 키 해시 생성

**디버그 키 해시:**
```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore \
-storepass android -keypass android | openssl sha1 -binary | openssl base64
```

**릴리즈 키 해시:**
```bash
keytool -exportcert -alias your_alias -keystore /path/to/your/keystore.jks \
-storepass your_password -keypass your_password | openssl sha1 -binary | openssl base64
```

생성된 키 해시를 카카오 개발자 콘솔에 등록해야 합니다.

### 프로젝트 빌드

1. **저장소 클론**
   ```bash
   git clone https://github.com/yourusername/ConnectMate.git
   cd ConnectMate
   ```

2. **Android Studio에서 프로젝트 열기**
   - `Open an existing project` 선택
   - 프로젝트 디렉토리 선택

3. **Gradle 동기화**
   - Android Studio가 자동으로 Gradle을 동기화합니다
   - 오류가 발생하면 `File > Sync Project with Gradle Files`

4. **빌드 및 실행**
   - **Debug 빌드**: `Run > Run 'app'` 또는 `./gradlew assembleDebug`
   - **Release 빌드**: `Build > Generate Signed Bundle / APK` 또는 `./gradlew assembleRelease`

## 📂 프로젝트 구조

```
ConnectMate/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/connectmate/
│   │   │   │   ├── ConnectMateApplication.java    # 앱 초기화
│   │   │   │   ├── LoginActivity.java             # 로그인 화면
│   │   │   │   ├── MainActivity.java              # 메인 화면
│   │   │   │   ├── MapActivity.java               # 지도 화면
│   │   │   │   ├── MapFragment.java               # 지도 프래그먼트
│   │   │   │   ├── CreateActivityActivity.java    # 활동 생성
│   │   │   │   ├── ActivityDetailActivity.java    # 활동 상세
│   │   │   │   ├── ChatRoomActivity.java          # 채팅방
│   │   │   │   ├── ProfileSetupActivity.java      # 프로필 설정
│   │   │   │   ├── EditProfileActivity.java       # 프로필 편집
│   │   │   │   ├── FriendsActivity.java           # 친구 관리
│   │   │   │   ├── models/                        # 데이터 모델
│   │   │   │   └── utils/                         # 유틸리티 클래스
│   │   │   ├── res/                               # 리소스 파일
│   │   │   └── AndroidManifest.xml
│   │   └── ...
│   ├── build.gradle.kts                            # 앱 수준 빌드 설정
│   └── proguard-rules.pro                          # ProGuard 규칙
├── build.gradle.kts                                # 프로젝트 수준 빌드 설정
├── local.properties.example                        # API 키 설정 예제
├── privacy-policy.html                             # 개인정보 처리방침
└── README.md                                       # 이 파일
```

## 🔒 보안 및 개인정보

ConnectMate는 사용자의 개인정보 보호를 최우선으로 합니다:

- 🔐 **최소 권한 원칙**: 필요한 권한만 요청
- 🛡️ **데이터 암호화**: 전송 중 데이터 암호화 (HTTPS/TLS)
- 🚫 **광고 없음**: 광고 SDK 미사용
- 📊 **분석 도구 미사용**: 사용자 추적 없음
- 🔒 **안전한 인증**: Firebase Authentication 사용

자세한 내용은 [개인정보 처리방침](privacy-policy.html)을 참조하세요.

## 🧪 테스트

### 단위 테스트 실행
```bash
./gradlew test
```

### 계측 테스트 실행
```bash
./gradlew connectedAndroidTest
```

## 🐛 알려진 이슈

- ⚠️ **에뮬레이터 제한**: 카카오맵은 에뮬레이터에서 정상 작동하지 않을 수 있습니다. 실제 기기에서 테스트하세요.
- ⚠️ **키 해시 등록**: 릴리즈 빌드 사용 시 릴리즈 키 해시를 카카오 콘솔에 반드시 등록해야 합니다.
- ⚠️ **GPS 권한**: 위치 기반 기능 사용 시 위치 권한이 필요합니다.

## 🤝 기여하기

ConnectMate 프로젝트에 기여를 환영합니다!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 버전 관리

- **v1.0.5** (현재 버전)
  - 버그 수정 및 UI 업데이트
  - 안정성 개선

- **v1.0.4**
  - Google Play 배포용 버그 수정

- **v1.0.0**
  - 최초 릴리즈
  - 기본 기능 구현

## 📄 라이선스

이 프로젝트는 개인 프로젝트입니다. 상업적 사용 전 개발자에게 문의하세요.

## 📧 연락처

**개발자**: Jinseon Yoo

- 📧 Email: jinseony0622@gmail.com
- 🌐 Website: [https://jenna-studio.dev](https://jenna-studio.dev)
- 📱 App Support: support@connectmate.app

## 🙏 감사의 말

ConnectMate는 다음 오픈소스 프로젝트를 사용합니다:

- [Firebase](https://firebase.google.com/)
- [Kakao SDK](https://developers.kakao.com/)
- [Naver Login SDK](https://developers.naver.com/)
- [Glide](https://github.com/bumptech/glide)
- [OkHttp](https://square.github.io/okhttp/)
- [Material Design Components](https://material.io/develop/android)

---

**Made with ❤️ by Jinseon Yoo**

> ConnectMate - 혼자가 아닌 함께, 더 즐거운 일상을 만듭니다.
