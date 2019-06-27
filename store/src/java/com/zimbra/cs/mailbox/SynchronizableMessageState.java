package com.zimbra.cs.mailbox;

import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Message.EventFlag;

public class SynchronizableMessageState extends SynchronizableMailItemState implements MessageState {

    public static final String F_EVENT_FLAG = "eventFlag";

    public SynchronizableMessageState(UnderlyingData data) {
        super(data);
    }

    @Override
    public EventFlag getEventFlag() {
        ItemField<EventFlag> field = getField(F_EVENT_FLAG);
        return field.get();
    }

    @Override
    public void setEventFlag(EventFlag eventFlag) {
        getField(F_EVENT_FLAG).set(eventFlag, AccessMode.DEFAULT);
    }

    @Override
    public void setEventFlag(EventFlag eventFlag, AccessMode accessMode) {
        getField(F_EVENT_FLAG).set(eventFlag, accessMode);
    }

    @Override
    protected void initFields() {
        super.initFields();
        addField(new ItemField<EventFlag>(F_EVENT_FLAG) {

            @Override
            protected void setLocal(EventFlag value) { if (value != null) data.eventFlag = value.getId(); }

            @Override
            protected EventFlag getLocal() { return EventFlag.of(data.eventFlag); }
        });
    }

}
