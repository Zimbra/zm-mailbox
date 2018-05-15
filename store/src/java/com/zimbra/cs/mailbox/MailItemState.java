package com.zimbra.cs.mailbox;

import java.util.HashMap;
import java.util.Map;

import org.redisson.client.RedisException;

import com.google.common.base.Joiner;
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
public class MailItemState {

    private UnderlyingData data;
    private ACL rights;
    private Color color;
    private int metadataVersion = 1;
    private int version = 1;
    private RetentionPolicy retentionPolicy;
    private SharedStateAccessor sharedState;

    private Map<String, ItemField<?>> fields;

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

    public static enum AccessMode { LOCAL_ONLY, REMOTE_ONLY, DEFAULT }

    public MailItemState(UnderlyingData data) {
        this.data = data;
        fields = new HashMap<>();
        initFields();
    }

    public void setSharedStateAccessor(SharedStateAccessor accessor) {
        sharedState = accessor;
    }

    public boolean hasSharedStateAccessor() {
        return sharedState != null;
    }

    public void clearSharedStateAccessor() {
        if (sharedState != null) {
            sharedState.delete();
            sharedState = null;
        }
    }

    public String getName() {
        return getStringField(F_NAME).get();
    }

    public void setName(String name) {
        getField(F_NAME).set(name);
    }

    public String getSubject() {
        return getStringField(F_SUBJECT).get();
    }

    public void setSubject(String subject) {
        getField(F_SUBJECT).set(subject);
    }

    public int getParentId() {
        return getIntField(F_PARENT_ID).get();
    }

    public void setParentId(int parentId) {
        getField(F_PARENT_ID).set(parentId);
    }

    public int getFolderId() {
        return getIntField(F_FOLDER_ID).get();
    }

    public void setFolderId(int folderId) {
        getField(F_FOLDER_ID).set(folderId);
    }


    public int getIndexId() {
        return getIntField(F_INDEX_ID).get();
    }

    public void setIndexId(int indexId) {
        getField(F_INDEX_ID).set(indexId);
    }

    public int getImapId() {
        return getIntField(F_IMAP_ID).get();
    }

    public void setImapId(int imapId) {
        getField(F_IMAP_ID).set(imapId);
    }

    public String getPrevFolders() {
        return getStringField(F_PREV_FOLDERS).get();
    }

    public void setPrevFolders(String prevFolders) {
        getField(F_PREV_FOLDERS).set(prevFolders);
    }

    public String getLocator() {
        return getStringField(F_LOCATOR).get();
    }

    public void setLocator(String locator) {
        getField(F_LOCATOR).set(locator);
    }

    public String getBlobDigest() {
        return getStringField(F_BLOB_DIGEST).get();
    }

    public void setBlobDigest(String digest) {
        getField(F_BLOB_DIGEST).set(digest);
    }

    public int getModMetadata() {
        return getIntField(F_MOD_METADATA).get();
    }

    public void setModMetadata(int modMetadata) {
        getField(F_MOD_METADATA).set(modMetadata);
    }

    public int getModContent() {
        return getIntField(F_MOD_CONTENT).get();
    }

    public void setModContent(int modContent) {
        getField(F_MOD_CONTENT).set(modContent);
    }

    public int getDate() {
        return getIntField(F_DATE).get();
    }

    public void setDate(int date) {
        getField(F_DATE).set(date);
    }

    public int getDateChanged() {
        return getIntField(F_DATE_CHANGED).get();
    }

    public void setDateChanged(int dateChanged) {
        getField(F_DATE_CHANGED).set(dateChanged);
    }

    private ItemField<Integer> flagField() {
        return getIntField(F_FLAGS);
    }

    public int getFlags() {
        return flagField().get();
    }

    public boolean isSet(FlagInfo flag) {
        flagField().refresh();
        return data.isSet(flag);
    }

    public void setFlag(FlagInfo flag) {
        flagField().refresh().set(data.setFlag(flag).getFlags());
    }

    public void setFlag(Flag flag) {
        flagField().refresh().set(data.setFlag(flag).getFlags());
    }

    public void unsetFlag(FlagInfo flag) {
        flagField().refresh().set(data.unsetFlag(flag).getFlags());
    }

    public void unsetFlag(Flag flag) {
        flagField().refresh().set(data.unsetFlag(flag).getFlags());
    }

    public void setFlags(int flags) {
        flagField().set(data.setFlags(flags).getFlags());
    }


    private ItemField<String[]> tagField() {
        return getStringArrField(F_TAGS);
    }

    private ItemField<String[]> smartFolderField() {
        return getStringArrField(F_SMARTFOLDERS);
    }

    public String[] getTags() {
        return tagField().get();
    }

    public void setTags(String[] tags) {
        tagField().set(tags);
    }

