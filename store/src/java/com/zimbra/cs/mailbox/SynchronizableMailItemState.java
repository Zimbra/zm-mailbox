package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.Map;

import org.redisson.client.RedisException;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Tag.NormalizedTags;
import com.zimbra.cs.mailbox.cache.SharedStateAccessor;
import com.zimbra.soap.mail.type.RetentionPolicy;

/**
 * Class encapsulating all state that underlies a MailItem.
 * @author iraykin
 *
 */
public class SynchronizableMailItemState implements MailItemState {

    protected final UnderlyingData data;
    private ACL rights;
    private Color color;
    private int metadataVersion = 1;
    private int version = 1;
    private int numIndexDocs = 0;
    private RetentionPolicy retentionPolicy;
    private SharedStateAccessor sharedState;

    private final Map<String, ItemField<?>> fields;

    public static final String F_NAME = "name";
    public static final String F_TYPE = "type";
    public static final String F_UUID = "uuid";
    public static final String F_SUBJECT = "subject";
    public static final String F_PARENT_ID = "parentId";
    public static final String F_FOLDER_ID = "folderId";
    public static final String F_INDEX_ID = "indexId";
    public static final String F_IMAP_ID = "imapId";
    public static final String F_PREV_FOLDERS = "prevFolders";
    public static final String F_LOCATOR = "locator";
    public static final String F_BLOB_DIGEST = "blogDigest";
    public static final String F_MOD_METADATA = "modMetadata";
    public static final String F_MOD_CONTENT = "modContent";
    public static final String F_DATE = "date";
    public static final String F_DATE_CHANGED = "dateChanged";
    public static final String F_FLAGS = "flags";
    public static final String F_TAGS = "tags";
    public static final String F_SMARTFOLDERS = "smartFolders";
    public static final String F_SIZE = "size";
    public static final String F_METADATA = "metadata";
    public static final String F_UNREAD_COUNT = "unreadCount";
    public static final String F_METADATA_VERSION = "metadataVersion";
    public static final String F_VERSION = "version";
    public static final String F_COLOR = "color";
    public static final String F_RIGHTS = "rights";
    public static final String F_RETENTION_POLICY = "retentionPolicy";
    public static final String F_NUM_INDEX_DOCS = "numIndexDocs";

    public SynchronizableMailItemState(UnderlyingData data) {
        this.data = data;
        fields = new HashMap<>();
        initFields();
    }

    public synchronized void setSharedStateAccessor(SharedStateAccessor accessor) {
        sharedState = accessor;
    }

    public synchronized boolean hasSharedStateAccessor() {
        return sharedState != null;
    }

    public synchronized void clearSharedStateAccessor() {
        if (sharedState != null) {
            sharedState.delete();
            sharedState = null;
        }
    }

    @Override
    public String getName() {
        return getStringField(F_NAME).get();
    }

    @Override
    public void setName(String name) {
        getField(F_NAME).set(name);
    }

    @Override
    public String getSubject() {
        return getStringField(F_SUBJECT).get();
    }

    @Override
    public void setSubject(String subject) {
        getField(F_SUBJECT).set(subject);
    }

    @Override
    public int getParentId() { return getIntFieldValue(F_PARENT_ID); }
    @Override
    public void setParentId(int parentId) {
        getField(F_PARENT_ID).set(parentId);
    }

    @Override
    public int getFolderId() { return getIntFieldValue(F_FOLDER_ID); }
    @Override
    public void setFolderId(int folderId) {
        getField(F_FOLDER_ID).set(folderId);
    }


    @Override
    public int getIndexId() {
        return getIntFieldValue(F_INDEX_ID);
    }
    @Override
    public void setIndexId(int indexId) {
        getField(F_INDEX_ID).set(indexId);
    }

    @Override
    public int getImapId() { return getIntFieldValue(F_IMAP_ID); }
    @Override
    public void setImapId(int imapId) {
        getField(F_IMAP_ID).set(imapId);
    }

    @Override
    public String getPrevFolders() {
        return getStringField(F_PREV_FOLDERS).get();
    }

