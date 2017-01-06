package com.zimbra.cs.session;

import java.io.InputStream;

import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.account.message.ImapMessageInfo;

public class ModificationItem implements ZimbraMailItem {

    private String acctId;
    private int id;
    private int imapUid;
    private int folderId;
    private int flags;
    private String[] tags;
    private MailItemType type;

    public ModificationItem(ImapMessageInfo msg, String acctId) {
        this.acctId = acctId;
        this.flags = msg.getFlags();
        this.tags = msg.getTags().split(",");
        this.id = msg.getId();
        this.imapUid = msg.getImapUid();
        this.type = MailItem.Type.of(msg.getType()).toCommon();
    }


    @Override
    public String getAccountId() throws ServiceException {
        return acctId;
    }

    @Override
    public int getFlagBitmask() {
        return flags;
    }

    @Override
    public int getFolderIdInMailbox() throws ServiceException {
        return folderId;
    }

    @Override
    public int getIdInMailbox() throws ServiceException {
        return id;
    }

    @Override
    public String[] getTags() {
        return tags;
    }

    @Override
    public int getImapUid() {
        return imapUid;
    }

    @Override
    public MailItemType getMailItemType() {
        return type;
    }

    /**The following interface methods aren't used by the PendingModifications system.
     * May be worthwhile factor the above methods to a super interface to avoid this.
     */
    @Override
    public int getModifiedSequence() {
        return 0;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public InputStream getContentStream() throws ServiceException {
        return null;
    }

    @Override
    public long getDate() {
        return 0;
    }
}