    public void setTags(NormalizedTags tags) {
        data.setTags(tags);
        tagField().set(data.getTags(), AccessMode.REMOTE_ONLY);
        smartFolderField().set(data.getSmartFolders(), AccessMode.REMOTE_ONLY);
    }

    public String[] getSmartFolders() {
        return getStringArrField(F_SMARTFOLDERS).get();
    }

    public void setSmartFolders(String[] smartFolders) {
        getField(F_SMARTFOLDERS).set(smartFolders);
    }

    public long getSize() {
        return getLongField(F_SIZE).get();
    }

    public void setSize(long size) {
        getField(F_SIZE).set(size);
    }

    public int getUnreadCount() {
        return getIntField(F_UNREAD_COUNT).get();
    }

    public void setUnreadCount(int unreadCount) {
        getField(F_UNREAD_COUNT).set(unreadCount);
    }

    public Color getColor() {
        ItemField<Color> field = getField(F_COLOR);
        return field.get();
    }

    public void setColor(Color color) {
        setColor(color, AccessMode.DEFAULT);
    }

    public void setColor(Color color, AccessMode setMode) {
        ItemField<Color> field = getField(F_COLOR);
        field.set(color, setMode);
    }

    public ACL getRights() {
        ItemField<ACL> field = getField(F_RIGHTS);
        return field.get();
    }

    public void setRights(ACL rights) {
        setRights(rights, AccessMode.DEFAULT);
    }

    public void setRights(ACL rights, AccessMode setMode) {
         getField(F_RIGHTS).set(rights, setMode);
    }

    public int getVersion() {
        return getIntField(F_VERSION).get();
    }

    public void setVersion(int version) {
        setVersion(version, AccessMode.DEFAULT);
    }

    public void setVersion(int version, AccessMode setMode) {
        getField(F_VERSION).set(version, setMode);
    }

    public void incrementVersion() {
        getField(F_VERSION).set(getVersion() + 1);
    }

    public int getMetadataVersion() {
        return getIntField(F_METADATA_VERSION).get();
    }

    public void setMetadataVersion(int metadataVersion) {
        setMetadataVersion(metadataVersion, AccessMode.DEFAULT);
    }

    public void setMetadataVersion(int metadataVersion, AccessMode setMode) {
        getField(F_METADATA_VERSION).set(metadataVersion, setMode);
    }

    public void incrementMetadataVersion() {
        getField(F_METADATA_VERSION).set(getMetadataVersion() + 1);
    }

    public UnderlyingData getUnderlyingData() {
        return data;
    }

    public void metadataChanged(Mailbox mbox, boolean updateFolderMODSEQ) throws ServiceException {
        data.metadataChanged(mbox, updateFolderMODSEQ);
    }

    public void contentChanged(Mailbox mbox) throws ServiceException {
        data.contentChanged(mbox);
    }

    public void contentChanged(Mailbox mbox, boolean bool) throws ServiceException {
        data.contentChanged(mbox, bool);
    }

    public void saveMetadata(MailItem item, String metadata) throws ServiceException {
        DbMailItem.saveMetadata(item, metadata);
        if (sharedState != null) {
            getField(F_METADATA).set(metadata, AccessMode.REMOTE_ONLY);
        }
    }

    public RetentionPolicy getRetentionPolicy() {
        ItemField<RetentionPolicy> field = getField(F_RETENTION_POLICY);
        return field.get();
    }

    public void setRetentionPolicy(RetentionPolicy policy) {
        setRetentionPolicy(policy, AccessMode.DEFAULT);
    }

    public void setRetentionPolicy(RetentionPolicy policy, AccessMode setMode) {
        getField(F_RETENTION_POLICY).set(policy, setMode);
    }


    protected abstract class ItemField<T> {

        protected String name;

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
            sharedState.set(name, value);
        }

        protected T getSharedState() {
            return sharedState.get(name);
        }

        public T get() {
            if (sharedState != null) {
                try {
                    T sharedValue = getSharedState();
                    //update the local value in case it's stale
                    setLocal(sharedValue);
                    return sharedValue;
                } catch (RedisException e) {
                    ZimbraLog.mailbox.error("unable to get value for field '%s' from redis, using local value", name, e);
                }
            }
            return getLocal();
        }

        protected abstract void setLocal(T value);
        protected abstract T getLocal();

        public void sync() {
            T localValue = getLocal();
            if (sharedState != null && hasData(localValue)) {
                set(localValue, AccessMode.REMOTE_ONLY);
            }
        }

