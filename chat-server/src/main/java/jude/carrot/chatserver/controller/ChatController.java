package jude.carrot.chatserver.controller;

import jude.carrot.chatserver.response.ChatMessageResponse;
import jude.carrot.chatserver.response.CreateChatRoomResponse;
import jude.carrot.chatserver.response.FetchRecentChatMessage;
import jude.carrot.chatserver.response.PollingChatMessagesResponse;
import jude.carrot.chatserver.service.ChatService;
import jude.carrot.infra.repository.chat.dto.CreateChatRoomRequest;
import jude.carrot.infra.repository.chat.dto.PublishChatRequest;
import jude.carrot.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import static jude.carrot.service.status.Status.SUCCESS;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatRoom")
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateChatRoomResponse>> createChatRoom(@AuthenticationPrincipal Long userId,
                                                            @RequestBody CreateChatRoomRequest createChatRoomRequest){
        CreateChatRoomResponse chatRoomResponse = chatService.createChatRoom(userId, createChatRoomRequest);
        return successResponseEntity(chatRoomResponse);
    }

    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ApiResponse<FetchRecentChatMessage>> fetchRecentChatMessage(@AuthenticationPrincipal Long userId,
                                                                                      @PathVariable Long chatRoomId){
        FetchRecentChatMessage fetchRecentChatMessage = chatService.fetchRecentChatMessage(userId, chatRoomId);
        return successResponseEntity(fetchRecentChatMessage);
    }

    @PostMapping("/publish/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable Long chatRoomId, @AuthenticationPrincipal Long userId,
                                                        @RequestBody PublishChatRequest publishChatRequest){
        chatService.publish(chatRoomId, userId, publishChatRequest);
        return successResponseEntity();
    }
    @GetMapping("/polling-fetch/{chatRoomId}")
    public DeferredResult<PollingChatMessagesResponse> pollingFetch(@PathVariable Long chatRoomId, @AuthenticationPrincipal Long userId,
                                                                    @RequestParam("chatMessageId") String lastChatMessageId){
        DeferredResult<PollingChatMessagesResponse> deferredResult = new DeferredResult<>(5000L);
        chatService.pollingFetch(chatRoomId, userId,lastChatMessageId)
                .subscribe(
                        deferredResult::setResult,
                        deferredResult::setErrorResult
                );
        return deferredResult;
    }

    @GetMapping("/fetch/{chatRoomId}")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> fetch(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                          @RequestParam(defaultValue = "20") Integer pageSize,
                                                                          @PathVariable Long chatRoomId,
                                                                          @AuthenticationPrincipal Long userId){
        ChatMessageResponse chatMessageResponse = chatService.fetch(pageNum, pageSize, chatRoomId, userId);
        return successResponseEntity(chatMessageResponse);
    }

    @PostMapping("/read/{chatRoomId}")
    public ResponseEntity<ApiResponse<Void>> read(@PathVariable Long chatRoomId, @AuthenticationPrincipal Long userId,
                                               @RequestParam String chatMessageKey){
        chatService.read(chatRoomId, userId, chatMessageKey);
        return successResponseEntity();
    }
    private <T> ResponseEntity<ApiResponse<T>> successResponseEntity(T data){
        ApiResponse<T> apiResponse = ApiResponse.successFrom(data);
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }

    private ResponseEntity<ApiResponse<Void>> successResponseEntity(){
        ApiResponse<Void> apiResponse = ApiResponse.successFrom();
        HttpStatus httpStatus = SUCCESS.getHttpStatus();
        return ResponseEntity
                .status(httpStatus.value())
                .body(apiResponse);
    }


}
