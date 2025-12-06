# ConnectMate

> 새로운 친구를 만나고, 함께 활동하고, 일상을 더 즐겁게 만들어보세요

혼자 하기 아쉬운 활동, 새로운 취미를 함께할 친구를 찾고 계신가요? Connect Mate는 같은 관심사를 가진 사람들을 쉽게 만나고 다양한 활동을 함께 즐길 수 있는 소셜 플랫폼입니다.

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

- 🏫 **대학생**: 캠퍼스에서 같이 식사하거나 공부할, 또는 같이 놀 친구를 찾는 학생들
- 🏢 **직장인**: 점심/저녁 식사 메이트, 다양한 활동을 공유할 사람을 찾는 직장인
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


## 📄 라이선스

이 프로젝트는 팀 프로젝트입니다. 상업적 사용 전 개발자에게 문의하세요.

## 📧 연락처

**개발자**: 유진선, 석지효, 이동열, 조성민

- 📧 Email: jinseony0622@gmail.com

  
## 🙏 감사의 말

ConnectMate는 다음 오픈소스 프로젝트를 사용합니다:

- [Firebase](https://firebase.google.com/)
- [Kakao SDK](https://developers.kakao.com/)
- [Naver Login SDK](https://developers.naver.com/)
- [Glide](https://github.com/bumptech/glide)
- [OkHttp](https://square.github.io/okhttp/)
- [Material Design Components](https://material.io/develop/android)

---

> ConnectMate - 혼자가 아닌 함께, 더 즐거운 일상을 만듭니다.
