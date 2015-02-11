package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;

/**
 *
 * @author Greg Solovyev
 *
 */
public class IndexingQueueItemLocator {
    public class MailItemIdentifier {
        private final MailItem.Type type;
        private final int id;
        private final boolean inDumpster;
        public MailItemIdentifier(int id, MailItem.Type type, boolean inDumpster) {
            this.id = id;
            this.type = type;
            this.inDumpster = inDumpster;
        }
        
        public MailItemIdentifier(MailItem item) {
            this(item.getId(),item.getType(),item.inDumpster());
        }
        
        public int getId() {
            return id;
        }
        
        public MailItem.Type getType() {
            return type;
        }
        
        public boolean isInDumpster() {
            return inDumpster;
        }
    }
    private final int mailboxID;
    private final int mailboxSchemaGroupID;
    private final List<MailItemIdentifier> mailItems;
    private final String accountID;
    private final boolean indexAttachments;

    public int getMailboxID() {
        return mailboxID;
    }

    public List<MailItemIdentifier> getMailItems() {
        return mailItems;
    }

    public String getAccountID() {
        return accountID;
    }

    public int getMailboxSchemaGroupID() {
        return mailboxSchemaGroupID;
    }

    public boolean attachmentIndexingEnabled() {
        return indexAttachments;
    }

    public void addMailItem(MailItem mailItem) {
        mailItems.add(new MailItemIdentifier(mailItem.getId(), mailItem.getType(), mailItem.inDumpster()));
    }
    
    public IndexingQueueItemLocator(int mbxId, int mailboxSchemaGroupId, MailItem item, String accountId,  boolean attachments) {
        mailboxID = mbxId;
        mailboxSchemaGroupID = mailboxSchemaGroupId;
        mailItems = new ArrayList<MailItemIdentifier>();
        mailItems.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        accountID = accountId;
        indexAttachments = attachments;
    }
    
    public IndexingQueueItemLocator(int mbxId, int mailboxSchemaGroupId, List<MailItem> items, String accountId, boolean attachments) {
        mailboxID = mbxId;
        mailboxSchemaGroupID = mailboxSchemaGroupId;
        mailItems = new ArrayList<MailItemIdentifier>();
        for(MailItem item : items) {
            mailItems.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        }
        accountID = accountId;
        indexAttachments = attachments;
    }
    
    public IndexingQueueItemLocator(List<MailItem> items, String accountId, int mbxId, int mailboxSchemaGroupId, boolean attachments) {
        mailboxID = mbxId;
        mailboxSchemaGroupID = mailboxSchemaGroupId;
        mailItems = new ArrayList<MailItemIdentifier>();
        for(MailItem item : items) {
            mailItems.add(new MailItemIdentifier(item));
        }
        accountID = accountId;
        indexAttachments = attachments;
    }
}