        protected boolean hasData(T localValue) {
            return localValue != null;
        }
    }

    protected <T> void addField(ItemField<T> field) {
        fields.put(field.name, field);
    }

    protected ItemField<String> getStringField(String fieldName) {
        return getField(fieldName);
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
        return (ItemField<T>) fields.get(fieldName);
    }

    public void syncWithSharedState() {
        if (sharedState != null) {
            for (ItemField<?> field: fields.values()) {
                try {
                    field.sync();
                } catch (Exception e) {
                    ZimbraLog.cache.error("unable to serialize field %s", field.name, e);
                }
            }
        }
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
            protected void setLocal(String value) { data.type = Type.of(value).toByte(); }

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
            protected void setLocal(Integer value) { data.parentId = value; }

            @Override
            protected Integer getLocal() { return data.parentId; }
        });

        addField(new ItemField<Integer>(F_FOLDER_ID) {

            @Override
            protected void setLocal(Integer value) { data.folderId = value; }

            @Override
            protected Integer getLocal() { return data.folderId; }
        });

        addField(new ItemField<Integer>(F_INDEX_ID) {

            @Override
            protected void setLocal(Integer value) { data.indexId = value; }

            @Override
            protected Integer getLocal() { return data.indexId; }
        });

        addField(new ItemField<Integer>(F_IMAP_ID) {

            @Override
            protected void setLocal(Integer value) { data.imapId = value; }

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
            protected void setLocal(Integer value) { data.modMetadata = value; }

            @Override
            protected Integer getLocal() { return data.modMetadata; }
        });

        addField(new ItemField<Integer>(F_MOD_CONTENT) {

            @Override
            protected void setLocal(Integer value) { data.modContent = value; }

            @Override
            protected Integer getLocal() { return data.modContent; }
        });

        addField(new ItemField<Integer>(F_DATE) {

            @Override
            protected void setLocal(Integer value) { data.date = value; }

            @Override
            protected Integer getLocal() { return data.date; }
        });

        addField(new ItemField<Integer>(F_DATE_CHANGED) {

            @Override
            protected void setLocal(Integer value) { data.dateChanged = value; }

            @Override
            protected Integer getLocal() { return data.dateChanged; }
        });

        addField(new ItemField<Integer>(F_FLAGS) {

            @Override
            protected void setLocal(Integer value) { data.setFlags(value); }

            @Override
            protected Integer getLocal() { return data.getFlags(); }

            @Override
            public ItemField<Integer> refresh() {
                if (sharedState == null) {
                    return this;
                }
                Integer flags = sharedState.get(F_FLAGS);
                data.setFlags(flags);
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
                sharedState.set(name, Joiner.on(",").join(tags));
            }

            @Override
            protected String[] getSharedState() {
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
                sharedState.set(name, Joiner.on(",").join(smartFolders));
            }

            @Override
            protected String[] getSharedState() {
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
            protected void setLocal(Long value) { data.size = value; }

            @Override
            protected Long getLocal() { return data.size; }
        });

        addField(new ItemField<Integer>(F_UNREAD_COUNT) {

            @Override
            protected void setLocal(Integer value) { data.unreadCount = value; }

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
            protected void setLocal(Integer value) { metadataVersion = value; }

            @Override
            protected Integer getLocal() { return metadataVersion; }
        });

        addField(new ItemField<Integer>(F_VERSION) {

            @Override
            protected void setLocal(Integer value) { version = value; }

            @Override
            protected Integer getLocal() { return MailItemState.this.version; }
        });

        addField(new ItemField<Color>(F_COLOR) {

            @Override
            protected void setLocal(Color value) { color = value; }

            @Override
            protected Color getLocal() { return color; }

            @Override
            protected void setSharedState(Color color) {
                sharedState.set(name, color.toMetadata());
            }

            @Override
            protected Color getSharedState() {
                long colorValue = sharedState.get(name);
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
                if (rights == null) {
                    sharedState.set(name, "");
                } else {
                    sharedState.set(name, rights.encode().toString());
                }
            }

            @Override
            protected ACL getSharedState() {
                String rightsMetaStr = sharedState.get(name);
                if (Strings.isNullOrEmpty(rightsMetaStr)) {
                    return null;
                }
                try {
                    Metadata rightsMeta = new Metadata(rightsMetaStr);
                    return new ACL(rightsMeta);
                } catch (MailServiceException e) {
                    ZimbraLog.cache.error("unable to decode ACL from Redis Live Object", e);
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
                sharedState.set(name, policy == null ? "" : RetentionPolicyManager.toMetadata(policy, true).toString());
            }

            @Override
            protected RetentionPolicy getSharedState() {
                String policyMetaStr = sharedState.get(name);
                if (Strings.isNullOrEmpty(policyMetaStr)) {
                    return new RetentionPolicy();
                }
                try {
                    Metadata policyMeta = new Metadata(policyMetaStr);
                    return RetentionPolicyManager.retentionPolicyFromMetadata(policyMeta, true);
                } catch (ServiceException e) {
                    ZimbraLog.cache.error("unable to decode RetentionPolicy from Redis Live Object", e);
                    return null;
                }
            }
        });
    }
}
