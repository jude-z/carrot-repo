package jude.carrot.chatserver.scheduler;

import jude.carrot.chatserver.service.ChatSyncService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatScheduler {

    private final ChatSyncService chatSyncService;

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "syncChatMessage", lockAtLeastFor = "PT10S", lockAtMostFor = "PT10M")
    public void syncChatMessage() {
        chatSyncService.syncChatMessage();
    }

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "syncChatRoomMessage", lockAtLeastFor = "PT10S", lockAtMostFor = "PT10M")
    public void syncChatRoomMessage() {
        chatSyncService.syncChatRoomMessage();
    }

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "syncReadStatus", lockAtLeastFor = "PT10S", lockAtMostFor = "PT10M")
    public void syncReadStatus() {
        chatSyncService.syncReadStatus();
    }
}
