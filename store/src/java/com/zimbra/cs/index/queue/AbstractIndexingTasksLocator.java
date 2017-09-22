package com.zimbra.cs.index.queue;


/**
 * Base class for describing queued indexing tasks
 * @author Greg Solovyev
 *
 */
public abstract class AbstractIndexingTasksLocator {
    protected int mailboxID;
    protected int mailboxSchemaGroupID;
    protected String accountID;
    protected boolean indexAttachments;
    private int retries = 0;

    protected AbstractIndexingTasksLocator (int mailboxID, int mailboxSchemaGroupID, String accountID) {
        this.mailboxID = mailboxID;
        this.mailboxSchemaGroupID = mailboxSchemaGroupID;
        this.accountID = accountID;
        this.indexAttachments = false;
    }

    protected AbstractIndexingTasksLocator (int mailboxID, int mailboxSchemaGroupID, String accountID,  boolean indexAttachments) {
        this.mailboxID = mailboxID;
        this.mailboxSchemaGroupID = mailboxSchemaGroupID;
        this.accountID = accountID;
        this.indexAttachments = indexAttachments;
    }


    public int getMailboxID() {
        return mailboxID;
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

    public void addRetry() {
        retries++;
    }

    public int getRetries() {
        return retries;
    }
}
