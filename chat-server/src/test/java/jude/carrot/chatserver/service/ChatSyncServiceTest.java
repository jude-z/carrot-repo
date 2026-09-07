package jude.carrot.chatserver.service;

import jude.carrot.infra.repository.chat.ChatRepository;
import jude.carrot.infra.repository.chat.dto.ChatMessageBulk;
import jude.carrot.infra.repository.chat.dto.ChatRoomMessageBulk;
import jude.carrot.infra.repository.chat.dto.ReadStatusBulk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSyncServiceTest {

    @InjectMocks
    private ChatSyncService chatSyncService;

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private Cursor<String> mockCursor(List<String> keys) {
        Cursor<String> cursor = mock(Cursor.class);
        Iterator<String> iterator = keys.iterator();
        when(cursor.hasNext()).thenAnswer(invocation -> iterator.hasNext());
        lenient().when(cursor.next()).thenAnswer(invocation -> iterator.next());
        return cursor;
    }


    @Test
    @DisplayName("스캔된 chatMessage 키가 1000개 미만이면 한 번에 벌크 저장한다")
    void syncChatMessage_flushesRemainderWhenUnderBatchSize() {
        List<String> keys = List.of("chatMessage::1", "chatMessage::2", "chatMessage::3");
        Cursor<String> cursor = mockCursor(keys);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<Object> redisChatMessages = keys.stream()
                .<Object>map(key -> Map.of("id", key, "content", "content-" + key, "publishedBy", 1, "publishedAt", "2026-09-07T12:30:15"))
                .toList();
        when(valueOperations.multiGet(keys)).thenReturn(redisChatMessages);

        chatSyncService.syncChatMessage();

        ArgumentCaptor<List<ChatMessageBulk>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatRepository, times(1)).bulkChatMessage(captor.capture());
        List<ChatMessageBulk> bulks = captor.getValue();
        assertThat(bulks).hasSize(3);
        assertThat(bulks).extracting(ChatMessageBulk::id).containsExactly("1", "2", "3");
        assertThat(bulks).extracting(ChatMessageBulk::content)
                .containsExactly("content-chatMessage::1", "content-chatMessage::2", "content-chatMessage::3");
    }

    @Test
    @DisplayName("스캔된 chatMessage 키가 1000개를 넘으면 1000개 단위로 나누어 벌크 저장한다")
    void syncChatMessage_flushesInBatchesOf1000() {
        List<String> keys = IntStream.rangeClosed(1, 1001)
                .mapToObj(i -> "chatMessage::" + i)
                .toList();
        Cursor<String> cursor = mockCursor(keys);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(anyList())).thenAnswer(invocation -> {
            List<String> requestedKeys = invocation.getArgument(0);
            return requestedKeys.stream()
                    .<Object>map(key -> Map.of("id", key, "content", key, "publishedBy", 1, "publishedAt", "2026-09-07T12:30:15"))
                    .toList();
        });

        chatSyncService.syncChatMessage();

        ArgumentCaptor<List<ChatMessageBulk>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatRepository, times(2)).bulkChatMessage(captor.capture());
        List<List<ChatMessageBulk>> allBatches = captor.getAllValues();
        assertThat(allBatches.get(0)).hasSize(1000);
        assertThat(allBatches.get(1)).hasSize(1);
    }


    @Test
    @DisplayName("스캔된 chatRoom 키가 1000개 미만이면 한 번에 벌크 저장한다")
    void syncChatRoomMessage_flushesRemainderWhenUnderBatchSize() {
        List<String> keys = List.of("chatRoom::10", "chatRoom::20");
        Cursor<String> cursor = mockCursor(keys);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // 실제 Redis에서는 Object.class serializer 때문에 멤버가 Map으로 역직렬화된다
        when(zSetOperations.range("chatRoom::10", 0, -1))
                .thenReturn(new LinkedHashSet<>(List.of(Map.of("chatMessageId", "100"))));
        when(zSetOperations.range("chatRoom::20", 0, -1))
                .thenReturn(new LinkedHashSet<>(List.of(Map.of("chatMessageId", "200"), Map.of("chatMessageId", "201"))));

        chatSyncService.syncChatRoomMessage();

        ArgumentCaptor<List<ChatRoomMessageBulk>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatRepository, times(1)).bulkChatRoomMessage(captor.capture());
        List<ChatRoomMessageBulk> bulks = captor.getValue();
        assertThat(bulks).extracting(ChatRoomMessageBulk::chatRoomId).containsExactly(10L, 20L, 20L);
        assertThat(bulks).extracting(ChatRoomMessageBulk::chatMessageId).containsExactly("100", "200", "201");
    }

    @Test
    @DisplayName("스캔된 chatRoom 키가 1000개를 초과할 때만 중간에 벌크 저장한다 (1000개는 초과하지 않아 마지막에 한 번에 저장)")
    void syncChatRoomMessage_doesNotFlushMidLoopAtExactly1000() {
        List<String> keys = IntStream.rangeClosed(1, 1000)
                .mapToObj(i -> "chatRoom::" + i)
                .toList();
        Cursor<String> cursor = mockCursor(keys);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(anyString(), eq(0L), eq(-1L))).thenAnswer(invocation -> {
            String requestedKey = invocation.getArgument(0);
            return new LinkedHashSet<>(List.of(Map.of("chatMessageId", requestedKey.split("::")[1])));
        });

        chatSyncService.syncChatRoomMessage();

        verify(chatRepository, times(1)).bulkChatRoomMessage(anyList());
    }


    @Test
    @DisplayName("스캔된 읽음 상태 키가 1000개 미만이면 한 번에 벌크 저장한다")
    void syncReadStatus_flushesRemainderWhenUnderBatchSize() {
        List<String> keys = List.of("chatParticipantId::5::100", "chatParticipantId::6::200");
        Cursor<String> cursor = mockCursor(keys);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(List.of(Map.of("chatMessageId", "1"), Map.of("chatMessageId", "2")));

        chatSyncService.syncReadStatus();

        ArgumentCaptor<List<ReadStatusBulk>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatRepository, times(1)).bulkReadStatus(captor.capture());
        List<ReadStatusBulk> bulks = captor.getValue();
        assertThat(bulks).extracting(ReadStatusBulk::chatParticipantId).containsExactly(5L, 6L);
        assertThat(bulks).extracting(ReadStatusBulk::chatMessageId).containsExactly("1", "2");
    }

    @Test
    @DisplayName("스캔할 키가 없으면 벌크 저장을 호출하지 않는다")
    void syncReadStatus_doesNothingWhenNoKeysScanned() {
        Cursor<String> cursor = mockCursor(List.of());
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        chatSyncService.syncReadStatus();

        verify(chatRepository, never()).bulkReadStatus(anyList());
    }

    @Test
    @DisplayName("reconciliation: zset에 없는 chatMessage 키만 삭제한다")
    void reconciliation_deletesOnlyOrphanChatMessageKeys() {
        Cursor<String> zsetKeyCursor = mockCursor(List.of("chatRoom::10"));
        Cursor<String> stringKeyCursor = mockCursor(List.of("chatMessage::1", "chatMessage::2", "chatMessage::3"));
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(zsetKeyCursor, stringKeyCursor);

        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        Cursor<ZSetOperations.TypedTuple<Object>> memberCursor = mock(Cursor.class);
        Iterator<ZSetOperations.TypedTuple<Object>> members = List.<ZSetOperations.TypedTuple<Object>>of(
                new DefaultTypedTuple<>(Map.of("chatMessageId", "1"), 1d),
                new DefaultTypedTuple<>(Map.of("chatMessageId", "2"), 2d)
        ).iterator();
        when(memberCursor.hasNext()).thenAnswer(invocation -> members.hasNext());
        lenient().when(memberCursor.next()).thenAnswer(invocation -> members.next());
        when(zSetOperations.scan(eq("chatRoom::10"), any(ScanOptions.class))).thenReturn(memberCursor);

        chatSyncService.reconciliation();

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, times(1)).delete(captor.capture());
        assertThat(captor.getValue()).containsExactly("chatMessage::3");
    }

    @Test
    @DisplayName("reconciliation: 고아 키가 없으면 삭제하지 않는다")
    void reconciliation_doesNotDeleteWhenNoOrphans() {
        Cursor<String> zsetKeyCursor = mockCursor(List.of("chatRoom::10"));
        Cursor<String> stringKeyCursor = mockCursor(List.of("chatMessage::1"));
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(zsetKeyCursor, stringKeyCursor);

        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        Cursor<ZSetOperations.TypedTuple<Object>> memberCursor = mock(Cursor.class);
        Iterator<ZSetOperations.TypedTuple<Object>> members = List.<ZSetOperations.TypedTuple<Object>>of(
                new DefaultTypedTuple<>(Map.of("chatMessageId", "1"), 1d)
        ).iterator();
        when(memberCursor.hasNext()).thenAnswer(invocation -> members.hasNext());
        lenient().when(memberCursor.next()).thenAnswer(invocation -> members.next());
        when(zSetOperations.scan(eq("chatRoom::10"), any(ScanOptions.class))).thenReturn(memberCursor);

        chatSyncService.reconciliation();

        verify(redisTemplate, never()).delete(anyList());
    }
}
