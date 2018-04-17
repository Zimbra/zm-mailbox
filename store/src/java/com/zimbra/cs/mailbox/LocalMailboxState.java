package com.zimbra.cs.mailbox;

import java.util.Set;

import com.zimbra.cs.mailbox.Mailbox.MailboxData;

public class LocalMailboxState extends MailboxState {

    public LocalMailboxState(MailboxData data) {
        super(data);
    }

    @Override
    protected SynchronizedField<?> initField(MailboxField field) {
        switch(field.getType()) {
        case BOOL:
            return new InMemoryField<Boolean>();
        case INT:
            return new InMemoryField<Integer>();
        case LONG:
            return new InMemoryField<Long>();
        case SET:
            return new InMemoryField<Set<String>>();
        case STRING:
        default:
            return new InMemoryField<String>();
        }
    }

    protected static class InMemoryField<T> implements MailboxState.SynchronizedField<T> {

        private T value = null;

        @Override
        public T value() {
            return value;
        }

        @Override
        public void set(T value) {
            this.value = value;

        }

        @Override
        public T setIfNotExists(T value) {
            if (this.value == null) {
                this.value = value;
            }
            return value;
        }

    }
    public static class Factory implements MailboxState.Factory {

        @Override
        public MailboxState getMailboxState(MailboxData data) {
            return new LocalMailboxState(data);
        }
    }
}