    @Override
    public void setPrevFolders(String prevFolders) {
        getField(F_PREV_FOLDERS).set(prevFolders);
    }

    @Override
    public String getLocator() {
        return getStringField(F_LOCATOR).get();
    }

    @Override
    public void setLocator(String locator) {
        getField(F_LOCATOR).set(locator);
    }

    @Override
    public String getBlobDigest() {
        return getStringField(F_BLOB_DIGEST).get();
    }

    @Override
    public void setBlobDigest(String digest) {
        getField(F_BLOB_DIGEST).set(digest);
    }

    @Override
    public int getModMetadata() { return getIntFieldValue(F_MOD_METADATA); }
    @Override
    public void setModMetadata(int modMetadata) {
        getField(F_MOD_METADATA).set(modMetadata);
    }

    @Override
    public int getModContent() { return getIntFieldValue(F_MOD_CONTENT); }
    @Override
    public void setModContent(int modContent) {
        getField(F_MOD_CONTENT).set(modContent);
    }

    @Override
    public int getDate() { return getIntFieldValue(F_DATE); }
    @Override
    public void setDate(int date) {
        getField(F_DATE).set(date);
    }

    @Override
    public int getDateChanged() { return getIntFieldValue(F_DATE_CHANGED); }
    @Override
    public void setDateChanged(int dateChanged) {
        getField(F_DATE_CHANGED).set(dateChanged);
    }

    private ItemField<Integer> flagField() {
        return getIntField(F_FLAGS);
    }

    @Override
    public int getFlags() {
        return flagField().get();
    }

    @Override
    public boolean isSet(FlagInfo flag) {
        flagField().refresh();
        return data.isSet(flag);
    }

    @Override
    public void setFlag(FlagInfo flag) {
        flagField().refresh().set(data.setFlag(flag).getFlags());
    }

    @Override
    public void setFlag(Flag flag) {
        flagField().refresh().set(data.setFlag(flag).getFlags());
    }

    @Override
    public void unsetFlag(FlagInfo flag) {
        flagField().refresh().set(data.unsetFlag(flag).getFlags());
    }

    @Override
    public void unsetFlag(Flag flag) {
        flagField().refresh().set(data.unsetFlag(flag).getFlags());
    }

    @Override
    public void setFlags(int flags) {
        flagField().set(data.setFlags(flags).getFlags());
    }


    private ItemField<String[]> tagField() {
        return getStringArrField(F_TAGS);
    }

    private ItemField<String[]> smartFolderField() {
        return getStringArrField(F_SMARTFOLDERS);
    }

    @Override
    public String[] getTags() {
        return tagField().get();
    }

    @Override
    public void setTags(String[] tags) {
        tagField().set(tags);
    }

    @Override
    public void setTags(NormalizedTags tags) {
        data.setTags(tags);
        tagField().set(data.getTags(), AccessMode.REMOTE_ONLY);
        smartFolderField().set(data.getSmartFolders(), AccessMode.REMOTE_ONLY);
    }

    @Override
    public String[] getSmartFolders() {
        return getStringArrField(F_SMARTFOLDERS).get();
    }

    @Override
    public void setSmartFolders(String[] smartFolders) {
        getField(F_SMARTFOLDERS).set(smartFolders);
    }

    @Override
    public long getSize() { return getLongFieldValue(F_SIZE); }

    @Override
    public void setSize(long size) {
        getField(F_SIZE).set(size);
    }

    @Override
    public int getUnreadCount() {
        return getIntFieldValue(F_UNREAD_COUNT);
    }
    @Override
    public void setUnreadCount(int unreadCount) {
        getField(F_UNREAD_COUNT).set(unreadCount);
    }

    @Override
    public Color getColor() {
        ItemField<Color> field = getField(F_COLOR);
        return field.get();
    }

    @Override
    public void setColor(Color color) {
        setColor(color, AccessMode.DEFAULT);
    }

    @Override
    public void setColor(Color color, AccessMode setMode) {
        ItemField<Color> field = getField(F_COLOR);
        field.set(color, setMode);
    }

    @Override
    public ACL getRights() {
        ItemField<ACL> field = getField(F_RIGHTS);
        return field.get();
    }

