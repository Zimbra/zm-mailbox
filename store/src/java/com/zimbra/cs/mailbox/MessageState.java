package com.zimbra.cs.mailbox;

import com.zimbra.cs.mailbox.Message.EventFlag;


public interface MessageState extends MailItemState {

    public abstract EventFlag getEventFlag();

    public abstract void setEventFlag(EventFlag eventFlag);

    public abstract void setEventFlag(EventFlag eventFlag, AccessMode accessMode);
}
