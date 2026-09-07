package jude.carrot.chatserver.websocket;

import jude.carrot.chatserver.metrics.ActiveClientTracker;
import jude.carrot.chatserver.metrics.Transport;
import jude.carrot.chatserver.service.ChatService;
import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.chat.dto.PublishChatRequest;
import jude.carrot.service.exception.CustomException;
import jude.carrot.service.status.Status;
import jude.carrot.web.response.ApiResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    static final String CHAT_ROOM_ID_ATTRIBUTE = "chatRoomId";
    static final String USER_ID_ATTRIBUTE = "userId";
    /** 접속 중인 WebSocket 세션 수 (long-polling 의 tomcat.threads.busy / http.server.requests 와 비교용) */
    static final String METRIC_ACTIVE_SESSIONS = "chat.websocket.sessions.active";
    /** 프레임 수신 -> Redis 저장 -> 브로드캐스트까지 걸린 시간 */
    static final String METRIC_PUBLISH = "chat.websocket.publish";
    private static final int SEND_TIME_LIMIT_MILLIS = 5_000;
    private static final int SEND_BUFFER_SIZE_LIMIT = 512 * 1024;

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ActiveClientTracker activeClientTracker;
    private final Timer publishTimer;
    private final AtomicInteger activeSessions = new AtomicInteger();

    /** chatRoomId -> (sessionId -> 동시 전송이 안전한 세션) */
    private final Map<Long, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper, MeterRegistry meterRegistry,
                                ActiveClientTracker activeClientTracker) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
        this.activeClientTracker = activeClientTracker;
        this.publishTimer = Timer.builder(METRIC_PUBLISH)
                .description("websocket publish: frame received -> redis saved -> broadcast done")
                .register(meterRegistry);
        Gauge.builder(METRIC_ACTIVE_SESSIONS, activeSessions, AtomicInteger::get)
                .description("active websocket sessions")
                .register(meterRegistry);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session.getPrincipal());
        Long chatRoomId = resolveChatRoomId(session.getUri());
        if (userId == null || chatRoomId == null) {
            log.warn("websocket rejected. principal={}, uri={}", session.getPrincipal(), session.getUri());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
        session.getAttributes().put(CHAT_ROOM_ID_ATTRIBUTE, chatRoomId);
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MILLIS, SEND_BUFFER_SIZE_LIMIT);
        roomSessions.computeIfAbsent(chatRoomId, key -> new ConcurrentHashMap<>()).put(session.getId(), safeSession);
        activeSessions.incrementAndGet();
        activeClientTracker.touch(Transport.WEBSOCKET, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTRIBUTE);
        Long chatRoomId = (Long) session.getAttributes().get(CHAT_ROOM_ID_ATTRIBUTE);
        if (userId == null || chatRoomId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        activeClientTracker.touch(Transport.WEBSOCKET, userId);
        Timer.Sample sample = Timer.start();
        try {
            PublishChatRequest publishChatRequest = objectMapper.readValue(message.getPayload(), PublishChatRequest.class);
            ChatMessageElement published = chatService.publishForWebSocket(chatRoomId, userId, publishChatRequest);
            broadcast(chatRoomId, objectMapper.writeValueAsString(published));
        } catch (JacksonException e) {
            sendTo(chatRoomId, session.getId(), ApiResponse.failFrom(Status.VALID_FAIL.getCode(), Status.VALID_FAIL.getDetailMessage()));
        } catch (CustomException e) {
            sendTo(chatRoomId, session.getId(), ApiResponse.failFrom(e.getCode(), e.getDetailMessage()));
        } finally {
            sample.stop(publishTimer);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long chatRoomId = (Long) session.getAttributes().get(CHAT_ROOM_ID_ATTRIBUTE);
        if (chatRoomId == null) {
            return;
        }
        roomSessions.computeIfPresent(chatRoomId, (key, sessions) -> {
            if (sessions.remove(session.getId()) != null) {
                activeSessions.decrementAndGet();
            }
            return sessions.isEmpty() ? null : sessions;
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("websocket transport error. sessionId={}", session.getId(), exception);
    }



    private void broadcast(Long chatRoomId, String payload) {
        Map<String, WebSocketSession> sessions = roomSessions.getOrDefault(chatRoomId, Map.of());
        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession target : sessions.values()) {
            send(target, textMessage);
        }
    }

    private void sendTo(Long chatRoomId, String sessionId, ApiResponse<Void> apiResponse) {
        WebSocketSession target = roomSessions.getOrDefault(chatRoomId, Map.of()).get(sessionId);
        if (target == null) {
            return;
        }
        send(target, new TextMessage(objectMapper.writeValueAsString(apiResponse)));
    }

    private void send(WebSocketSession target, TextMessage textMessage) {
        if (!target.isOpen()) {
            return;
        }
        try {
            target.sendMessage(textMessage);
        } catch (IOException e) {
            log.warn("websocket send fail. sessionId={}", target.getId(), e);
        }
    }

    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** /ws/chat/{chatRoomId} 의 마지막 경로 조각 */
    private Long resolveChatRoomId(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }
        String path = uri.getPath();
        String last = path.substring(path.lastIndexOf('/') + 1);
        try {
            return Long.valueOf(last);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