    @Override
    public void setRights(ACL rights) {
        setRights(rights, AccessMode.DEFAULT);
    }

    @Override
    public void setRights(ACL rights, AccessMode setMode) {
         getField(F_RIGHTS).set(rights, setMode);
    }

    @Override
    public int getVersion() {
        return getIntFieldValue(F_VERSION);
    }
    @Override
    public void setVersion(int version) {
        setVersion(version, AccessMode.DEFAULT);
    }

    @Override
    public void setVersion(int version, AccessMode setMode) {
        getField(F_VERSION).set(version, setMode);
    }

    @Override
    public void incrementVersion() {
        getField(F_VERSION).set(getVersion() + 1);
    }

    @Override
    public int getMetadataVersion() {
        return getIntFieldValue(F_METADATA_VERSION);
    }

    @Override
    public void setMetadataVersion(int metadataVersion) {
        setMetadataVersion(metadataVersion, AccessMode.DEFAULT);
    }

    @Override
    public void setMetadataVersion(int metadataVersion, AccessMode setMode) {
        getField(F_METADATA_VERSION).set(metadataVersion, setMode);
    }

    @Override
    public void incrementMetadataVersion() {
        getField(F_METADATA_VERSION).set(getMetadataVersion() + 1);
    }

    @Override
    public UnderlyingData getUnderlyingData() {
        return data;
    }

    @Override
    public void metadataChanged(Mailbox mbox, boolean updateFolderMODSEQ) throws ServiceException {
        data.metadataChanged(mbox, updateFolderMODSEQ);
    }

    @Override
    public void contentChanged(Mailbox mbox) throws ServiceException {
        data.contentChanged(mbox);
    }

    @Override
    public void contentChanged(Mailbox mbox, boolean bool) throws ServiceException {
        data.contentChanged(mbox, bool);
    }

    @Override
    public void saveMetadata(MailItem item, String metadata) throws ServiceException {
        DbMailItem.saveMetadata(item, metadata);
        if (sharedState != null) {
            getField(F_METADATA).set(metadata, AccessMode.REMOTE_ONLY);
        }
    }

    @Override
    public RetentionPolicy getRetentionPolicy() {
        ItemField<RetentionPolicy> field = getField(F_RETENTION_POLICY);
        return field.get();
    }

    @Override
    public void setRetentionPolicy(RetentionPolicy policy) {
        setRetentionPolicy(policy, AccessMode.DEFAULT);
    }

    @Override
    public void setRetentionPolicy(RetentionPolicy policy, AccessMode setMode) {
        getField(F_RETENTION_POLICY).set(policy, setMode);
    }

    @Override
    public int getNumIndexDocs() {
        return getIntFieldValue(F_NUM_INDEX_DOCS);
    }

    @Override
    public void setNumIndexDocs(int numIndexDocs) {
        setNumIndexDocs(numIndexDocs, AccessMode.DEFAULT);
    }

    @Override
    public void setNumIndexDocs(int numIndexDocs, AccessMode setMode) {
        getField(F_NUM_INDEX_DOCS).set(numIndexDocs, setMode);
    }

    protected abstract class ItemField<T> {

        protected final String name;

        public ItemField(String name) {
            this.name = name;
        }

        public void set(T value) {
            set(value, AccessMode.DEFAULT);
        }

        public void set(T value, AccessMode setMode) {
            if (setMode != AccessMode.REMOTE_ONLY) {
                setLocal(value);
            }
            if (sharedState != null && setMode != AccessMode.LOCAL_ONLY) {
                setSharedState(value);
            }
        }

        public ItemField<T> refresh() {
            return this;
        }

        protected void setSharedState(T value) {
            if (sharedState == null) {
                ZimbraLog.cache.debug("setSharedState(%s) (name=%s) - NULL sharedState, so ignoring! %s",
                        value, name, SynchronizableMailItemState.this);
                return;
            }
            if (value == null) {
                ZimbraLog.cache.debug("setSharedState(%s) (name=%s) - NULL values not supported, so ignoring! %s",
                        value, name, SynchronizableMailItemState.this);
                return;
            }
            sharedState.set(name, value);
        }

