package com.zimbra.cs.session;

import java.io.InputStream;
import java.util.List;

import com.zimbra.common.mailbox.ACLGrant;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.account.message.ImapMessageInfo;

public class ModificationItem implements ZimbraMailItem, FolderStore {

    private String acctId;
    private int id;
    private int imapUid;
    private int folderId;
    private int flags;
    private String[] tags;
    private MailItemType type;

    //for folder rename
    private String path;

    public ModificationItem(int id, String path, String acctId) {
        this.id = id;
        this.acctId = acctId;
        this.path = path;
    }
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

    @Override
    public String getPath() {
        return path;
    }


    @Override
    public int getFolderIdInOwnerMailbox() {
        return id;
    }

    /**TODO: The following interface methods aren't used by the PendingModifications system.
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


    @Override
    public List<ACLGrant> getACLGrants() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getFolderIdAsString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ItemIdentifier getFolderItemIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getImapMODSEQ() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getImapMessageCount() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getImapUIDNEXT() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getImapUnreadCount() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public MailboxStore getMailboxStore() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getUIDValidity() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public boolean hasSubfolders() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean inTrash() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isChatsFolder() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isContactsFolder() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isDeletable() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isHidden() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isIMAPSubscribed() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isInboxFolder() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isSearchFolder() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isSyncFolder() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isVisibleInImap(boolean bool) {
        // TODO Auto-generated method stub
        return false;
    }
}
