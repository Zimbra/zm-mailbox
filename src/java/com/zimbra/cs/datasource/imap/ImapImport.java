package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.datasource.MailItemImport;
import com.zimbra.cs.datasource.LogOutputStream;
import com.zimbra.cs.datasource.ImapFolder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.db.DbImapFolder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SSLSocketFactoryManager;
import com.zimbra.common.util.ZimbraLog;

import java.io.PrintStream;

public abstract class ImapImport extends MailItemImport {
    private final ImapConnection connection;

    public ImapImport(DataSource ds) throws ServiceException {
        super(ds);
        connection = new ImapConnection(getImapConfig(ds));
    }

    public static ImapConfig getImapConfig(DataSource ds) {
        ImapConfig config = new ImapConfig();
        config.setHost(ds.getHost());
        config.setPort(ds.getPort());
        config.setAuthenticationId(ds.getUsername());
        config.setSecurity(getSecurity(ds.getConnectionType()));
        // bug 37982: Disable use of LITERAL+ due to problems with Yahoo IMAP.
        // Avoiding LITERAL+ also gives servers a chance to reject uploaded
        // messages that are too big, since the server must send a continuation
        // response before the literal data can be sent.
        config.setUseLiteralPlus(false);
        if (ds.isDebugTraceEnabled()) {
            config.setDebug(true);
            config.setTrace(true);
            config.setMaxLiteralTraceSize(
                ds.getIntAttr(Provisioning.A_zimbraDataSourceMaxTraceSize, 0));
            config.setTraceStream(
                new PrintStream(new LogOutputStream(ZimbraLog.imap), true));
        }
        config.setSSLSocketFactory(SSLSocketFactoryManager.getDefaultSSLSocketFactory());
        return config;
    }
}