        protected T getSharedState() {
            if (sharedState == null) {
                ZimbraLog.cache.debug("getSharedState() (name=%s) - NULL sharedState, so returning null! %s",
                        name, SynchronizableMailItemState.this);
                return null;
            }
            T retVal = sharedState.get(name);
            return retVal;
        }

        public T get() {
            if (sharedState != null) {
                try {
                    T sharedValue = getSharedState();
                    // Note that, at least in the case of Redis, the maps used to store key/value pairs cannot have
                    // null values, so if the value is null here, that means shared state did not have a value for it
                    // the most probable cause for this is the shared object being uncached, which at that point
                    // causes the map to be emptied.
                    if (sharedValue != null) {
                        //update the local value in case it's stale
                        setLocal(sharedValue);
                        return sharedValue;
                    }
                } catch (RedisException e) {
                    ZimbraLog.cache.error("unable to get value for field '%s' from redis, using local value %s",
                            name, SynchronizableMailItemState.this, e);
                }
            }
            return getLocal();
        }

        protected abstract void setLocal(T value);
        protected abstract T getLocal();

        public boolean sync() {
            T localValue = getLocal();
            if (sharedState != null && hasData(localValue)) {
                set(localValue, AccessMode.REMOTE_ONLY);
                return true;
            } else {
                return false;
            }
        }

        protected boolean hasData(T localValue) {
            return localValue != null;
        }

        public void unset() {
            set(null, AccessMode.LOCAL_ONLY);
            if (sharedState != null) {
                sharedState.unset(name);
            }
        }
    }

    protected <T> void addField(ItemField<T> field) {
        fields.put(field.name, field);
    }

    protected ItemField<String> getStringField(String fieldName) {
        return getField(fieldName);
    }

    /**
     * Wrapper round getIntField(fieldName).get() which logs if going to end up throwing an NPE so
     * that have more context for the cause of the NPE.  Of course, should never end up with a null...
     */
    protected int getIntFieldValue(String fieldName) {
        Integer retVal = getIntField(fieldName).get();
        if (retVal == null) {
            ZimbraLog.cache.info("getIntFieldValue(%s) returning null! %s", fieldName, this);
        }
        return retVal;
    }

    /**
     * Wrapper round getLongField(fieldName).get() which logs if going to end up throwing an NPE so
     * that have more context for the cause of the NPE.  Of course, should never end up with a null...
     */
    protected long getLongFieldValue(String fieldName) {
        Long retVal = getLongField(fieldName).get();
        if (retVal == null) {
            ZimbraLog.cache.info("getLongFieldValue(%s) returning null! %s", fieldName, this);
        }
        return retVal;
    }

    protected ItemField<Integer> getIntField(String fieldName) {
        return getField(fieldName);
    }

    protected ItemField<String[]> getStringArrField(String fieldName) {
        return getField(fieldName);
    }

    protected ItemField<Long> getLongField(String fieldName) {
        return getField(fieldName);
    }

    protected ItemField<Boolean> getBoolField(String fieldName) {
        return getField(fieldName);
    }

    @SuppressWarnings("unchecked")
    protected <T> ItemField<T> getField(String fieldName) {
        ItemField<T> field = (ItemField<T>) fields.get(fieldName);
        if (field == null) {
            ZimbraLog.cache.info("getField(%s) returning null! %s", fieldName, SynchronizableMailItemState.this);
        }
        return field;
    }

    public void syncWithSharedState(MailItem item) {
        if (sharedState != null) {
            boolean syncedMetadata = false;
            for (ItemField<?> field: fields.values()) {
                try {
                    boolean synced = field.sync();
                    if (synced && field.name.equals(F_METADATA)) {
                        syncedMetadata = true;
                    }
                } catch (Exception e) {
                    ZimbraLog.cache.error("unable to serialize field %s", field.name, e);
                }
            }
            if (!syncedMetadata) {
                try {
                    String encodedMeta = item.encodeMetadata().toString();
                    getField(F_METADATA).set(encodedMeta, AccessMode.REMOTE_ONLY);
                } catch (ServiceException e) {
                    ZimbraLog.cache.error("unable to encode metadata for %s %s", item.getType().toString(), item.getId(), e);
                }
            }
        }
    }

