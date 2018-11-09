package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.mailbox.Mailbox.MailboxData;

/**
 * This class is used to synchronize mailbox state across multiple mailbox workers.
 * It is a wrapper around {@link MailboxData}, which serves as the ground truth in cases
 * when the shared state provider (such as Redis) does not yet initialized the state for this mailbox.
 * If data already exists in the state provider, then the MailboxData fields are set to the values
 * cached there, as they may be more current.
 *
 * @author iraykin
 *
 */
public abstract class MailboxState {

    private static Factory factory;
    protected MailboxData data;
    private Map<MailboxField, SynchronizedField<?>> fieldMap = new HashMap<>();
    protected TransactionCacheTracker cacheTracker;

    public MailboxState(MailboxData data, TransactionCacheTracker cacheTracker) {
        this.data = data;
        this.cacheTracker = cacheTracker;
        init();
    }

    protected void init() {
        for (MailboxField fieldType: MailboxField.values()) {
            fieldMap.put(fieldType, initField(fieldType));
        }

        //for each mailbox field, we initialize the cluster-wide state to the current value (if it is unset),
        //or set the *local* value to that of the state provider
        data.lastItemId     = getIntField(MailboxField.ITEM_ID).setIfNotExists(data.lastItemId);
        data.lastSearchId   = getIntField(MailboxField.SEARCH_ID).setIfNotExists(data.lastSearchId);
        data.lastChangeId   = getIntField(MailboxField.CHANGE_ID).setIfNotExists(data.lastChangeId);
        data.size           = getLongField(MailboxField.SIZE).setIfNotExists(data.size);
        data.contacts       = getIntField(MailboxField.NUM_CONTACTS).setIfNotExists(data.contacts);
        data.lastBackupDate = getIntField(MailboxField.LAST_BACKUP_DATE).setIfNotExists(data.lastBackupDate);
        data.lastChangeDate = getLongField(MailboxField.LAST_CHANGE_DATE).setIfNotExists(data.lastChangeDate);
        data.lastWriteDate  = getIntField(MailboxField.LAST_WRITE_DATE).setIfNotExists(data.lastWriteDate);
        data.configKeys     = getStringSetField(MailboxField.CONFIG_KEYS).setIfNotExists(data.configKeys);
        data.itemcacheCheckpoint = getIntField(MailboxField.ITEMCACHE_CHECKPOINT).setIfNotExists(data.itemcacheCheckpoint);
        data.recentMessages = getIntField(MailboxField.RECENT_MESSAGES).setIfNotExists(data.recentMessages);
        data.trackImap      = getBoolField(MailboxField.TRACKING_IMAP).setIfNotExists(data.trackImap);
        data.trackSync      = getIntField(MailboxField.TRACK_SYNC).setIfNotExists(data.trackSync);
    }

    protected abstract SynchronizedField<?> initField(MailboxField fieldType);

    public long getSize() {
        return getLongField(MailboxField.SIZE).value();
    }

    public int getNumContacts() {
        return getIntField(MailboxField.NUM_CONTACTS).value();
    }

    public int getLastBackupDate() {
        return getIntField(MailboxField.LAST_BACKUP_DATE).value();
    }

    public long getLastChangeDate() {
        return getLongField(MailboxField.LAST_CHANGE_DATE).value();
    }

    public int getLastWriteDate() {
        return getIntField(MailboxField.LAST_WRITE_DATE).value();
    }

    public int getRecentMessages() {
        return getIntField(MailboxField.RECENT_MESSAGES).value();
    }

    public boolean isTrackingImap() {
        return getBoolField(MailboxField.TRACKING_IMAP).value();
    }

    public int getTrackSync() {
        return getIntField(MailboxField.TRACK_SYNC).value();
    }

    public Set<String> getConfigKeys() {
        return getStringSetField(MailboxField.CONFIG_KEYS).value();
    }

    public int getItemcacheCheckpoint() {
        return getIntField(MailboxField.ITEMCACHE_CHECKPOINT).value();
    }

    public int getItemId() {
        return getIntField(MailboxField.ITEM_ID).value();
    }

    public int getSearchId() {
        return getIntField(MailboxField.SEARCH_ID).value();
    }

    public int getChangeId() {
        return getIntField(MailboxField.CHANGE_ID).value();
    }


    public void setSize(long size) {
        getLongField(MailboxField.SIZE).set(size);
        data.size = size;
    }

    public void setNumContacts(int numContacts) {
        getIntField(MailboxField.NUM_CONTACTS).set(numContacts);
        data.contacts = numContacts;
    }

    public void setLastBackupDate(int backupDate) {
        getIntField(MailboxField.LAST_BACKUP_DATE).set(backupDate);
        data.lastBackupDate = backupDate;
    }

