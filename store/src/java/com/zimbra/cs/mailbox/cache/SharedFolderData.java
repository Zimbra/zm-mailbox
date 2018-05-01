package com.zimbra.cs.mailbox.cache;

import org.redisson.api.annotation.REntity;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

@REntity
public class SharedFolderData extends SharedItemData {

    private long totalSize;
    private int imapUIDNEXT;
    private int imapMODSEQ;
    private int imapRECENT;
    private int imapRECENTCutoff;
    private int deletedCount;
    private int deletedUnreadCount;

    public SharedFolderData() {}

    public SharedFolderData(String id) {
        super(id);
    }

    public SharedFolderData(String accountId, UnderlyingData data) {
        super(accountId, data);
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public UnderlyingData toUnderlyingData() {
        return super.toUnderlyingData(Type.FOLDER);
    }

    public int getImapUIDNEXT() {
        return imapUIDNEXT;
    }

    public void setImapUIDNEXT(int imapUIDNEXT) {
        this.imapUIDNEXT = imapUIDNEXT;
    }

    public int getImapMODSEQ() {
        return imapMODSEQ;
    }

    public void setImapMODSEQ(int imapMODSEQ) {
        this.imapMODSEQ = imapMODSEQ;
    }

    public int getImapRECENT() {
        return imapRECENT;
    }

    public void setImapRECENT(int imapRECENT) {
        this.imapRECENT = imapRECENT;
    }

    public int getImapRECENTCutoff() {
        return imapRECENTCutoff;
    }

    public void setImapRECENTCutoff(int imapRECENTCutoff) {
        this.imapRECENTCutoff = imapRECENTCutoff;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public int getDeletedUnreadCount() {
        return deletedUnreadCount;
    }

    public void setDeletedUnreadCount(int deletedUnreadCount) {
        this.deletedUnreadCount = deletedUnreadCount;
    }
}
