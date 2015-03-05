package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;

/**
 * Describes a queued indexing tasks
 * @author Greg Solovyev
 *
 */
public class AddToIndexTaskLocator extends AbstractIndexingTasksLocator {
    private final List<MailItemIdentifier> mailItemsToAdd;
    private final boolean isReindex; //set to TRUE for batch re-indexing tasks

    public void addMailItem(MailItem mailItem) {
        mailItemsToAdd.add(new MailItemIdentifier(mailItem.getId(), mailItem.getType(), mailItem.inDumpster()));
    }

    public AddToIndexTaskLocator(MailItem item, String accountID, int mailboxID, int mailboxSchemaGroupId) {
        super(mailboxID, mailboxSchemaGroupId, accountID);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        isReindex = false;
    }
    
    public AddToIndexTaskLocator(MailItem item,  String accountID, int mailboxID, int mailboxSchemaGroupId, boolean indexAttachments) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        isReindex = false;
    }
    
    public AddToIndexTaskLocator(MailItem item,  String accountID, int mailboxID, int mailboxSchemaGroupId, boolean indexAttachments, boolean reIndex) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        isReindex = reIndex;
    }
    
    public AddToIndexTaskLocator(List<MailItem> items, String accountID,  int mailboxID, int mailboxSchemaGroupId, boolean indexAttachments) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        for(MailItem item : items) {
            mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        }
        isReindex = false;
    }
    
    public AddToIndexTaskLocator(List<MailItem> items, String accountID,  int mailboxID, int mailboxSchemaGroupId, boolean indexAttachments, boolean reIndex) {
        super(mailboxID, mailboxSchemaGroupId, accountID, indexAttachments);
        mailItemsToAdd = new ArrayList<MailItemIdentifier>();
        for(MailItem item : items) {
            mailItemsToAdd.add(new MailItemIdentifier(item.getId(), item.getType(), item.inDumpster()));
        }
        isReindex = reIndex;
    }
    
    /**
     * 
     * @return List<MailItemIdentifier> list of items to be indexed
     */
    public List<MailItemIdentifier> getMailItems() {
        return mailItemsToAdd;
    }
    
    /**
     * 
     * @return boolean flag indicating whether this task is part of a re-indexing batch
     */
    public boolean isReindex() {
        return isReindex;
    }
    
    public class MailItemIdentifier {
        private final MailItem.Type type;
        private final int id;
        private final boolean inDumpster;
    
        public MailItemIdentifier(int id) {
            this.id = id;
            this.type = MailItem.Type.UNKNOWN;
            this.inDumpster = false;
        }
        
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
}
