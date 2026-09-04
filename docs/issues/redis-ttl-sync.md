# Redis 데이터를 DB로 동기화하기 전에 삭제될 위험이 있다면, TTL 전략을 어떻게 가져가야 할까?

## 문제

## 고민한 선택지

## 해결

현재 구현 요약 (dev 브랜치 기준)
- 채팅 메시지 · 채팅방 ZSET · 읽음 상태 키는 TTL 없이 저장한다.
- `ChatScheduler`가 60초마다 `ChatSyncService`를 실행하고, ShedLock으로 한 인스턴스만 돌게 한다.
- `SCAN`으로 키를 순회해 1,000개 단위로 `MGET` 후 `JdbcTemplate.batchUpdate`로 벌크 반영한다.
- 반영이 끝난 키를 삭제하는 로직은 추가 예정이다.

## 결과 · 측정

## 관련 코드
- `chat-server/.../scheduler/ChatScheduler.java`
- `chat-server/.../service/ChatSyncService.java`
- `chat-server/.../config/ShedLockConfig.java`
- 시퀀스: [docs/USE_CASE.md](../USE_CASE.md#5-redis--mysql-동기화)
