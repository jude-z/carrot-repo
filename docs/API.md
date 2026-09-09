# API 명세서

> `dev` 브랜치의 컨트롤러 · 서비스 · 요청 · 응답 클래스를 기준으로 작성했습니다.
> 회원 · 게시물 · 이미지는 `api-server`, 채팅은 `chat-server`가 처리합니다.

## 공통

### 응답 포맷
모든 응답은 아래 형태입니다. `data`가 없으면 필드가 생략됩니다.
```json
{
  "data": { },
  "code": "SC",
  "detailMessage": "success"
}
```

### 인증
- 로그인 · 회원가입 · 헬스체크를 제외한 모든 API는 로그인 후 발급되는 **세션 쿠키**로 인증합니다. *(auth)* 표시가 인증 필요 API입니다.
- 세션은 Redis에 저장되므로 `api-server`와 `chat-server` 어느 쪽으로 요청해도 같은 세션이 유지됩니다.

### 검증 실패 응답
`@Valid` 검증에 실패하면 `400`과 함께 필드별 메시지를 반환합니다.
```json
{
  "data": [
    { "field": "email", "message": "이메일 형식이 올바르지 않습니다." },
    { "field": "password", "message": "비밀번호는 8자 이상이어야 합니다." }
  ],
  "code": "VF",
  "detailMessage": "valid fail"
}
```

