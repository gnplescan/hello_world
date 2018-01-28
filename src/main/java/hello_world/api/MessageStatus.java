package hello_world.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageStatus {
    private long id;

    private String content;

    public MessageStatus() {
        // Jackson deserialization
    }

    public MessageStatus(long id, String content) {
        this.id = id;
        this.content = content;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public String getContent() {
        return content;
    }
}