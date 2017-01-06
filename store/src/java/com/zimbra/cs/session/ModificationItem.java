package com.zimbra.cs.session;

import java.io.InputStream;
import java.util.List;

import com.zimbra.common.mailbox.ACLGrant;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.mailbox.ZimbraTag;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.account.message.ImapMessageInfo;

public class ModificationItem implements ZimbraMailItem, FolderStore, ZimbraTag {

    private String acctId;
    private int id;
    private int imapUid;
    private int folderId;
    private int flags;
    private String[] tags;
    private MailItemType type;

    //for folder rename
    private String path;

    //for tag rename
    private int tagId;
    private String tagName;

    public ModificationItem(int tagId, String tagName) {
        this.tagId = tagId;
        this.tagName = tagName;
    }

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

    @Override
    public int getTagId() {
        return tagId;
    }
    @Override
    public String getTagName() {
        return tagName;
    }

    /**TODO: The following interface methods aren't used by the PendingModifications system.
     * We should factor out the above methods to separate interfaces to avoid this.
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
        return null;
    }


    @Override
    public String getFolderIdAsString() {
        return null;
    }

    @Override
    public ItemIdentifier getFolderItemIdentifier() {
        return null;
    }


    @Override
    public int getImapMODSEQ() {
        return 0;
    }


    @Override
    public int getImapMessageCount() {
        return 0;
    }


    @Override
    public int getImapUIDNEXT() {
        return 0;
    }


    @Override
    public int getImapUnreadCount() {
        return 0;
    }


    @Override
    public MailboxStore getMailboxStore() {
        return null;
    }


    @Override
    public String getName() {
        return null;
    }


    @Override
    public int getUIDValidity() {
        return 0;
    }


    @Override
    public boolean hasSubfolders() {
        return false;
    }


    @Override
    public boolean inTrash() {
        return false;
    }


    @Override
    public boolean isChatsFolder() {
        return false;
    }


    @Override
    public boolean isContactsFolder() {
        return false;
    }


    @Override
    public boolean isDeletable() {
        return false;
    }


    @Override
    public boolean isHidden() {
        return false;
    }


    @Override
    public boolean isIMAPSubscribed() {
        return false;
    }


    @Override
    public boolean isInboxFolder() {
        return false;
    }


    @Override
    public boolean isSearchFolder() {
        return false;
    }


    @Override
    public boolean isSyncFolder() {
        return false;
    }


    @Override
    public boolean isVisibleInImap(boolean bool) {
        return false;
    }
}
