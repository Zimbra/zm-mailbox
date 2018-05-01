package com.zimbra.cs.mailbox.cache;

import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RFieldAccessor;
import org.redisson.api.annotation.RId;

import com.google.common.base.Joiner;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Tag.NormalizedTags;

/**
 * Class encompassing mutable MailItem field that need to be synchronized
 * across mailbox workers using Redis Live Objects
 * @author iraykin
 *
 */
@REntity
public class SharedItemData {

    @RId
    private String id;
    private String uuid;
    private String name;
    private String subject;
    private  String tags;
    private String smartFolders;
    private int flags;
    private int parentId;
    private int folderId;
    private int imapId;
    private int indexId;
    private long size;
    private int unreadCount;
    private int date;
    private int dateChanged;
    private int modMetadata;
    private int modContent;
    private String metadata;
    private String prevFolders;
    private String locator;
    private String blobDigest;

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getPrevFolders() {
        return prevFolders;
    }

    public void setPrevFolders(String prevFolders) {
        this.prevFolders = prevFolders;
    }

    public Integer getIndexId() {
        return indexId;
    }

    public void setIndexId(Integer indexId) {
        this.indexId = indexId;
    }

    public int getImapId() {
        return imapId;
    }

    public void setImapId(int imapId) {
        this.imapId = imapId;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getSmartFolders() {
        return smartFolders;
    }

    public void setSmartFolders(String smartFolders) {
        this.smartFolders = smartFolders;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public int getDateChanged() {
        return dateChanged;
    }

    public void setDateChanged(int dateChanged) {
        this.dateChanged = dateChanged;
    }

    public int getModMetadata() {
        return modMetadata;
    }

    public void setModMetadata(int modMetadata) {
        this.modMetadata = modMetadata;
    }

    public int getModContent() {
        return modContent;
    }

    public void setModContent(int modContent) {
        this.modContent = modContent;
    }

    public SharedItemData() {}

    public SharedItemData(String id) {
        this.id = id;
    }

    public SharedItemData(String accountId, UnderlyingData data) {
        this(accountId + ":" + String.valueOf(data.id));
        uuid = data.uuid;
        name = data.name;
        subject = data.getSubject();
        tags = Joiner.on(",").join(data.getTags());
        smartFolders = Joiner.on(",").join(data.getSmartFolders());
        flags = data.getFlags();
        parentId = data.parentId;
        folderId = data.folderId;
        imapId = data.imapId;
        indexId = data.indexId;
        size = data.size;
        unreadCount = data.unreadCount;
        date = data.date;
        dateChanged = data.dateChanged;
        modMetadata = data.modMetadata;
        modContent = data.modContent;
        metadata = data.metadata;
        prevFolders = data.getPrevFolders();
        locator = data.locator;
        blobDigest = data.getBlobDigest();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @RFieldAccessor
    public <T> void set(String field, T value) {
    }

    @RFieldAccessor
    public <T> T get(String field) {
        return null;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags){
        this.tags = tags;
    }

    public void setUnreadCount(int unread) {
        this.unreadCount = unread;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public String getLocator() {
        return locator;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public String getBlobDigest() {
        return blobDigest;
    }

    public void setBlobDigest(String blobDigest) {
        this.blobDigest = blobDigest;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public UnderlyingData toUnderlyingData(Type type) {
        UnderlyingData data = new UnderlyingData();
        data.type = type.toByte();
        data.id = Integer.valueOf(getId().split(":")[1]); //TODO: proper id handling
        data.uuid = getUuid();
        data.name = getName();
        data.setSubject(getSubject());
        data.setTags(getTags().split(","));
        data.setSmartFolders(getSmartFolders().split(","));
        data.setFlags(getFlags());
        data.parentId = getParentId();
        data.folderId = getFolderId();
        data.imapId = getImapId();
        data.indexId = getIndexId();
        data.size = getSize();
        data.unreadCount = getUnreadCount();
        data.date = getDate();
        data.dateChanged = getDateChanged();
        data.modMetadata = getModMetadata();
        data.modContent = getModContent();
        data.metadata = getMetadata();
        data.setPrevFolders(getPrevFolders());
        data.locator = getLocator();
        data.setBlobDigest(getBlobDigest());
        return data;
    }

    public static enum ItemField {

        PARENT_ID("parentId"),
        FOLDER_ID("folderId"),
        INDEX_ID("indexId"),
        IMAP_ID("imapId"),
        TAGS("tags"),
        FLAGS("flags"),
        SMARTFOLDERS("smartFolders"),
        METADATA("metadata"),
        DATE_CHANGED("dateChanged"),
        UNREAD_COUNT("unreadCount"),
        SIZE("size"),
        PREV_FOLDERS("prevFolders");

        private String fieldName;

        private ItemField(String fieldName) {
            this.fieldName = fieldName;
        }

        public String field() {
            return fieldName;
        }
    }
}
