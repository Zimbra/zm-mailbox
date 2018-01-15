package com.zimbra.cs.ml.query;

import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.schema.MessageClassificationInput;

public class MessageClassificationQuery extends AbstractClassificationQuery<Message> {

    private String classifierId;
    private String receiverAddr;

    public MessageClassificationQuery(String classifierId, String receiverAddr, MessageClassificationInput input) {
        super(input);
        this.classifierId = classifierId;
        this.receiverAddr = receiverAddr;
    }

    public String getClassifierId() {
        return classifierId;
    }

    public String getReceiverAddress() {
        return receiverAddr;
    }

    @Override
    public MessageClassificationInput getInput() {
        return (MessageClassificationInput) input;
    }
}
