/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource.imap;

import com.zimbra.common.net.SocketFactories;
import com.zimbra.cs.datasource.MailItemImport;
import com.zimbra.cs.datasource.LogOutputStream;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.common.service.ServiceException;
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
        // config.setSecurity(getSecurity(ds.getConnectionType()));
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
        config.setSocketFactory(SocketFactories.defaultSocketFactory());
        config.setSSLSocketFactory(SocketFactories.defaultSSLSocketFactory());
        return config;
    }
}