    /**
     * Used to set an Integer value where the target is an int.  Logs if the value would be null and tries to choose
     * an appropriate value instead.
     */
    protected int newIntLocalValue(ItemField<Integer>field, Integer value) {
        if (value == null) {
            Integer currVal = field.getLocal();
            ZimbraLog.cache.info("newIntLocalValue(%s, %s) - attempting to keep current value=%s! %s",
                    field.name, value, currVal, SynchronizableMailItemState.this);
            return currVal == null ? 0 : currVal;
        }
        return value;
    }

    /**
     * Used to set a Long value where the target is a long.  Logs if the value would be null and tries to choose
     * an appropriate value instead.
     */
    protected long newLongLocalValue(ItemField<Long>field, Long value) {
        if (value == null) {
            Long currVal = field.getLocal();
            ZimbraLog.cache.info("newLongLocalValue(%s, %s) - attempting to keep current value=%s! %s",
                    field.name, value, currVal, SynchronizableMailItemState.this);
            return currVal == null ? 0 : currVal;
        }
        return value;
    }

    protected void initFields() {
        addField(new ItemField<String>(F_NAME) {

            @Override
            protected void setLocal(String value) { data.name = value; }

            @Override
            protected String getLocal() { return data.name; }
        });

        addField(new ItemField<String>(F_TYPE) {

            @Override
            protected void setLocal(String value) { if (value != null) data.type = Type.of(value).toByte(); }

            @Override
            protected String getLocal() { return Type.of(data.type).toString(); }
        });

        addField(new ItemField<String>(F_UUID) {

            @Override
            protected void setLocal(String value) { data.uuid = value; }

            @Override
            protected String getLocal() { return data.uuid; }
        });

        addField(new ItemField<String>(F_SUBJECT) {

            @Override
            protected void setLocal(String value) { data.setSubject(value); }

            @Override
            protected String getLocal() { return data.getSubject(); }
        });

        addField(new ItemField<Integer>(F_PARENT_ID) {

            @Override
            protected void setLocal(Integer value) {
                data.parentId = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.parentId; }
        });

        addField(new ItemField<Integer>(F_FOLDER_ID) {

            @Override
            protected void setLocal(Integer value) {
                data.folderId = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.folderId; }
        });

        addField(new ItemField<Integer>(F_INDEX_ID) {

            @Override
            protected void setLocal(Integer value) {
                data.indexId = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.indexId; }
        });

        addField(new ItemField<Integer>(F_IMAP_ID) {

            @Override
            protected void setLocal(Integer value) {
                data.imapId = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.imapId; }
        });

        addField(new ItemField<String>(F_PREV_FOLDERS) {

            @Override
            protected void setLocal(String value) { data.setPrevFolders(value); }

            @Override
            protected String getLocal() { return data.getPrevFolders(); }
        });

        addField(new ItemField<String>(F_LOCATOR) {

            @Override
            protected void setLocal(String value) { data.locator = value; }

            @Override
            protected String getLocal() { return data.locator; }
        });

        addField(new ItemField<String>(F_BLOB_DIGEST) {

            @Override
            protected void setLocal(String value) { data.setBlobDigest(value); }

            @Override
            protected String getLocal() { return data.getBlobDigest(); }
        });

        addField(new ItemField<Integer>(F_MOD_METADATA) {

            @Override
            protected void setLocal(Integer value) {
                data.modMetadata = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.modMetadata; }
        });

        addField(new ItemField<Integer>(F_MOD_CONTENT) {

            @Override
            protected void setLocal(Integer value) {
                data.modContent = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.modContent; }
        });

        addField(new ItemField<Integer>(F_DATE) {

            @Override
            protected void setLocal(Integer value) {
                data.date = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.date; }
        });

        addField(new ItemField<Integer>(F_DATE_CHANGED) {

            @Override
            protected void setLocal(Integer value) {
                data.dateChanged = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.dateChanged; }
        });

        addField(new ItemField<Integer>(F_FLAGS) {

            @Override
            protected void setLocal(Integer value) {
                if (value == null) {
                    ZimbraLog.cache.info("setLocal(%s) (name=%s) - ignoring! %s",
                            value, name, SynchronizableMailItemState.this);
                    return;
                }
                data.setFlags(value);
            }

            @Override
            protected Integer getLocal() { return data.getFlags(); }

            @Override
            public ItemField<Integer> refresh() {
                if (sharedState == null) {
                    return this;
                }
                Integer flags = sharedState.get(F_FLAGS);
                setLocal(flags);
                return this;
            }
        });

        addField(new ItemField<String[]>(F_TAGS) {

            @Override
            protected void setLocal(String[] value) { data.setTags(value); }

            @Override
            protected String[] getLocal() { return data.getTags(); }

            @Override
            protected void setSharedState(String[] tags) {
                if (sharedState == null) {
                    ZimbraLog.cache.info("setSharedState(%s) (name=%s) - NULL sharedState, so ignoring! %s",
                            tags, name, SynchronizableMailItemState.this);
                    return;
                }
                sharedState.set(name, Joiner.on(",").join(tags));
            }

            @Override
            protected String[] getSharedState() {
                if (sharedState == null) {
                    ZimbraLog.cache.info("getSharedState() (name=%s) - sharedState=null! return empty array. %s",
                            name, SynchronizableMailItemState.this);
                    return new String[0];
                }
                String tags = sharedState.get(name);
                return tags == null ? new String[0] : tags.split(",");
            }

            @Override
            protected boolean hasData(String[] tags) {
                return tags != null && tags.length > 0;
            }
        });

        addField(new ItemField<String[]>(F_SMARTFOLDERS) {

            @Override
            protected void setLocal(String[] value) { data.setSmartFolders(value); }

            @Override
            protected String[] getLocal() { return data.getSmartFolders(); }

            @Override
            protected void setSharedState(String[] smartFolders) {
                if (sharedState == null) {
                    ZimbraLog.cache.info("setSharedState(%s) (name=%s) - NULL sharedState, so ignoring! %s",
                            smartFolders, name, SynchronizableMailItemState.this);
                    return;
                }
                sharedState.set(name, Joiner.on(",").join(smartFolders));
            }

            @Override
            protected String[] getSharedState() {
                if (sharedState == null) {
                    ZimbraLog.cache.info("getSharedState() (name=%s) - sharedState=null! return empty array. %s",
                            name, SynchronizableMailItemState.this);
                    return new String[0];
                }
                String smartFolders = sharedState.get(name);
                return smartFolders == null ? new String[0] : smartFolders.split(",");
            }

            @Override
            protected boolean hasData(String[] smartFolders) {
                return smartFolders != null && smartFolders.length > 0;
            }
        });

        addField(new ItemField<Long>(F_SIZE) {

            @Override
            protected void setLocal(Long value) {
                data.size = newLongLocalValue(this, value);
            }

            @Override
            protected Long getLocal() { return data.size; }
        });

        addField(new ItemField<Integer>(F_UNREAD_COUNT) {

            @Override
            protected void setLocal(Integer value) {
                data.unreadCount = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return data.unreadCount; }
        });

        addField(new ItemField<String>(F_METADATA) {

            @Override
            protected void setLocal(String value) { data.metadata = value; }

            @Override
            protected String getLocal() { return data.metadata; }
        });

        addField(new ItemField<Integer>(F_METADATA_VERSION) {

            @Override
            protected void setLocal(Integer value) {
                metadataVersion = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return metadataVersion; }
        });

        addField(new ItemField<Integer>(F_VERSION) {

            @Override
            protected void setLocal(Integer value) {
                version = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return SynchronizableMailItemState.this.version; }
        });

        addField(new ItemField<Color>(F_COLOR) {

            @Override
            protected void setLocal(Color value) { color = value; }

            @Override
            protected Color getLocal() { return color; }

            @Override
            protected void setSharedState(Color color) {
                if (sharedState == null) {
                    ZimbraLog.cache.info("setSharedState(%s) (name=%s) - NULL sharedState, so ignoring! %s",
                            color, name, SynchronizableMailItemState.this);
                    return;
                }
                sharedState.set(name, color == null ? 0L : color.toMetadata());
            }

            @Override
            protected Color getSharedState() {
                if (sharedState == null) {
                    ZimbraLog.cache.info("getSharedState() (name=%s) - NULL sharedState, so returning null! %s",
                            name, SynchronizableMailItemState.this);
                    return null;
                }
                Long colorValue = sharedState.get(name);
                if (colorValue == null) {
                    ZimbraLog.cache.info("getSharedState() (name=%s) - returning null! %s",
                            name, SynchronizableMailItemState.this);
                    return null;
                }
                return Color.fromMetadata(colorValue);
            }
        });

        addField(new ItemField<ACL>(F_RIGHTS) {

            @Override
            protected void setLocal(ACL value) { rights = value; }

            @Override
            protected ACL getLocal() { return rights; }

            @Override
            protected void setSharedState(ACL rights) {
                if (sharedState == null) {
                    ZimbraLog.cache.info("setSharedState(%s) (name=%s) - NULL sharedState, so ignoring! %s",
                            rights, name, SynchronizableMailItemState.this);
                    return;
                }
                if (rights == null) {
                    sharedState.set(name, "");
                } else {
                    sharedState.set(name, rights.encode().toString());
                }
            }

            @Override
            protected ACL getSharedState() {
                if (sharedState == null) {
                    ZimbraLog.cache.info("getSharedState() (name=%s) - NULL sharedState, so returning null! %s",
                            name, SynchronizableMailItemState.this);
                    return null;
                }
                String rightsMetaStr = sharedState.get(name);
                if (Strings.isNullOrEmpty(rightsMetaStr)) {
                    return null;
                }
                try {
                    Metadata rightsMeta = new Metadata(rightsMetaStr);
                    return new ACL(rightsMeta);
                } catch (MailServiceException e) {
                    ZimbraLog.cache.error("unable to decode ACL from string '%s'", rightsMetaStr, e);
                    return null;
                }
            }
        });

        addField(new ItemField<RetentionPolicy>(F_RETENTION_POLICY) {

            @Override
            protected void setLocal(RetentionPolicy value) { retentionPolicy = value; }

            @Override
            protected RetentionPolicy getLocal() { return retentionPolicy; }

            @Override
            protected void setSharedState(RetentionPolicy policy) {
                if (sharedState == null) {
                    ZimbraLog.cache.info("setSharedState(%s) (name=%s) - NULL sharedState, so ignoring! %s",
                            policy, name, SynchronizableMailItemState.this);
                    return;
                }
                sharedState.set(name, policy == null ? "" : RetentionPolicyManager.toMetadata(policy, true).toString());
            }

            @Override
            protected RetentionPolicy getSharedState() {
                if (sharedState == null) {
                    ZimbraLog.cache.info("getSharedState() (name=%s) - NULL sharedState, so returning null! %s",
                            name, SynchronizableMailItemState.this);
                    return null;
                }
                String policyMetaStr = sharedState.get(name);
                if (Strings.isNullOrEmpty(policyMetaStr)) {
                    return new RetentionPolicy();
                }
                try {
                    Metadata policyMeta = new Metadata(policyMetaStr);
                    return RetentionPolicyManager.retentionPolicyFromMetadata(policyMeta, true);
                } catch (ServiceException e) {
                    ZimbraLog.cache.error("unable to decode RetentionPolicy from string '%s'", policyMetaStr, e);
                    return null;
                }
            }
        });

        addField(new ItemField<Integer>(F_NUM_INDEX_DOCS) {

            @Override
            protected void setLocal(Integer value) {
                numIndexDocs = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return numIndexDocs; }
        });
    }

    protected MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this).omitNullValues()
                .add("version", version)
                .add("metadataVersion", metadataVersion)
                .add("data", data)
                .add("color", color)
                .add("retentionPolicy", retentionPolicy)
                .add("sharedState", sharedState)
                .add("numIndexDocs", numIndexDocs)
                .add("hashCode", System.identityHashCode(this));
    }

    @Override
    public final String toString() {
        return toStringHelper().toString();
    }
}
