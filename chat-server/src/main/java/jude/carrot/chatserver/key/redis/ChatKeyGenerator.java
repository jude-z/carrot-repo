package jude.carrot.chatserver.key.redis;

public class ChatKeyGenerator {
    private static final String chatMessageFormat = "chatMessage::%s";
    private static final String chatRoomMessageFormat = "chatRoom::%s";
    private static final String readStatusFormat = "chatParticipantId::%s::%s";
    private static final String token = "::";


    public static String generateChatMessageKey(String snowflakeId){
        return chatMessageFormat.formatted(snowflakeId);
    }

    public static String parseChatMessageId(String chatMessageKey){
        String[] split = chatMessageKey.split(token);
        if(split.length != 2) return null;
        if(!split[0].equals("chatMessage")) return null;
        return split[1];
    }

    public static String generateChatRoomMessageKey(Long chatRoomId){
        String strChatRoomId = String.valueOf(chatRoomId);
        return chatRoomMessageFormat.formatted(strChatRoomId);
    }

    public static Long parseChatRoomId(String chatRoomMessageKey){
        return Long.valueOf(chatRoomMessageKey.split(token)[1]);
    }

    public static String generateReadStatusKey(Long chatParticipantId, Long chatRoomId){
        String strChatParticipantId = String.valueOf(chatParticipantId);
        String strChatRoomId = String.valueOf(chatRoomId);
        return readStatusFormat.formatted(strChatParticipantId, strChatRoomId);
    }

    public static Long parseReadStatusChatParticipantId(String readStatusKey){
        return Long.valueOf(readStatusKey.split(token)[1]);
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
