package jude.carrot.chatserver.key.redis;

public class ChatKeyGenerator {
    private static final String DELIMITER = "::";
    private static String chatMessageFormat = "chatMessage::%s";
    private static String chatRoomMessageFormat = "chatRoom::%s";
    private static String readStatusFormat = "chatParticipantId::%s::%s";


    public static String generateChatMessageKey(String snowflakeId){
        return chatMessageFormat.formatted(snowflakeId);
    }

    public static String parseChatMessageId(String chatMessageKey){
        return chatMessageKey.split(DELIMITER)[1];
    }

    public static String generateChatRoomMessageKey(Long chatRoomId){
        String strChatRoomId = String.valueOf(chatRoomId);
        return chatRoomMessageFormat.formatted(strChatRoomId);
    }

    public static Long parseChatRoomId(String chatRoomMessageKey){
        return Long.valueOf(chatRoomMessageKey.split(DELIMITER)[1]);
    }

    public static String generateReadStatusKey(Long chatParticipantId, Long chatRoomId){
        String strChatParticipantId = String.valueOf(chatParticipantId);
        String strChatRoomId = String.valueOf(chatRoomId);
        return readStatusFormat.formatted(strChatParticipantId, strChatRoomId);
    }

    public static Long parseReadStatusChatParticipantId(String readStatusKey){
        return Long.valueOf(readStatusKey.split(DELIMITER)[1]);
    }

    public static String chatMessageKeyPattern(){
        return chatMessageFormat.formatted("*");
    }
    public static String chatRoomMessageKeyPattern(){
        return chatRoomMessageFormat.formatted("*");
    }
    public static String readStatusKeyPattern(){
        return readStatusFormat.formatted("*", "*");
    }
}
