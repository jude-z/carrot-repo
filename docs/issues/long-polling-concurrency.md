# 고정된 리소스로 최대한의 동시성을 얻기 위해서는 어떤 구조를 택해야 할까?

## 문제

## 고민한 선택지

## 해결

현재 구현 요약 (dev 브랜치 기준)
- `GET /api/v1/chatRoom/polling-fetch/{chatRoomId}`가 `DeferredResult`(타임아웃 5초)를 반환해 서블릿 스레드를 즉시 돌려준다.
- `Flux.interval`이 0.5초 간격으로 Redis ZSET을 `rangeByScore`로 조회하고, 새 메시지가 생기면 `multiGet`으로 본문을 받아 응답한다.
- 4.5초 동안 없으면 스트림이 끝나고 5초에 타임아웃(503), 클라이언트가 재요청한다.

## 결과 · 측정

## 관련 코드
- `chat-server/.../controller/ChatController.java` `pollingFetch`
- `chat-server/.../service/ChatService.java` `pollingFetch`
- 시퀀스: [docs/USE_CASE.md](../USE_CASE.md#4-메시지-수신-long-polling)
