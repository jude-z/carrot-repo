# API 명세서

> 모든 응답은 `{ "code": "SUCCESS", "data": {...} }` 공통 포맷.
> 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더 포함.

---

## 1. 인증 (Auth)

### 1.1 회원가입
- `POST /api/v1/auth/signup`
- Request
```json
{
  "email": "user@example.com",
  "password": "qwer1234!",
  "nickname": "jude"
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": null
}
```

### 1.2 로그인
- `POST /api/v1/auth/login`
- Request
```json
{
  "email": "user@example.com",
  "password": "qwer1234!"
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "userId": 1001,
    "accessToken": "eyJ...",
    "refreshToken": "dGhp...",
    "expire": 3600
  }
}
```

### 1.3 로그아웃
- `POST /api/v1/auth/logout` *(auth)*
- Request: 없음
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": null
}
```

### 1.4 내 위치 기반 주소 등록(인증)
- `POST /api/v1/verify/address` *(auth)*
- Request
```json
{
  "latitude": 37.4979,
  "longitude": 127.0276
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "sido": "서울특별시",
    "sigungu": "강남구",
    "dong": "역삼동",
    "verified": true
  }
}
```

---

## 2. S3 Presigned URL

### 2.1 업로드용 presigned URL 발급
- `POST /api/v1/url` *(auth)*
- Request
```json
{
  "fileName": "my_room_shot.jpg",
  "fileSize": 5242880
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "imageuuid": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
    "uploadUrl": "https://s3.../...?X-Amz-...",
    "expiresAt": "2026-05-28T05:46:12Z"
  }
}
```

---

## 3. 게시물 (Post)

### 3.1 내 주소 기반 거래 게시물 조회
- `GET /api/v1/posts?page=0&size=20` *(auth)*
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "content": [
      {
        "postId": 1,
        "title": "맥북 팝니다",
        "price": 1200000,
        "thumbnailUrl": "https://cdn.jude.com/uuid.webp",
        "sido": "서울특별시",
        "sigungu": "강남구",
        "dong": "역삼동",
        "createdAt": "2026-05-27T10:00:00Z",
        "poster": "최성욱"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 134
  }
}
```

### 3.2 게시물 상세 조회
- `GET /api/v1/posts/{postId}` *(auth)*
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "content": [
      {
        "mediaId": 501,
        "type": "IMAGE",
        "thumbnailUrl": "https://cdn.jude.com/thumbnails/t-f81d4f...webp",
        "createdAt": "2026-05-28T15:30:00Z",
        "videoDurationSec": null 
      },
      {
        "mediaId": 202,
        "type": "VIDEO",
        "thumbnailUrl": "https://cdn.jude.com/thumbnails/t-9d1f3c...webp",
        "createdAt": "2026-05-28T14:15:00Z",
        "videoDurationSec": 15 
      },
      {
        "mediaId": 500,
        "type": "IMAGE",
        "thumbnailUrl": "https://cdn.jude.com/thumbnails/t-4b82a6...webp",
        "createdAt": "2026-05-27T09:00:00Z",
        "videoDurationSec": null
      }
    ],
    "page": 0,
    "size": 30,
    "totalElements": 145
  }
}
```

### 3.3 게시물 등록
- `POST /api/v1/posts` *(auth)*
- Request
```json
{
  "title": "맥북 팝니다",
  "content": "M3 프로",
  "price": 1200000,
  "imageuuids": [
    "f81d4fae-7dec-11d0-a765-00a0c91e6bf6.webp",
    "9d1f3c7e-8a62-4d5a-bb0f-2f4fcb6a71e8.webp",
    "4b82a6d1-3e0c-47c9-9f2b-8d7a14f65c23.webp"
  ]
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "postId": 1
  }
}
```

### 3.4 게시물 수정
- `PUT /api/v1/posts/{postId}` *(auth)*
- Request
```json
{
  "title": "맥북 팝니다",
  "content": "M3 프로",
  "price": 1200000,
  "imageuuids": [
    "f81d4fae-7dec-11d0-a765-00a0c91e6bf6.webp",
    "9d1f3c7e-8a62-4d5a-bb0f-2f4fcb6a71e8.webp",
    "4b82a6d1-3e0c-47c9-9f2b-8d7a14f65c23.webp"
  ]
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "postId": 1
  }
}
```

### 3.5 게시물 삭제
- `DELETE /api/v1/posts/{postId}` *(auth)*
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": null
}
```

---

## 4. 채팅 (Chat)

### 4.1 채팅방 생성
- `POST /api/v1/chat-rooms` *(auth)*
- Request
```json
{
  "postId": 1,
  "opponentUserId": 1002
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "chatRoomId": 77
  }
}
```

