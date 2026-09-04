# 분산 환경에서 PK가 중복되어 데이터 정합성이 깨지는 문제를 막으려면 어떤 키 전략을 써야 할까?

## 문제

## 고민한 선택지

## 해결

현재 구현 요약 (dev 브랜치 기준)
- `SnowFlakeKeyGenerator`가 timestamp + instance-id(`snowflake.instance-id`) + sequence(12bit)로 ID를 만든다.
- 시퀀스 증가는 `AtomicLong.compareAndSet`으로 처리해 락 없이 동시성을 보장한다.
- 같은 값을 Redis ZSET score로 써서 시간 순 범위 조회에 그대로 사용한다.

## 결과 · 측정

## 관련 코드
- `chat-server/.../key/snowflake/SnowFlakeKeyGenerator.java`
- `chat-server/src/main/resources/application.yml` `snowflake.instance-id`
