package com.zimbra.cs.mailbox.cache;

import com.google.common.base.Joiner;
import com.zimbra.cs.mailbox.MailItem.IndexStatus;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public abstract class ItemFieldProvider {

    private  String tags;
    private int parentId = -1;
    private int folderId = -1;
    private String prevFolders;
    private Integer indexId  = IndexStatus.NO.id();
    private int imapId   = -1;
    private long size;
    private int unreadCount;
    private int flags;
    private String smartFolders;
    private String metadata;
    private int dateChanged;
    private int modMetadata;
    public int modContent;

    public abstract int getParentId();

    public abstract void setParentId(int parentId);

    public abstract int getFolderId();

    public abstract void setFolderId(int folderId);

    public abstract String getPrevFolders();

    public abstract void setPrevFolders(String prevFolders);

    public abstract int getIndexId();

    public abstract void setIndexId(Integer indexId);

    public abstract int getImapId();

    public abstract void setImapId(int imapId);

    public abstract int getFlags();

    public abstract void setFlags(int flags);

    public abstract String getSmartFolders();

    public abstract void setSmartFolders(String smartFolders);

    public abstract String getMetadata();

    public abstract void setMetadata(String metadata);

    public abstract int getDateChanged();

    public abstract void setDateChanged(int dateChanged);

    public abstract int getModMetadata();

    public abstract void setModMetadata(int modMetadata);

    public abstract int getModContent();

    public abstract void setModContent(int modContent);

    public ItemFieldProvider(String accountId, UnderlyingData data) {
        tags = Joiner.on(",").join(data.getTags());
        flags = data.getFlags();
        smartFolders = Joiner.on(",").join(data.getSmartFolders());
        prevFolders = data.getPrevFolders();
        parentId = data.parentId;
        folderId = data.folderId;
        indexId = data.indexId;
        imapId = data.imapId;
        size = data.size;
        metadata = data.metadata;
        dateChanged = data.dateChanged;
    }

    public abstract long getSize();

    public abstract void setSize(long size);

    public abstract String getTags();

    public abstract void setTags(String tags);

    public abstract void setUnreadCount(int unread);

    public abstract int getUnreadCount();
}