### 4.2 채팅방 메시지 전송 (SSE 트리거)
- `POST /api/v1/chat-rooms/{chatRoomId}/messages` *(auth)*
- 클라이언트는 별도로 `GET /api/v1/chat-rooms/{chatRoomId}/stream` (SSE) 구독.
- Request
```json
{
  "content": "안녕하세요"
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "sentAt": "2026-05-27T10:05:00Z"
  }
}
```

### 4.3 채팅방 메시지 SSE 구독
- `GET /api/v1/chat-rooms/{chatRoomId}/stream` *(auth)*
- `Content-Type: text/event-stream`

- Response `200`
```json
{
  "code": "SUCCESS",
  "data": null
}
```
### 4.4 채팅방 이미지 등록 (resize, webp)
- `POST /api/v1/chat-rooms/{chatRoomId}/images` *(auth)*
- 서버가 원본을 다운로드 → resize → webp 변환 후 저장.
- Request
```json
{
  "imageuuids": [
    "f81d4fae-7dec-11d0-a765-00a0c91e6bf6.webp",
    "9d1f3c7e-8a62-4d5a-bb0f-2f4fcb6a71e8.webp"
  ]
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": null
}
```

### 4.5 채팅방 동영상 등록()
- `POST /api/v1/chat-rooms/{chatRoomId}/videos` *(auth)*
- Request

```json
{
  "videos": [
    {
      "videoUrl": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6.m3u8",
      "thumbnailUrl": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6.webp"
    },
    {
      "videoUrl": "9d1f3c7e-8a62-4d5a-bb0f-2f4fcb6a71e8.m3u8",
      "thumbnailUrl": "9d1f3c7e-8a62-4d5a-bb0f-2f4fcb6a71e8.webp"
    },
    {
      "videoUrl": "4b82a6d1-3e0c-47c9-9f2b-8d7a14f65c23.m3u8",
      "thumbnailUrl": "4b82a6d1-3e0c-47c9-9f2b-8d7a14f65c23.webp"
    }
  ]
}
```
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": null
}
```

### 4.6 채팅방 미디어 목록 조회
- `GET /api/v1/chat-rooms/{chatRoomId}/media?page=0&size=20` *(auth)*
- 이미지/동영상을 분리하여 각각 페이징해서 반환.
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "images": {
      "data": [
        {
          "mediaId": 301,
          "type": "IMAGE",
          "thumbnailUrl": "https://cdn.jude.com/f81d4fae-7dec-11d0-a765-00a0c91e6bf6.webp",
          "createdAt": "2026-05-27T10:05:10Z"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 42
    },
    "videos": {
      "data": [
        {
          "mediaId": 202,
          "type": "VIDEO",
          "thumbnailUrl": "https://cdn.jude.com/9d1f3c7e-8a62-4d5a-bb0f-2f4fcb6a71e8.webp",
          "createdAt": "2026-05-27T10:06:00Z"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 42
    }
  }
}
```

### 4.7 사진 상세 조회
- `GET /api/v1/chat-rooms/{chatRoomId}/images/{imageId}` *(auth)*
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "imageId": 301,
    "originalUrl": "https://cdn.jude.com/f81d4fae-7dec-11d0-a765-00a0c91e6bf6.webp",
    "thumbnailUrl": "https://cdn.jude.com/9d1f3c7e-8a62-4d5a-bb0f-2f4fcb6a71e8.webp",
    "width": 1080,
    "height": 1440,
    "createdAt": "2026-05-27T10:05:10Z"
  }
}
```

### 4.8 동영상 상세 조회
- `GET /api/v1/chat-rooms/{chatRoomId}/videos/{videoId}` *(auth)*
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "videoId": 202,
    "videoUrl": "https://cdn.jude.com/f81d4fae-7dec-11d0-a765-00a0c91e6bf6.m3u8",
    "thumbnailUrl": "https://cdn.jude.com/f81d4fae-7dec-11d0-a765-00a0c91e6bf6.webp",
    "durationSec": 12,
    "sizeBytes": 5242880,
    "createdAt": "2026-05-27T10:06:00Z"
  }
}
```

---

## 5. 유저 (User)

### 5.1 유저 프로필 조회
- `GET /api/v1/users/{userId}` *(auth)*
- 본인 프로필 조회 시에도 동일 엔드포인트 사용 (`userId` = 본인 id).
- Response `200`
```json
{
  "code": "SUCCESS",
  "data": {
    "userId": 1001,
    "nickname": "jude",
    "profileImageUrl": "https://cdn.jude.com/profile/1001.webp",
    "sido": "서울특별시",
    "sigungu": "강남구",
    "dong": "역삼동",
    "verified": true,
    "isMine": true
  }
}
```