package com.zimbra.cs.mailbox;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Tag.NormalizedTags;
import com.zimbra.soap.mail.type.RetentionPolicy;

public class LocalMailItemState implements IMailItemState {

    protected final UnderlyingData data;
    private ACL rights;
    private Color color;
    private int metadataVersion = 1;
    private int version = 1;
    private RetentionPolicy retentionPolicy;

    public LocalMailItemState(UnderlyingData data) {
        this.data = data;
    }

    @Override
    public String getName() {
        return data.name;
    }

    @Override
    public void setName(String name) {
        data.name = name;

    }

    @Override
    public String getSubject() {
        return data.getSubject();
    }

    @Override
    public void setSubject(String subject) {
        data.setSubject(subject);
    }

    @Override
    public int getParentId() {
        return data.parentId;
    }

    @Override
    public void setParentId(int parentId) {
        data.parentId = parentId;
    }

    @Override
    public int getFolderId() {
        return data.folderId;
    }

    @Override
    public void setFolderId(int folderId) {
        data.folderId = folderId;

    }

    @Override
    public int getIndexId() {
        return data.indexId;
    }

    @Override
    public void setIndexId(int indexId) {
        data.indexId = indexId;
    }

    @Override
    public int getImapId() {
        return data.imapId;
    }

    @Override
    public void setImapId(int imapId) {
        data.imapId = imapId;
    }

    @Override
    public String getPrevFolders() {
        return data.getPrevFolders();
    }

    @Override
    public void setPrevFolders(String prevFolders) {
        data.setPrevFolders(prevFolders);
    }

    @Override
    public String getLocator() {
        return data.locator;
    }

    @Override
    public void setLocator(String locator) {
        data.locator = locator;
    }

    @Override
    public String getBlobDigest() {
        return data.getBlobDigest();
    }

    @Override
    public void setBlobDigest(String digest) {
        data.setBlobDigest(digest);
    }

    @Override
    public int getModMetadata() {
        return data.modMetadata;
    }

    @Override
    public void setModMetadata(int modMetadata) {
        data.modMetadata = modMetadata;
    }

    @Override
    public int getModContent() {
        return data.modContent;
    }

    @Override
    public void setModContent(int modContent) {
        data.modContent = modContent;
    }

    @Override
    public int getDate() {
        return data.date;
    }

    @Override
    public void setDate(int date) {
        data.date = date;
    }

    @Override
    public int getDateChanged() {
        return data.dateChanged;
    }

    @Override
    public void setDateChanged(int dateChanged) {
        data.dateChanged = dateChanged;
    }

    @Override
    public int getFlags() {
        return data.getFlags();
    }

    @Override
    public boolean isSet(FlagInfo flag) {
        return data.isSet(flag);
    }

    @Override
    public void setFlag(FlagInfo flag) {
        data.setFlag(flag);
    }

    @Override
    public void setFlag(Flag flag) {
        data.setFlag(flag);
    }

    @Override
    public void unsetFlag(FlagInfo flag) {
        data.unsetFlag(flag);
    }

    @Override
    public void unsetFlag(Flag flag) {
        data.unsetFlag(flag);
    }

    @Override
    public void setFlags(int flags) {
        data.setFlags(flags);
    }

    @Override
    public String[] getTags() {
        return data.getTags();
    }

    @Override
    public void setTags(String[] tags) {
        data.setTags(tags);
    }

    @Override
    public void setTags(NormalizedTags tags) {
        data.setTags(tags);
    }

    @Override
    public String[] getSmartFolders() {
        return data.getSmartFolders();
    }

    @Override
    public void setSmartFolders(String[] smartFolders) {
        data.setSmartFolders(smartFolders);
    }

    @Override
    public long getSize() {
        return data.size;
    }

    @Override
    public void setSize(long size) {
        data.size = size;
    }

    @Override
    public int getUnreadCount() {
        return data.unreadCount;
    }

    @Override
    public void setUnreadCount(int unreadCount) {
        data.unreadCount = unreadCount;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void setColor(Color color, AccessMode setMode) {
        this.color = color;
    }

    @Override
    public ACL getRights() {
        return rights;
    }

    @Override
    public void setRights(ACL rights) {
        this.rights = rights;
    }

    @Override
    public void setRights(ACL rights, AccessMode setMode) {
        this.rights = rights;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public void setVersion(int version, AccessMode setMode) {
        this.version = version;
    }

    @Override
    public void incrementVersion() {
        this.version++;
    }

    @Override
    public int getMetadataVersion() {
        return metadataVersion;
    }

    @Override
    public void setMetadataVersion(int metadataVersion) {
        this.metadataVersion = metadataVersion;

    }

    @Override
    public void setMetadataVersion(int metadataVersion, AccessMode setMode) {
        this.metadataVersion = metadataVersion;
    }

    @Override
    public void incrementMetadataVersion() {
        this.metadataVersion++;

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
    public void contentChanged(Mailbox mbox, boolean updateFolderMODSEQ) throws ServiceException {
        data.contentChanged(mbox, updateFolderMODSEQ);
    }

    @Override
    public void saveMetadata(MailItem item, String metadata) throws ServiceException {
        DbMailItem.saveMetadata(item, metadata);
    }

    @Override
    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    @Override
    public void setRetentionPolicy(RetentionPolicy policy) {
        this.retentionPolicy = policy;

    }

    @Override
    public void setRetentionPolicy(RetentionPolicy policy, AccessMode setMode) {
        this.retentionPolicy = policy;
    }
}
