package jude.carrot.chatserver.key.redis;

public class ChatKeyGenerator {
    private static String chatMessageFormat = "chatMessage::%s";
    private static String chatRoomMessageFormat = "chatRoom::%s";
    private static String readStatusFormat = "chatParticipantId::%s::%s";


    public static String generateChatMessageKey(String snowflakeId){
        return chatMessageFormat.formatted(snowflakeId);
    }

    public static String parseChatMessageId(String chatMessageKey){
        return chatMessageKey.split("::")[1];
    }

    public static String generateChatRoomMessageKey(Long chatRoomId){
        String strChatRoomId = String.valueOf(chatRoomId);
        return chatRoomMessageFormat.formatted(strChatRoomId);
    }

    public static String generateReadStatusKey(Long userId, Long chatRoomId){
        String strUserId = String.valueOf(userId);
        String strChatRoomId = String.valueOf(chatRoomId);
        return readStatusFormat.formatted(strUserId, strChatRoomId);
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