### 비즈니스 오류 응답
서비스에서 `CustomException`이 발생하면 `Status`의 코드 · 메시지 · HTTP 상태로 응답합니다.
```json
{
  "code": "PNE",
  "detailMessage": "post not exist"
}
```
코드 목록은 문서 끝의 [응답 코드](#응답-코드)를 참고합니다.

### 공통 오류 (모든 API에 해당)
아래는 애플리케이션이 직접 처리하지 않고 Spring Security · Spring MVC 기본 동작으로 응답하는 경우입니다. 본문은 공통 응답 포맷이 아니라 Spring Boot 기본 오류 JSON(`timestamp`, `status`, `error`, `path`)입니다.

| 상황 | HTTP | 비고 |
| --- | --- | --- |
| *(auth)* API를 세션 없이 호출 | 403 | 인증 진입점을 따로 두지 않아 401이 아닌 403으로 응답 |
| 요청 본문이 JSON이 아니거나 파싱 불가 | 400 | `HttpMessageNotReadableException` |
| 필수 쿼리 파라미터 누락 (`chatMessageId`, `chatMessageKey`) | 400 | `MissingServletRequestParameterException` |
| 경로 변수 · 쿼리 파라미터 타입 불일치 (예: `postId=abc`) | 400 | `MethodArgumentTypeMismatchException` |
| 지원하지 않는 HTTP 메서드 | 405 | |
| 아래 각 API에 적은 "처리되지 않는 오류" | 500 | 서비스에서 잡지 않는 런타임 예외 |

### 헬스체크
- `GET /health`
- Response `200` · 본문 `health` (문자열)

---

## 1. 인증 (Auth)

### 1.1 회원가입
- `POST /api/v1/auth/signup`
- Request
```json
{
  "email": "user@example.com",
  "password": "qwer1234!",
  "confirmPassword": "qwer1234!",
  "nickname": "jude",
  "profileImageUrl": "https://cdn.example.com/profile/uuid.webp"
}
```
- 검증: `email` 이메일 형식 · `password`, `confirmPassword` 8자 이상, 영문 · 숫자 · 특수문자 포함 · `nickname` 필수 · `profileImageUrl` 선택
- Response `200`
```json
{
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| `email` 비어 있음 또는 이메일 형식 아님 | `VF` | 400 |
| `password` 또는 `confirmPassword`가 비어 있음, 8자 미만, 영문 · 숫자 · 특수문자 중 하나라도 없음 | `VF` | 400 |
| `nickname` 비어 있음 | `VF` | 400 |
| `password`와 `confirmPassword`가 다름 | `PCM` | 400 |
| 같은 `email`로 이미 가입됨 | `UE` | 409 |

### 1.2 로그인
- `POST /api/v1/auth/login`
- Request
```json
{
  "email": "user@example.com",
  "password": "qwer1234!"
}
```
- Response `200` · 세션 쿠키 발급
```json
{
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| `email` 또는 `password`가 검증 규칙(회원가입과 동일)에 어긋남 | `VF` | 400 |
| 본문이 JSON이 아니거나 비어 있음 (빈 요청으로 간주되어 검증 실패) | `VF` | 400 |
| 가입되지 않은 `email` | `AF` | 401 |
| 비밀번호 불일치 | `AF` | 401 |
| 로그인 URL을 POST 이외 메서드로 호출 | `SE` | 500 |
| 그 외 예상하지 못한 인증 오류 | `SE` | 500 |

### 1.3 로그아웃
- `POST /api/v1/auth/logout` *(auth)*
- Request: 없음
- 동작: 세션을 만료시킵니다. Spring Security 기본 로그아웃 처리라 JSON 본문 없이 `302`로 `/login?logout`에 리다이렉트합니다.
- 실패: 세션이 없어도 같은 `302` 응답입니다 (실패 케이스 없음).

---

## 2. 회원 (User)

### 2.1 프로필 조회
- `GET /api/v1/users/{userId}` *(auth)*
- 본인 프로필도 같은 엔드포인트를 사용합니다. 본인이면 `isMine`이 `true`이고 `password`, `verified`가 포함됩니다.
- Response `200`
```json
{
  "data": {
    "id": 1001,
    "email": "user@example.com",
    "password": "$2a$10$...",
    "nickname": "jude",
    "profileImageUrl": "https://cdn.example.com/profile/uuid.webp",
    "address": { "sido": "서울특별시", "gu": "강남구", "dong": "역삼동" },
    "verified": true,
    "isMine": true
  },
  "code": "SC",
  "detailMessage": "success"
}
```
- 타인 조회 시 `password`, `verified`는 생략되고 `isMine`은 `false`입니다. 프로필 이미지가 없으면 `profileImageUrl`, 동네 인증 전이면 `address`가 `null`입니다.
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| `userId`에 해당하는 회원이 없음 | `UE` | 400 |

### 2.2 프로필 수정
- `PUT /api/v1/users/{userId}` *(auth)* · 본인만 가능
- Request
```json
{
  "password": "qwer1234!",
  "confirmPassword": "qwer1234!",
  "nickname": "jude",
  "profileImageUrl": "https://cdn.example.com/profile/new-uuid.webp"
}
```
- 검증: 회원가입과 동일. `profileImageUrl`은 선택이며 기존 URL과 다를 때만 이미지가 교체됩니다.
- Response `200`
```json
{
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패 (검사 순서대로)

| 조건 | code | HTTP |
| --- | --- | --- |
| `password`, `confirmPassword`, `nickname` 검증 실패 | `VF` | 400 |
| `userId`에 해당하는 회원이 없음 | `UE` | 400 |
| `userId`가 로그인한 본인이 아님 | `NF` | 401 |
| `password`와 `confirmPassword`가 다름 | `PCM` | 400 |

### 2.3 동네 인증
- `POST /api/v1/users/verify/address` *(auth)*
- 좌표를 Kakao Local API로 행정구역으로 변환해 회원 주소로 저장하고 `verified`를 `true`로 바꿉니다.
- Request
```json
{
  "latitude": "37.4979",
  "longitude": "127.0276"
}
```
- Response `200`
```json
{
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| `latitude` 또는 `longitude`가 비어 있음 | `VF` | 400 |
| 로그인한 회원이 DB에 없음 | `UE` | 400 |
| Kakao API가 200이 아닌 응답을 돌려줌 (3xx 등) | `ESE` | 500 |
| Kakao API가 4xx · 5xx를 돌려줌 (토큰 오류, 좌표 형식 오류, 장애) | 처리되지 않는 오류 | 500 |
| Kakao API 네트워크 오류 · 타임아웃 | 처리되지 않는 오류 | 500 |

- 참고: Kakao가 200을 주지만 결과가 없는 좌표(바다, 해외 등)는 오류가 아니라 `sido` · `gu` · `dong`이 모두 `null`인 주소로 저장되고 `verified`는 `true`가 됩니다.

---

## 3. 이미지 (Image)

### 3.1 업로드용 presigned URL 발급
- `POST /image/presigned-url` *(auth)*
- Request: 없음
- 10분 동안 유효한 S3 PUT URL을 발급합니다. 클라이언트가 이 URL로 파일을 직접 올리고, 업로드된 URL을 회원가입 · 프로필 수정 · 게시물 등록에 넣습니다.
- Response `200`
```json
{
  "data": {
    "url": "https://bucket.s3.ap-northeast-2.amazonaws.com/key?X-Amz-Algorithm=...&X-Amz-Expires=600&..."
  },
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| 서버에 AWS 자격 증명 또는 리전 설정이 없음 (`S3Presigner.create()` 실패) | 처리되지 않는 오류 | 500 |

---

## 4. 게시물 (Post)

### 4.1 게시물 목록 조회
- `GET /api/v1/post?pageNum=1&pageSize=20` *(auth)*
- `pageNum`은 1부터 시작합니다. 응답의 `pageNum`은 0부터 시작하는 내부 페이지 번호입니다.
- Response `200`
```json
{
  "data": {
    "page": [
      {
        "id": 1,
        "title": "맥북 팝니다",
        "price": 1200000,
        "address": { "sido": "서울특별시", "gu": "강남구", "dong": "역삼동" },
        "createdById": 1001,
        "createdByEmail": "user@example.com",
        "thumbnailImageUrl": "https://cdn.example.com/thumb/uuid.webp"
      }
    ],
    "pageSize": 20,
    "pageNum": 0,
    "totalPage": 7,
    "elementCount": 20,
    "isLast": false
  },
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| `pageNum`이 0 이하 또는 `pageSize`가 0 이하 (`PageRequest` 생성 실패) | 처리되지 않는 오류 | 500 |
| 범위를 벗어난 페이지 | 오류 아님. `page`가 빈 배열, `isLast`가 `true` | 200 |

### 4.2 게시물 상세 조회
- `GET /api/v1/post/{postId}` *(auth)*
- Response `200`
```json
{
  "data": {
    "id": 1,
    "title": "맥북 팝니다",
    "price": 1200000,
    "address": { "sido": "서울특별시", "gu": "강남구", "dong": "역삼동" },
    "createdById": 1001,
    "createdByEmail": "user@example.com",
    "thumbnailImageUrl": "https://cdn.example.com/thumb/uuid.webp",
    "contentImageUrls": [
      "https://cdn.example.com/post/uuid-1.webp",
      "https://cdn.example.com/post/uuid-2.webp"
    ]
  },
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| `postId`에 해당하는 게시물이 없음 | `PNE` | 400 |

### 4.3 게시물 등록
- `POST /api/v1/post` *(auth)*
- 요청 좌표를 Kakao Local API로 다시 조회하고, 회원의 인증된 동네와 구 또는 동이 일치할 때만 저장합니다.
- Request
```json
{
  "title": "맥북 팝니다",
  "content": "M3 프로",
  "price": 1200000,
  "latitude": "37.4979",
  "longitude": "127.0276",
  "thumbNailImageUrl": "https://cdn.example.com/thumb/uuid.webp",
  "contentImageUrls": [
    "https://cdn.example.com/post/uuid-1.webp",
    "https://cdn.example.com/post/uuid-2.webp"
  ]
}
```
- 검증: `title`, `content`, `latitude`, `longitude` 필수 · `price` 0보다 큰 정수 · `contentImageUrls` 1개 이상 · `thumbNailImageUrl` 선택
- Response `200`
```json
{
  "data": { "id": 1 },
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패 (검사 순서대로)

| 조건 | code | HTTP |
| --- | --- | --- |
| `title`, `content`, `latitude`, `longitude` 비어 있음 · `price` 없거나 0 이하 · `contentImageUrls` 비어 있음 | `VF` | 400 |
| 로그인한 회원이 DB에 없음 | `UE` | 400 |
| Kakao API가 200이 아닌 응답 (3xx 등) | `ESE` | 500 |
| Kakao API 4xx · 5xx, 네트워크 오류 | 처리되지 않는 오류 | 500 |
| 회원이 동네 인증(2.3)을 한 적이 없음 (인증 주소가 `null`이라 비교 중 NPE) | 처리되지 않는 오류 | 500 |
| 요청 좌표의 구 · 동이 인증된 동네의 구 · 동과 모두 다름 | `ANE` | 400 |

### 4.4 게시물 수정
- `PUT /api/v1/post/{postId}` *(auth)* · 작성자만 가능
- Request
```json
{
  "title": "맥북 팝니다",
  "content": "M3 프로",
  "price": 1100000,
  "thumbNailImageUrl": "https://cdn.example.com/thumb/uuid.webp",
  "contentImageUrls": [
    "https://cdn.example.com/post/uuid-1.webp"
  ]
}
```
- 검증: 등록과 동일하되 좌표는 받지 않습니다.
- Response `200`
```json
{
  "data": { "id": 1 },
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패 (검사 순서대로)

| 조건 | code | HTTP |
| --- | --- | --- |
| `title`, `content` 비어 있음 · `price` 없거나 0 이하 · `contentImageUrls` 비어 있음 | `VF` | 400 |
| 로그인한 회원이 DB에 없음 | `UE` | 400 |
| `postId`에 해당하는 게시물이 없음 | `PNE` | 400 |
| 로그인한 회원이 작성자가 아님 | `UPN` | 400 |

### 4.5 게시물 삭제
- `DELETE /api/v1/post/{postId}` *(auth)* · 작성자만 가능
- Response `200`
```json
{
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패 (검사 순서대로)

| 조건 | code | HTTP |
| --- | --- | --- |
| 로그인한 회원이 DB에 없음 | `UE` | 400 |
| `postId`에 해당하는 게시물이 없음 | `PNE` | 400 |
| 로그인한 회원이 작성자가 아님 | `UPN` | 400 |

---

## 5. 채팅 (Chat)

채팅 메시지 ID는 Snowflake 기반 문자열이며 시간 순으로 정렬됩니다.

### 5.1 채팅방 생성
- `POST /api/v1/chatRoom` *(auth)*
- Request
```json
{
  "opponentId": 1002,
  "title": "맥북 팝니다"
}
```
- Response `200`
```json
{
  "data": { "id": 77 },
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| 로그인한 회원이 DB에 없음 | `UE` | 400 |
| `opponentId`에 해당하는 회원이 없음 | `UE` | 400 |
| `opponentId` 누락 (`null`로 조회 시도) | 처리되지 않는 오류 | 500 |

- 참고: 본인을 `opponentId`로 지정하거나 같은 상대와 채팅방을 여러 번 만드는 것을 막지 않습니다. `title`은 검증하지 않습니다.

### 5.2 메시지 전송
- `POST /api/v1/chatRoom/publish/{chatRoomId}` *(auth)*
- 메시지를 Redis에 저장합니다. Redis 오류 시 최대 3회(기본 1초 간격) 재시도하고, 60초마다 스케줄러가 MySQL로 반영합니다.
- Request
```json
{
  "content": "안녕하세요"
}
```
- Response `200`
```json
{
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패 (검사 순서대로)

| 조건 | code | HTTP |
| --- | --- | --- |
| `chatRoomId`에 해당하는 채팅방이 없음 | `CRE` | 400 |
| 로그인한 회원이 그 채팅방의 참여자가 아님 | `CPE` | 400 |
| Redis 저장이 3회 모두 실패 (연결 실패, 타임아웃 등 예외 종류와 무관) | `CMF` | 500 |

- 참고: `content`는 검증하지 않아 `null`이나 빈 문자열도 저장됩니다.

### 5.3 메시지 수신 (Long Polling)
- `GET /api/v1/chatRoom/polling-fetch/{chatRoomId}?chatMessageId={마지막으로 받은 메시지 ID}` *(auth)*
- `chatMessageId` 이후에 도착한 메시지를 0.5초 간격으로 확인하다가 새 메시지가 생기면 즉시 응답합니다.
- 이 API는 공통 응답 포맷으로 감싸지 않고 아래 본문을 그대로 반환합니다.
- Response `200`
```json
{
  "elements": [
    {
      "id": "7345891234567890123",
      "content": "안녕하세요",
      "publishedBy": 15,
      "publishedAt": "2026-09-04T10:05:00"
    }
  ],
  "elementCount": 1
}
```
- `publishedBy`는 채팅 참여자 ID(`chat_participant.id`)입니다.
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| `chatMessageId` 쿼리 파라미터 누락 | Spring 기본 오류 | 400 |
| `chatRoomId`에 해당하는 채팅방이 없음 | `CRE` | 400 |
| 로그인한 회원이 그 채팅방의 참여자가 아님 | `CPE` | 400 |
| 4.5초 동안 새 메시지가 없음 → 5초 시점에 `DeferredResult` 타임아웃 | Spring 기본 오류 (`AsyncRequestTimeoutException`) | 503 |
| `chatMessageId`가 숫자가 아님 (ZSET 범위 조회 중 `NumberFormatException`) | 처리되지 않는 오류 | 500 |
| 폴링 중 Redis 연결 오류 | 처리되지 않는 오류 | 500 |

- 클라이언트는 `503`을 "새 메시지 없음"으로 보고 즉시 다시 요청합니다.

### 5.4 메시지 목록 조회
- `GET /api/v1/chatRoom/fetch/{chatRoomId}?pageNum=1&pageSize=20` *(auth)*
- MySQL에 반영된 메시지를 페이지 단위로 반환합니다. Redis에만 있는 최근 60초 이내 메시지는 포함되지 않을 수 있습니다.
- Response `200`
```json
{
  "data": {
    "page": [
      {
        "id": "7345891234567890123",
        "content": "안녕하세요",
        "publishedBy": 15,
        "publishedAt": "2026-09-04T10:05:00"
      }
    ],
    "pageSize": 20,
    "pageNum": 0,
    "totalPage": 3,
    "elementCount": 20,
    "isLast": false
  },
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패

| 조건 | code | HTTP |
| --- | --- | --- |
| 채팅방이 없거나 로그인한 회원이 참여자가 아님 (한 쿼리로 확인) | `CRE` | 400 |
| `pageNum`이 0 이하 또는 `pageSize`가 0 이하 | 처리되지 않는 오류 | 500 |

### 5.5 최근 메시지 조회
- `GET /api/v1/chatRoom/{chatRoomId}` *(auth)*
- 채팅방의 가장 최근 메시지 한 건과 보낸 사람 정보를 반환합니다.
- Response `200`
```json
{
  "data": {
    "id": "7345891234567890123",
    "content": "안녕하세요",
    "publishedById": 1002,
    "publishedByEmail": "opponent@example.com",
    "publishedByNickname": "carrot"
  },
  "code": "SC",
  "detailMessage": "success"
}
```
- `publishedById`는 회원 ID(`user.id`)입니다.
- 실패 (검사 순서대로)

| 조건 | code | HTTP |
| --- | --- | --- |
| 채팅방이 없거나 로그인한 회원이 참여자가 아님 (한 쿼리로 확인) | `CRE` | 400 |
| MySQL에 반영된 메시지가 아직 한 건도 없음 | `CNE` | 400 |

### 5.6 읽음 처리
- `POST /api/v1/chatRoom/read/{chatRoomId}?chatMessageKey={메시지 키}` *(auth)*
- 참여자가 마지막으로 읽은 메시지를 Redis에 기록하고, 60초마다 스케줄러가 MySQL `read_status`로 반영합니다.
- `chatMessageKey`는 `chatMessage::{메시지 ID}` 형식입니다.
- Response `200`
```json
{
  "code": "SC",
  "detailMessage": "success"
}
```
- 실패 (검사 순서대로)

| 조건 | code | HTTP |
| --- | --- | --- |
| `chatMessageKey` 쿼리 파라미터 누락 | Spring 기본 오류 | 400 |
| `chatRoomId`에 해당하는 채팅방이 없음 | `CRE` | 400 |
| 로그인한 회원이 그 채팅방의 참여자가 아님 | `CPE` | 400 |
| `chatMessageKey`가 `chatMessage::{ID}` 형식이 아님 | `CNE` | 400 |
| 키에 해당하는 메시지가 MySQL에 없음 (아직 동기화 전이거나 잘못된 ID) | `CNE` | 400 |

### 5.7 메시지 전송 (WebSocket)
- `ws://{host}/ws/chat/{chatRoomId}` *(auth)*
- HTTP 방식(5.2)과 비교하기 위해 같은 발행 로직을 순수 WebSocket으로도 제공합니다. 두 방식 모두 같은 저장 로직을 거쳐 Redis에 동일하게 저장됩니다.
- 접속: 로그인 세션 쿠키가 필요하며, 미인증이면 SecurityFilterChain에서 핸드셰이크가 거부됩니다.
- 발행: 텍스트 프레임으로 JSON을 전송합니다.
```json
{
  "content": "안녕하세요"
}
```
- 수신: 발행 성공 시 같은 `chatRoomId`에 접속한 모든 세션(발행자 포함)에 아래 JSON이 전달됩니다.
```json
{
  "id": "123456789",
  "content": "안녕하세요",
  "publishedBy": 7,
  "publishedAt": "2026-09-07T12:30:15"
}
```
- 실패: 발행자 세션에만 `ApiResponse` 실패 포맷이 전달됩니다.

| 조건 | code | 동작 |
| --- | --- | --- |
| Principal이 없거나 `chatRoomId`가 숫자가 아님 | - | 접속 직후 `1008 POLICY_VIOLATION`으로 종료 |
| 프레임이 JSON으로 파싱되지 않음 | `VF` | 발행자 세션에 실패 JSON 전송 |
| `chatRoomId`에 해당하는 채팅방이 없음 | `CRE` | 발행자 세션에 실패 JSON 전송 |
| 로그인한 회원이 그 채팅방의 참여자가 아님 | `CPE` | 발행자 세션에 실패 JSON 전송 |
| Redis 저장이 3회 모두 실패 | `CMF` | 발행자 세션에 실패 JSON 전송 |

```json
{
  "code": "CPE",
  "detailMessage": "chat participant does not exist"
}
```

---

## 응답 코드

| code | HTTP | detailMessage | 의미 |
| --- | --- | --- | --- |
| `SC` | 200 | success | 성공 |
| `VF` | 400 | valid fail | 요청 검증 실패. `data`에 필드별 메시지 |
| `UE` | 409 | user already exists | 이미 가입된 이메일 |
| `UE` | 400 | user already exists | 존재하지 않는 회원 (코드 · 메시지가 위와 같음) |
| `PCM` | 400 | password and confirm password do not match | 비밀번호 확인 불일치 |
| `AF` | 401 | authentication fail | 로그인 실패 |
| `SE` | 500 | security error | 예상하지 못한 인증 오류 |
| `NF` | 401 | cannot update other user | 타인 프로필 수정 |
| `ESE` | 500 | server error | Kakao Local API가 200이 아닌 응답 |
| `NVA` | 400 | not valid address | 좌표 누락 (현재는 `@Valid`가 먼저 걸러 `VF`로 응답) |
| `ANE` | 400 | address not enrolled | 게시물 위치가 인증 동네와 불일치 |
| `PNE` | 400 | post not exist | 존재하지 않는 게시물 |
| `UPN` | 400 | this user is not creater | 게시물 작성자가 아님 |
| `CRE` | 400 | chat room does not exist | 존재하지 않는 채팅방 또는 참여자가 아님 |
| `CPE` | 400 | chat participant does not exist | 채팅방 참여자가 아님 |
| `CNE` | 400 | chat message not exit | 존재하지 않는 메시지 |
| `CMF` | 500 | push chat message fail | 메시지 Redis 저장 재시도 초과 |

정의만 있고 현재 사용되지 않는 코드: `PNC` (password not correct), `CRA` (not a participant of this chat room)
