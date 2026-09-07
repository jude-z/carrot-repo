package jude.carrot.chatserver.key.snowflake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SnowFlakeKeyGeneratorTest {

    private final SnowFlakeKeyGenerator generator = new SnowFlakeKeyGenerator();
    private final SnowFlakeKeyGenerator anotherGenerator = new SnowFlakeKeyGenerator();
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(generator, "instanceId", 1L);
        ReflectionTestUtils.setField(anotherGenerator, "instanceId", 2L);
    }

    @Test
    @DisplayName("같은 밀리초에 여러 번 호출해도 서로 다른 키를 생성한다")
    void generateSnowFlakeKey_isUniqueWithinSameMillisecond() {
        LocalDateTime now = LocalDateTime.now();

        String key1 = generator.generateSnowFlakeKey(now);
        String key2 = generator.generateSnowFlakeKey(now);
        String key3 = generator.generateSnowFlakeKey(now);

        assertThat(Set.of(key1, key2, key3)).hasSize(3);
    }

    @Test
    @DisplayName("시간이 지나면 이전보다 더 큰(정렬 가능한) 키를 생성한다")
    void generateSnowFlakeKey_increasesOverTime() {
        LocalDateTime earlier = LocalDateTime.now();
        LocalDateTime later = earlier.plusSeconds(1);

        long earlierKey = Long.parseLong(generator.generateSnowFlakeKey(earlier));
        long laterKey = Long.parseLong(generator.generateSnowFlakeKey(later));

        assertThat(laterKey).isGreaterThan(earlierKey);
    }

    @Test
    @DisplayName("동시에 여러 스레드가 호출해도 모든 키가 유일하다")
    void generateSnowFlakeKey_isUniqueUnderConcurrentAccess() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(16);

        List<CompletableFuture<String>> completableFutures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> generator.generateSnowFlakeKey(now), executorService))
                .toList();

        Set<String> keys = completableFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toSet());

        assertThat(keys.size()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("instanceId가 다르면 같은 시각이어도 서로 다른 키를 생성한다")
    void generateSnowFlakeKey_differsByInstanceId() {
        LocalDateTime now = LocalDateTime.now();

        String key = generator.generateSnowFlakeKey(now);
        String otherKey = anotherGenerator.generateSnowFlakeKey(now);

        assertThat(key).isNotEqualTo(otherKey);
    }
}
