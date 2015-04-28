package com.zimbra.cs.index;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;




/**
 * Base class for describing queued indexing tasks
 * @author Greg Solovyev
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonSubTypes({  
        @Type(value = DeleteFromIndexTaskLocator.class, name = "DeleteFromIndexTaskLocator"),  
        @Type(value = AddToIndexTaskLocator.class, name = "AddToIndexTaskLocator") })

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
