package com.zimbra.cs.pubsub.message;

public class PubSubMsg {
    private String targetService;

    // Default for serialization - don't remove
    public PubSubMsg() {
        this.targetService = "all";
    }

    public PubSubMsg(String targetService) {
        this.targetService = targetService;
    }

    public void setTargetService(String targetService) {
        if (targetService != null) {
            this.targetService = targetService;
        }
    }

    public String getTargetService() {
        return this.targetService;
    }

    public String toString() {
        return String.format("PubSubMsg[targetService=%s]", this.targetService);
    }
}
