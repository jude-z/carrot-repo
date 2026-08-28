package jude.carrot.chatserver.controller;

import jude.carrot.chatserver.response.ChatMessageResponse;
import jude.carrot.chatserver.response.CreateChatRoomResponse;
import jude.carrot.chatserver.response.FetchRecentChatMessage;
import jude.carrot.chatserver.response.PollingChatMessagesResponse;
import jude.carrot.chatserver.service.ChatService;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.advice.CommonControllerAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.core.publisher.Mono;

import java.util.List;

import static jude.carrot.service.status.Status.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Bean;

@WebMvcTest(controllers = ChatController.class)
class ChatControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CHAT_ROOM_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @BeforeEach
    void setUpPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("채팅방을 생성하면 200과 함께 chatRoomId를 반환한다")
    void createChatRoom_success() throws Exception {
        when(chatService.createChatRoom(eq(USER_ID), any())).thenReturn(CreateChatRoomResponse.from(100L));

        mockMvc.perform(post("/api/v1/chatRoom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"opponentId":2,"title":"우리 동네"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.id").value(100));

        verify(chatService).createChatRoom(eq(USER_ID), any());
    }

    @Test
    @DisplayName("상대방이 존재하지 않으면 400과 함께 USER_NOT_EXIST 코드를 반환한다")
    void createChatRoom_fail_whenOpponentNotExist() throws Exception {
        when(chatService.createChatRoom(eq(USER_ID), any())).thenThrow(new CustomException(USER_NOT_EXIST));

        mockMvc.perform(post("/api/v1/chatRoom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"opponentId":2,"title":"우리 동네"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(USER_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("최근 채팅 메시지를 조회하면 200과 함께 데이터를 반환한다")
    void fetchRecentChatMessage_success() throws Exception {
        FetchRecentChatMessage dto = FetchRecentChatMessage.builder()
                .id("1").content("hi").publishedById(USER_ID).publishedByEmail("carrot@carrot.com").publishedByNickname("carrot")
                .build();
        when(chatService.fetchRecentChatMessage(USER_ID, CHAT_ROOM_ID)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/chatRoom/{chatRoomId}", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.content").value("hi"));
    }

    @Test
    @DisplayName("속하지 않은 채팅방을 조회하면 400과 함께 CHAT_ROOM_NOT_EXIST 코드를 반환한다")
    void fetchRecentChatMessage_fail_whenChatRoomNotExist() throws Exception {
        when(chatService.fetchRecentChatMessage(USER_ID, CHAT_ROOM_ID)).thenThrow(new CustomException(CHAT_ROOM_NOT_EXIST));

        mockMvc.perform(get("/api/v1/chatRoom/{chatRoomId}", CHAT_ROOM_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CHAT_ROOM_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("메시지를 발행하면 200을 반환하고 서비스를 호출한다")
    void publish_success() throws Exception {
        mockMvc.perform(post("/api/v1/chatRoom/publish/{chatRoomId}", CHAT_ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"));

        verify(chatService).publish(eq(CHAT_ROOM_ID), eq(USER_ID), any());
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 발행 시 400과 함께 CHAT_PARTICIPANT_NOT_EXIST 코드를 반환한다")
    void publish_fail_whenChatParticipantNotExist() throws Exception {
        doThrow(new CustomException(CHAT_PARTICIPANT_NOT_EXIST)).when(chatService).publish(eq(CHAT_ROOM_ID), eq(USER_ID), any());

        mockMvc.perform(post("/api/v1/chatRoom/publish/{chatRoomId}", CHAT_ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CHAT_PARTICIPANT_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("메시지 목록을 페이지 단위로 조회하면 200과 함께 데이터를 반환한다")
    void fetch_success() throws Exception {
        ChatMessageResponse dto = ChatMessageResponse.builder()
                .page(List.of()).pageNum(0).pageSize(20).totalPage(0).elementCount(0).isLast(true).build();
        when(chatService.fetch(1, 20, CHAT_ROOM_ID, USER_ID)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/chatRoom/fetch/{chatRoomId}", CHAT_ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    @DisplayName("읽음 처리를 요청하면 200을 반환하고 서비스를 호출한다")
    void read_success() throws Exception {
        mockMvc.perform(post("/api/v1/chatRoom/read/{chatRoomId}", CHAT_ROOM_ID)
                        .param("chatMessageKey", "chatMessage::1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"));

        verify(chatService).read(CHAT_ROOM_ID, USER_ID, "chatMessage::1");
    }

    @Test
    @DisplayName("존재하지 않는 메시지를 읽음 처리하면 400과 함께 CHAT_MESSAGE_NOT_EXIST 코드를 반환한다")
    void read_fail_whenChatMessageNotExist() throws Exception {
        doThrow(new CustomException(CHAT_MESSAGE_NOT_EXIST)).when(chatService).read(CHAT_ROOM_ID, USER_ID, "chatMessage::1");

        mockMvc.perform(post("/api/v1/chatRoom/read/{chatRoomId}", CHAT_ROOM_ID)
                        .param("chatMessageKey", "chatMessage::1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CHAT_MESSAGE_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("폴링 요청은 비동기로 처리되며 완료되면 새 메시지 목록을 반환한다")
    void pollingFetch_success() throws Exception {
        PollingChatMessagesResponse dto = PollingChatMessagesResponse.builder()
                .elements(List.of()).elementCount(0).build();
        when(chatService.pollingFetch(CHAT_ROOM_ID, USER_ID, "1000")).thenReturn(Mono.just(dto));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/chatRoom/polling-fetch/{chatRoomId}", CHAT_ROOM_ID)
                        .param("chatMessageId", "1000"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementCount").value(0));
    }

    @TestConfiguration
    static class ChatControllerTestConfiguration{
        @Bean
        ArgumentResolverConfig argumentResolverConfig(){
            return new ArgumentResolverConfig();
        }
        @Bean
        CommonControllerAdvice commonControllerAdvice(){
            return new CommonControllerAdvice();
        }
    }

    static class ArgumentResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }


}
