package jude.carrot.chatserver.key.snowflake;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
@Component
public class SnowFlakeKeyGenerator {
    @Value("${snowflake.instance-id}")
    private Long instanceId;
    private AtomicLong status = new AtomicLong(0L);
    private static final long MACHINE_ID_BITS = 10;
    private static final long SEQUENCE_BITS = 12;
    private static final long EPOCH_MILLI_TIME = Instant.EPOCH.toEpochMilli();

    public String generateSnowFlakeKey(LocalDateTime now){
        long timestamp = (now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - EPOCH_MILLI_TIME) << SEQUENCE_BITS;
        long sequenceMask = (1L << SEQUENCE_BITS) - 1;
        long currentStatus;
        long sequence;
        while(true){
            long lastStatus = status.get();
            sequence = timestamp == (lastStatus & ~sequenceMask) ? (lastStatus & sequenceMask) + 1 : 0L;
            currentStatus = timestamp | sequence;
            if(status.compareAndSet(lastStatus, currentStatus)) break;
        }
        long result = (timestamp << MACHINE_ID_BITS) | (instanceId << SEQUENCE_BITS) | sequence;
        return String.valueOf(result);
    }
}
