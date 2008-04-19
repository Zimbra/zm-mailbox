package com.zimbra.cs.datasource;

import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.SystemUtil;

import java.io.IOException;

/**
 * Common base class for both ImapImport and Pop3Import.
 */
public abstract class AbstractMailItemImport implements MailItemImport {
    protected final DataSource dataSource;

    protected AbstractMailItemImport(DataSource ds) {
        dataSource = ds;
    }
    
    public synchronized String test() throws ServiceException {
        validateDataSource();
        try {
            connect();
            // disconnect();
        } catch (ServiceException e) {
            Throwable except = SystemUtil.getInnermostException(e);
            if (except == null) except = e;
            ZimbraLog.datasource.info("Error connecting to mail store: ", except);
            return except.toString();
        }
        return null;
    }

    protected abstract void connect() throws ServiceException;

    protected void validateDataSource() throws ServiceException {
        DataSource ds = getDataSource();
        if (ds.getHost() == null) {
            throw ServiceException.FAILURE(ds + ": host not set", null);
        }
        if (ds.getPort() == null) {
            throw ServiceException.FAILURE(ds + ": port not set", null);
        }
        if (ds.getConnectionType() == null) {
            throw ServiceException.FAILURE(ds + ": connectionType not set", null);
        }
        if (ds.getUsername() == null) {
            throw ServiceException.FAILURE(ds + ": username not set", null);
        }
    }

    private static final RuleManager RULE_MANAGER = RuleManager.getInstance();
    
    protected int addMessage(ParsedMessage pm, int folderId, int flags)
        throws ServiceException, IOException {
        Mailbox mbox = dataSource.getMailbox();
        SharedDeliveryContext context = new SharedDeliveryContext();
        Message msg = null;
        if (folderId == Mailbox.ID_FOLDER_INBOX) {
            try {
                msg = RULE_MANAGER.applyRules(mbox.getAccount(), mbox, pm,
                    pm.getRawSize(), dataSource.getEmailAddress(), context);
                if (msg == null) {
                    return 0; // Message was discarded
                }
                // Set flags (setting of BITMASK_UNREAD is implicit)
                if (flags != Flag.BITMASK_UNREAD) {
                    mbox.setTags(null, msg.getId(), MailItem.TYPE_MESSAGE,
                                 flags, MailItem.TAG_UNCHANGED);
                }
            } catch (Exception e) {
                ZimbraLog.datasource.warn("Error applying filter rules", e);
            }
        }
        if (msg == null) {
            msg = mbox.addMessage(null, pm, folderId, false, flags, null);
        }
        return msg.getId();
    }

    public boolean isSslEnabled() {
        return dataSource.getConnectionType() == DataSource.ConnectionType.ssl;
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
}
