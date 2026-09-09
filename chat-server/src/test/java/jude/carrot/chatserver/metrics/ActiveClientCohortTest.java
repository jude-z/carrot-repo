package jude.carrot.chatserver.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveClientCohortTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private double gauge(String name, String transport, String cohort) {
        return registry.get(name).tag("transport", transport).tag("cohort", cohort).gauge().value();
    }

    @Test
    @DisplayName("userId 가 threshold 이하면 inside, 초과면 outside 코호트로 활성/누적 사용자를 나눠 센다")
    void splitsByCohort() {
        ActiveClientTracker tracker = new ActiveClientTracker(registry, Duration.ofSeconds(30), 1003L, Clock.systemUTC());

        tracker.touch(Transport.WEBSOCKET, 5L);
        tracker.touch(Transport.WEBSOCKET, 1003L);
        tracker.touch(Transport.WEBSOCKET, 1005L);
        tracker.touch(Transport.LONG_POLLING, 2003L);

        assertThat(gauge(ActiveClientTracker.METRIC_ACTIVE_COHORT, "websocket", "inside")).isEqualTo(2);
        assertThat(gauge(ActiveClientTracker.METRIC_ACTIVE_COHORT, "websocket", "outside")).isEqualTo(1);
        assertThat(gauge(ActiveClientTracker.METRIC_SERVED, "websocket", "inside")).isEqualTo(2);
        assertThat(gauge(ActiveClientTracker.METRIC_SERVED, "websocket", "outside")).isEqualTo(1);
        assertThat(gauge(ActiveClientTracker.METRIC_SERVED, "longpolling", "outside")).isEqualTo(1);
        assertThat(gauge(ActiveClientTracker.METRIC_SERVED, "longpolling", "inside")).isEqualTo(0);
    }

    @Test
    @DisplayName("served 는 window 가 지나도 줄지 않는 누적값이고, active.cohort 는 window 밖이면 빠진다")
    void servedIsCumulativeActiveIsWindowed() {
        Instant start = Instant.parse("2026-09-08T00:00:00Z");
        MutableClock clock = new MutableClock(start);
        ActiveClientTracker tracker = new ActiveClientTracker(registry, Duration.ofSeconds(30), 1003L, clock);

        tracker.touch(Transport.LONG_POLLING, 1005L);
        clock.advance(Duration.ofSeconds(31));

        assertThat(gauge(ActiveClientTracker.METRIC_ACTIVE_COHORT, "longpolling", "outside")).isEqualTo(0);
        assertThat(gauge(ActiveClientTracker.METRIC_SERVED, "longpolling", "outside")).isEqualTo(1);

        tracker.touch(Transport.LONG_POLLING, 1005L);
        assertThat(gauge(ActiveClientTracker.METRIC_SERVED, "longpolling", "outside")).isEqualTo(1);
    }

    @Test
    @DisplayName("요청 카운터는 touch 마다 코호트별로 증가하고, connected 는 사용자만 기록하고 요청은 세지 않는다")
    void countsRequestsByCohort() {
        ActiveClientTracker tracker = new ActiveClientTracker(registry, Duration.ofSeconds(30), 1003L, Clock.systemUTC());

        tracker.connected(Transport.WEBSOCKET, 1005L);
        tracker.touch(Transport.WEBSOCKET, 1005L);
        tracker.touch(Transport.WEBSOCKET, 1005L);
        tracker.touch(Transport.LONG_POLLING, 5L);

        double wsOutside = registry.get(ActiveClientTracker.METRIC_REQUESTS).tag("transport", "websocket").tag("cohort", "outside").counter().count();
        double wsInside = registry.get(ActiveClientTracker.METRIC_REQUESTS).tag("transport", "websocket").tag("cohort", "inside").counter().count();
        double lpInside = registry.get(ActiveClientTracker.METRIC_REQUESTS).tag("transport", "longpolling").tag("cohort", "inside").counter().count();
        assertThat(wsOutside).isEqualTo(2);
        assertThat(wsInside).isEqualTo(0);
        assertThat(lpInside).isEqualTo(1);
        assertThat(gauge(ActiveClientTracker.METRIC_SERVED, "websocket", "outside")).isEqualTo(1);
    }

    static class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
