# Redis에서 두 개 이상의 명령을 원자적으로 수행하려 할 때, 데이터 정합성을 어떻게 지킬까?

## 문제

## 고민한 선택지

## 해결

현재 구현 요약 (dev 브랜치 기준)
- 메시지 전송 시 `SET chatMessage::{id}`와 `ZADD chatRoom::{roomId}` 두 명령을 순서대로 실행한다.
- `ChatRetryService.saveRedis`에 `@Retryable`을 붙여 실패 시 최대 3회 재시도하고, `@Recover`에서 `CustomException(PUBLISH_CHAT_MESSAGE_FAIL)`을 던진다.
- 두 명령을 하나로 묶는 `MULTI/EXEC` 또는 Lua 스크립트는 아직 없다.

## 결과 · 측정

## 관련 코드
- `chat-server/.../service/ChatRetryService.java`
- `chat-server/.../service/ChatService.java` `publish`
