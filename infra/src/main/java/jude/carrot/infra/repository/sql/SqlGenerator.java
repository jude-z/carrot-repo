package jude.carrot.infra.repository.sql;

public class SqlGenerator {
    private SqlGenerator(){}

    public static String bulkReadStatueSql(){
        return """
                insert into read_status(chat_participant_id, chat_message_id, create_time, update_time)
                values(?, ?, ?, ?)
                """;
    }
    public static String bulkChatRoomMessageSql(){
        return """
                update chat_message set chat_room_id = ? where id = ?
                """;
    }
    public static String bulkChatMessageSql(){
        return """
                insert chat_message(id, content, published_by_id, published_at)
                values(?, ?, ?, ?)
                """;
    }
}
