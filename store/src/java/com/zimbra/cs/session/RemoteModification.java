package com.zimbra.cs.session;

import java.io.InputStream;

import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.mail.type.ItemSpec;

public class RemoteModification {
    private MailItem.Type type;


    public enum ModificationType {
        creation, deletion, modification;
    }

    public static class ModifiedItem implements ZimbraMailItem {
        public ModifiedItem(ItemSpec itemSpec) {

        }
        public MailboxStore mbox;
        public int flags;
        public int id;
        public int imapUid;
        public int folderId;
        public int modSeq;
        public MailItemType type;
        public String[] tags;

        @Override
        public String getAccountId() throws ServiceException {
            return mbox.getAccountId();
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
        public int getImapUid() {
            return imapUid;
        }

        @Override
        public MailItemType getMailItemType() {
            return type;
        }

        @Override
        public int getModifiedSequence() {
            return modSeq;
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public String[] getTags() {
            return tags;
        }
    }
}
