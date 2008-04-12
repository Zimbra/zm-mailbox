package com.zimbra.cs.datasource;

import com.zimbra.cs.account.DataSource;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.SystemUtil;

import javax.mail.Store;
import javax.mail.MessagingException;

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

    protected void connect() throws ServiceException  {
        Store store = getStore();
        if (!store.isConnected()) {
            DataSource ds = getDataSource();
            try {
                store.connect(ds.getHost(), ds.getPort(), ds.getUsername(),
                              ds.getDecryptedPassword());
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Unable to connect to mail store: " + ds, e);
            }
        }
    }

    protected void disconnect() throws ServiceException {
        Store store = getStore();
        if (store.isConnected()) {
            DataSource ds = getDataSource();
            try {
                store.close();
            } catch (MessagingException e) {
                ZimbraLog.datasource.warn("Unable to disconnect from mail store: " + ds);
            }
        }
    }
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

    public DataSource getDataSource() {
        return dataSource;
    }

    public abstract Store getStore();
}
