package com.zimbra.cs.datasource;

import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.account.DataSource;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.CustomSSLSocketFactory;

import java.io.IOException;

/**
 */
public class NewImapImport extends AbstractMailItemImport {
    private final ImapConnection connection;

    public NewImapImport(DataSource ds) {
        super(ds);
        connection = new ImapConnection(getImapConfig(ds));
    }

    private static ImapConfig getImapConfig(DataSource ds) {
        ImapConfig config = new ImapConfig();
        config.setHost(ds.getHost());
        config.setPort(ds.getPort());
        config.setAuthenticationId(ds.getUsername());
        config.setMaxLiteralMemSize(LC.data_source_max_message_memory_size.intValue());
        config.setTlsEnabled(LC.javamail_imap_enable_starttls.booleanValue());
        config.setSslEnabled(ds.isSslEnabled());
        if (LC.data_source_trust_self_signed_certs.booleanValue()) {
            config.setSSLSocketFactory(new DummySSLSocketFactory());
        } else {
            config.setSSLSocketFactory(new CustomSSLSocketFactory());
        }
        return config;
    }
    
    protected void connect() throws ServiceException {
        if (connection.isConnected()) return;
        try {
            connection.connect();
            connection.login(dataSource.getDecryptedPassword());
        } catch (IOException e) {
            throw ServiceException.FAILURE(
                "Unable to connect to IMAP server: " + dataSource, e);
        }
    }

    public void importData(boolean fullSync) throws ServiceException {
        validateDataSource();
        connect();
    }
}
