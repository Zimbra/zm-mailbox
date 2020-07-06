package com.zimbra.cs.pubsub.message;

public class BackupHostMsg extends PubSubMsg {

    private String originatingHost;

    // needed for serialization
    public BackupHostMsg() {}

    public BackupHostMsg(String originatingHost) {
        setOriginatingHost(originatingHost);
    }

    public void setOriginatingHost(String originatingHost) {
        this.originatingHost = originatingHost;
    }

    public String getOriginatingHost() {
        return originatingHost;
    }

    @Override
    public String toString() {
        return String.format("BackupHostMsg[origin=%s, selector=%s]",  originatingHost);
    }
}
