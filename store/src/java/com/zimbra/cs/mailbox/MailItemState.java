package com.zimbra.cs.mailbox;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Tag.NormalizedTags;
import com.zimbra.soap.mail.type.RetentionPolicy;

public interface MailItemState {

    public static enum AccessMode { LOCAL_ONLY, REMOTE_ONLY, DEFAULT }

    public String getName();

    public void setName(String name);

    public String getSubject();

    public void setSubject(String subject);

    public int getParentId();

    public void setParentId(int parentId);

    public int getFolderId();

    public void setFolderId(int folderId);

    public int getIndexId();

    public void setIndexId(int indexId);

    public int getImapId();

    public void setImapId(int imapId);

    public String getPrevFolders();

    public void setPrevFolders(String prevFolders);

    public String getLocator();

    public void setLocator(String locator);

    public String getBlobDigest();

    public void setBlobDigest(String digest);

    public int getModMetadata();

    public void setModMetadata(int modMetadata);

    public int getModContent();

    public void setModContent(int modContent);

    public int getDate();

    public void setDate(int date);

    public int getDateChanged();

    public void setDateChanged(int dateChanged);

    public int getFlags();

    public boolean isSet(FlagInfo flag);

    public void setFlag(FlagInfo flag);

    public void setFlag(Flag flag);

    public void unsetFlag(FlagInfo flag);

    public void unsetFlag(Flag flag);

    public void setFlags(int flags);

    public String[] getTags();

    public void setTags(String[] tags);

    public void setTags(NormalizedTags tags);

    public String[] getSmartFolders();

    public void setSmartFolders(String[] smartFolders);

    public long getSize();

    public void setSize(long size);

    public int getUnreadCount();

    public void setUnreadCount(int unreadCount);

    public Color getColor();

    public void setColor(Color color);

    public void setColor(Color color, AccessMode setMode);

    public ACL getRights();

    public void setRights(ACL rights);

    public void setRights(ACL rights, AccessMode setMode);

    public int getVersion();

    public void setVersion(int version);

    public void setVersion(int version, AccessMode setMode);

    public void incrementVersion();

    public int getMetadataVersion();

    public void setMetadataVersion(int metadataVersion);

    public void setMetadataVersion(int metadataVersion, AccessMode setMode);

    public void incrementMetadataVersion();

    public UnderlyingData getUnderlyingData();

    public void metadataChanged(Mailbox mbox, boolean updateFolderMODSEQ) throws ServiceException;

    public void contentChanged(Mailbox mbox) throws ServiceException;

    public void contentChanged(Mailbox mbox, boolean updateFolderMODSEQ) throws ServiceException;

    public void saveMetadata(MailItem item, String metadata) throws ServiceException;

    public RetentionPolicy getRetentionPolicy();

    public void setRetentionPolicy(RetentionPolicy policy);

    public void setRetentionPolicy(RetentionPolicy policy, AccessMode setMode);
}
