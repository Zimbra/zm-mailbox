package com.zimbra.cs.mailbox;

import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Message.EventFlag;

public class LocalMessageState extends LocalMailItemState implements IMessageState {

    public LocalMessageState(UnderlyingData data) {
        super(data);
    }

    @Override
    public EventFlag getEventFlag() {
        return EventFlag.of(data.eventFlag);
    }

    @Override
    public void setEventFlag(EventFlag eventFlag) {
        data.eventFlag = eventFlag.getId();
    }

    @Override
    public void setEventFlag(EventFlag eventFlag, AccessMode accessMode) {
        data.eventFlag = eventFlag.getId();
    }
}
