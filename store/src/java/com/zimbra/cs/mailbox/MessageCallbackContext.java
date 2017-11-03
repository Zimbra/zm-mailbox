package com.zimbra.cs.mailbox;

import com.zimbra.cs.mailbox.Mailbox.MessageCallback;

public class MessageCallbackContext {
    private String recipient;
    private Long timestamp;
    private MessageCallback.Type type;

    public MessageCallbackContext(MessageCallback.Type type) {
        this.type = type;
    }

    public MessageCallback.Type getCallbackType() {
        return type;
    }

    public String getRecipient() {
        return recipient;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}