    public void setLastChangeDate(long changeDate) {
        getLongField(MailboxField.LAST_CHANGE_DATE).set(changeDate);
        data.lastChangeDate = changeDate;
    }

    public void setLastWriteDate(int writeDate) {
        getIntField(MailboxField.LAST_WRITE_DATE).set(writeDate);
        data.lastWriteDate = writeDate;
    }

    public void setRecentMessages(int numMessages) {
        getIntField(MailboxField.RECENT_MESSAGES).set(numMessages);
        data.recentMessages = numMessages;
    }

    public void setTrackingImap(boolean bool) {
        getBoolField(MailboxField.TRACKING_IMAP).set(bool);
        data.trackImap = bool;
    }

    public void setTrackSync(int trackSync) {
        getIntField(MailboxField.TRACK_SYNC).set(trackSync);
        data.trackSync = trackSync;
    }

    public void setItemcacheCheckpoint(int checkpoint) {
        getIntField(MailboxField.ITEMCACHE_CHECKPOINT).set(checkpoint);
        data.itemcacheCheckpoint = checkpoint;
    }

    public void setItemId(int value) {
        getIntField(MailboxField.ITEM_ID).set(value);
        data.lastItemId = value;
    }

    public void setSearchId(int value) {
        getIntField(MailboxField.SEARCH_ID).set(value);
        data.lastSearchId = value;
    }

    public void setChangeId(int value) {
        getIntField(MailboxField.CHANGE_ID).set(value);
        data.lastChangeId = value;
    }

    public void addConfigKey(String key) {
        Set<String> keys = getConfigKeys();
        if (keys == null) {
            keys = new HashSet<String>();
        }
        keys.add(key);
        getStringSetField(MailboxField.CONFIG_KEYS).set(keys);
        if (data.configKeys == null) {
            data.configKeys = new HashSet<String>(1);
            data.configKeys.add(key);
        }
    }

    public void removeConfigKey(String key) {
        Set<String> keys = getConfigKeys();
        if (keys == null || keys.isEmpty()) {
            return;
        }
        keys.remove(key);
        getStringSetField(MailboxField.CONFIG_KEYS).set(keys);
        if (data.configKeys != null) {
            data.configKeys.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> SynchronizedField<T> getField(MailboxField field) {
        return (SynchronizedField<T>) fieldMap.get(field);
    }

    private SynchronizedField<Integer> getIntField(MailboxField field) {
        SynchronizedField<Integer> f = getField(field);
        return f;
    }

    private SynchronizedField<Set<String>> getStringSetField(MailboxField field){
        SynchronizedField<Set<String>> f = getField(field);
        return f;
    }

    private SynchronizedField<Long> getLongField(MailboxField field) {
        SynchronizedField<Long> f = getField(field);
        return f;
    }

    private SynchronizedField<Boolean> getBoolField(MailboxField field) {
        SynchronizedField<Boolean> f = getField(field);
        return f;
    }

    public static Factory getFactory() {
        return factory;
    }

    public static void setFactory(Factory factory) {
        MailboxState.factory = factory;
    }

    /**
     * An enum corresponding to fields in {@link MailboxData}
     */
    protected static enum MailboxField {
        ITEM_ID(FieldType.INT),
        SEARCH_ID(FieldType.INT),
        CHANGE_ID(FieldType.INT),
        SIZE(FieldType.LONG),
        NUM_CONTACTS(FieldType.INT),
        LAST_BACKUP_DATE(FieldType.INT),
        LAST_CHANGE_DATE(FieldType.LONG),
        LAST_WRITE_DATE(FieldType.INT),
        RECENT_MESSAGES(FieldType.INT),
        TRACKING_IMAP(FieldType.BOOL),
        TRACK_SYNC(FieldType.INT),
        CONFIG_KEYS(FieldType.SET),
        ITEMCACHE_CHECKPOINT(FieldType.INT);

        private FieldType type;

        private MailboxField(FieldType type) {
            this.type = type;
        }

        public FieldType getType() {
            return type;
        }
    }

    /**
     * Helper enum used as a type hint for MailboxField values
     */
    protected static enum FieldType {
        BOOL, INT, LONG, STRING, SET
    }

    /**
     * Helper class representing a value of a Mailbox field shared across all mailbox nodes
     * using some state provider
     */
    protected static interface SynchronizedField<T> {

        /**
         * Get the current value of the field
         */
        T value();

        /**
         * Set the field value
         */
        void set(T value);

        /**
         * If the field is unset, set it to the given value.
         * @return the field value
         */
        T setIfNotExists(T value);
    }

    public static interface Factory {

        public MailboxState getMailboxState(MailboxData data, TransactionCacheTracker tracker);
    }
}
