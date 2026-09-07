package jude.carrot.chatserver.metrics;

public enum Transport {
    WEBSOCKET("websocket"),
    LONG_POLLING("longpolling");

    private final String tag;

    Transport(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
