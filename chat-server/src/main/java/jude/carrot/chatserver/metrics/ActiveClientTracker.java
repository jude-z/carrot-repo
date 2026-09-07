package jude.carrot.chatserver.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveClientTracker {

    public static final String METRIC_NAME = "chat.clients.active";

    private final Clock clock;
    private final long windowMillis;
    private final Map<Transport, ConcurrentHashMap<Long, Long>> lastSeenByTransport = new EnumMap<>(Transport.class);

    @Autowired
    public ActiveClientTracker(MeterRegistry meterRegistry,
                               @Value("${chat.metrics.active-client-window:30s}") Duration window) {
        this(meterRegistry, window, Clock.systemUTC());
    }

    ActiveClientTracker(MeterRegistry meterRegistry, Duration window, Clock clock) {
        this.clock = clock;
        this.windowMillis = window.toMillis();
        for (Transport transport : Transport.values()) {
            ConcurrentHashMap<Long, Long> lastSeen = new ConcurrentHashMap<>();
            lastSeenByTransport.put(transport, lastSeen);
            Gauge.builder(METRIC_NAME, lastSeen, this::countActive)
                    .tag("transport", transport.tag())
                    .description("distinct clients active within the window")
                    .register(meterRegistry);
        }
    }

    public void touch(Transport transport, Long userId) {
        if (userId == null) {
            return;
        }
        lastSeenByTransport.get(transport).put(userId, clock.millis());
    }

    private int countActive(ConcurrentHashMap<Long, Long> lastSeen) {
        long threshold = clock.millis() - windowMillis;
        lastSeen.values().removeIf(seenAt -> seenAt < threshold);
        return lastSeen.size();
    }
}
