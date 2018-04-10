package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.Map;


/**
 * Abstract class used for ensuring monotonically increasing IDs in a distributed environment
 * @author iraykin
 */
public abstract class IdProvider {

    private static Factory factory;
    private Map<IdType, SynchronizedId> idMap = new HashMap<>();

    protected static enum IdType {
        ITEM_ID, SEARCH_ID, CHANGE_ID;
    }

    protected static interface SynchronizedId {

        /**
         * Get the current value of the ID
         */
        int value();

        /**
         * Increment the value of the ID by the given delta
         */
        void increment(int delta);

        /**
         * If the ID is unset, set it to the given value.
         * @return the ID value
         */
        int setIfNotExists(int value);
    }

    protected Mailbox mbox;

    public IdProvider(Mailbox mbox) {
        this.mbox = mbox;
        init();
    }

    protected void init() {
        for (IdType idType: IdType.values()) {
            idMap.put(idType, initId(idType));
        }
    }

    public static void setFactory(Factory factory) {
        IdProvider.factory = factory;
    }

    public static Factory getFactory() {
        return factory;
    }

    protected abstract SynchronizedId initId(IdType idType);

    private SynchronizedId getId(IdType idType) {
        return idMap.get(idType);
    }

    public IdProvider.SynchronizedId getItemId() {
        return getId(IdType.ITEM_ID);
    }

    public IdProvider.SynchronizedId getSearchId() {
        return getId(IdType.SEARCH_ID);
    }

    public IdProvider.SynchronizedId getChangeId() {
        return getId(IdType.CHANGE_ID);
    }

    public static interface Factory {

        public IdProvider getIdProvider(Mailbox mbox);
    }
}