package com.zimbra.cs.index.history;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbSearchHistory;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

public class DbSavedSearchPromptLog extends SavedSearchPromptLog {

    private Mailbox mbox;
    private DbSearchHistory db;

    public DbSavedSearchPromptLog(Account acct) throws ServiceException {
        this.mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        this.db = new DbSearchHistory(mbox);
    }

    @Override
    public SavedSearchStatus getSavedSearchStatus(String searchString) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        return db.getSavedSearchStatus(conn, searchString);
    }

    @Override
    public void setPromptStatus(String searchString, SavedSearchStatus status) throws ServiceException {
        DbConnection conn = mbox.getOperationConnection();
        db.setSavedSearchStatus(conn, searchString, status);
    }

}
