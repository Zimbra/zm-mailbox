package com.zimbra.cs.mailbox;

public class InMemoryIdProvider extends IdProvider {

    public InMemoryIdProvider(Mailbox mbox) {
        super(mbox);
    }

    @Override
    protected SynchronizedId initId(IdType idType) {
        return new InMemoryId();
    }

    private static class InMemoryId implements IdProvider.SynchronizedId {

        private int value;

        @Override
        public int value() {
            return value;
        }

        @Override
        public void increment(int delta) {
            value += delta;

        }

        @Override
        public int setIfNotExists(int value) {
            this.value = value;
            return value;
        }
    }

    public static class Factory implements IdProvider.Factory {

        @Override
        public IdProvider getIdProvider(Mailbox mbox) {
            return new InMemoryIdProvider(mbox);
        }
    }
}
