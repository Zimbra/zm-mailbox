package com.zimbra.cs.mailbox;

import com.zimbra.cs.session.Session.Type;

public class LocalPubSub extends NotificationPubSub {

    public LocalPubSub(Mailbox mbox) {
        super(mbox);
    }

    public class LocalPublisher extends Publisher {

        @Override
        public int getNumListeners(Type type) {
            return LocalPubSub.this.getSubscriber().getListeners(type).size();
        }
    }

    public class LocalSubscriber extends Subscriber {}

    @Override
    protected Publisher initPubisher() {
        return new LocalPublisher();
    }

    @Override
    protected Subscriber initSubscriber() {
        return new LocalSubscriber();
    }

    public static class Factory extends NotificationPubSub.Factory {

        @Override
        protected NotificationPubSub initPubSub(Mailbox mbox) {
            return new LocalPubSub(mbox);
        }

    }
}
