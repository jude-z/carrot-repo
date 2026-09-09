package jude.carrot.chatserver.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveClientTracker {

    public static final String METRIC_NAME = "chat.clients.active";
    public static final String METRIC_ACTIVE_COHORT = "chat.clients.active.cohort";
    public static final String METRIC_SERVED = "chat.clients.served";
    public static final String METRIC_REQUESTS = "chat.requests.served";
    static final String COHORT_INSIDE = "inside";
    static final String COHORT_OUTSIDE = "outside";

    private final Clock clock;
    private final long windowMillis;
    private final long cohortThresholdUserId;
    private final Map<Transport, ConcurrentHashMap<Long, Long>> lastSeenByTransport = new EnumMap<>(Transport.class);
    private final Map<Transport, Set<Long>> servedByTransport = new EnumMap<>(Transport.class);
    private final Map<Transport, Counter[]> requestCounters = new EnumMap<>(Transport.class);

    @Autowired
    public ActiveClientTracker(MeterRegistry meterRegistry,
                               @Value("${chat.metrics.active-client-window:30s}") Duration window,
                               @Value("${chat.metrics.cohort-threshold-user-id:1003}") long cohortThresholdUserId) {
        this(meterRegistry, window, cohortThresholdUserId, Clock.systemUTC());
    }

    ActiveClientTracker(MeterRegistry meterRegistry, Duration window, long cohortThresholdUserId, Clock clock) {
        this.clock = clock;
        this.windowMillis = window.toMillis();
        this.cohortThresholdUserId = cohortThresholdUserId;
        for (Transport transport : Transport.values()) {
            ConcurrentHashMap<Long, Long> lastSeen = new ConcurrentHashMap<>();
            Set<Long> served = ConcurrentHashMap.newKeySet();
            lastSeenByTransport.put(transport, lastSeen);
            servedByTransport.put(transport, served);

            Gauge.builder(METRIC_NAME, lastSeen, this::countActive)
                    .tag("transport", transport.tag())
                    .description("distinct clients active within the window")
                    .register(meterRegistry);
            Counter[] counters = new Counter[2];
            requestCounters.put(transport, counters);
            for (boolean inside : new boolean[]{true, false}) {
                String cohort = inside ? COHORT_INSIDE : COHORT_OUTSIDE;
                counters[inside ? 0 : 1] = Counter.builder(METRIC_REQUESTS)
                        .tag("transport", transport.tag()).tag("cohort", cohort)
                        .description("served requests (publish / polling-fetch / websocket frame), split by userId cohort")
                        .register(meterRegistry);
                Gauge.builder(METRIC_ACTIVE_COHORT, lastSeen, map -> countActive(map, inside))
                        .tag("transport", transport.tag()).tag("cohort", cohort)
                        .description("distinct clients active within the window, split by userId cohort")
                        .register(meterRegistry);
                Gauge.builder(METRIC_SERVED, served, set -> countServed(set, inside))
                        .tag("transport", transport.tag()).tag("cohort", cohort)
                        .description("cumulative distinct clients served since startup, split by userId cohort")
                        .register(meterRegistry);
            }
        }
    }

    public void touch(Transport transport, Long userId) {
        if (userId == null) {
            return;
        }
        markSeen(transport, userId);
        requestCounters.get(transport)[isInside(userId) ? 0 : 1].increment();
    }

    public void connected(Transport transport, Long userId) {
        if (userId == null) {
            return;
        }
        markSeen(transport, userId);
    }

    private void markSeen(Transport transport, Long userId) {
        lastSeenByTransport.get(transport).put(userId, clock.millis());
        servedByTransport.get(transport).add(userId);
    }

    private boolean isInside(long userId) {
        return userId <= cohortThresholdUserId;
    }

    private int countActive(ConcurrentHashMap<Long, Long> lastSeen) {
        long threshold = clock.millis() - windowMillis;
        lastSeen.values().removeIf(seenAt -> seenAt < threshold);
        return lastSeen.size();
    }

    private long countActive(ConcurrentHashMap<Long, Long> lastSeen, boolean inside) {
        long threshold = clock.millis() - windowMillis;
        lastSeen.values().removeIf(seenAt -> seenAt < threshold);
        return lastSeen.keySet().stream().filter(id -> isInside(id) == inside).count();
    }

    private long countServed(Set<Long> served, boolean inside) {
        return served.stream().filter(id -> isInside(id) == inside).count();
    }
}
