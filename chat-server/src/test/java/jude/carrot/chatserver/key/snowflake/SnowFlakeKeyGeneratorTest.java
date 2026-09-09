package jude.carrot.chatserver.key.snowflake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

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
    @DisplayName("같은 timestamp + instanceId 로 100개 스레드가 동시에 호출하면 sequence 가 0부터 1씩 빠짐없이 증가한다")
    void generateSnowFlakeKey_sequenceIncrementsByOneUnderConcurrentAccess() throws Exception {


        LocalDateTime now = LocalDateTime.now();
        int threadCount = 100;
        int callsPerThread = 10;
        int total = threadCount * callsPerThread;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<List<String>>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                ready.countDown();
                start.await();
                List<String> mine = new ArrayList<>(callsPerThread);
                for (int j = 0; j < callsPerThread; j++) {
                    mine.add(generator.generateSnowFlakeKey(now));
                }
                return mine;
            }));
        }
        ready.await();
        start.countDown();

        List<Long> keys = new ArrayList<>(total);
        for (Future<List<String>> future : futures) {
            for (String key : future.get(5, TimeUnit.SECONDS)) {
                keys.add(Long.parseLong(key));
            }
        }
        executorService.shutdown();

        long sequenceMask = (1L << 12) - 1;
        long instanceMask = (1L << 10) - 1;
        Set<Long> timestamps = keys.stream().map(key -> key >> 22).collect(Collectors.toSet());
        Set<Long> instanceIds = keys.stream().map(key -> (key >> 12) & instanceMask).collect(Collectors.toSet());
        List<Long> sequences = keys.stream().map(key -> key & sequenceMask).sorted().toList();

        assertThat(timestamps).hasSize(1);
        assertThat(instanceIds).containsExactly(1L);
        assertThat(sequences)
                .containsExactlyElementsOf(LongStream.range(0, total).boxed().toList());
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
