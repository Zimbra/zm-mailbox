/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DataSource.ConnectionType;
import com.zimbra.cs.datasource.LogOutputStream;
import com.zimbra.cs.datasource.MessageContent;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailConfig;
import com.zimbra.cs.mailclient.MailConfig.Security;
import com.zimbra.cs.mailclient.auth.Authenticator;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.DataHandler;
import com.zimbra.cs.mailclient.imap.IDInfo;
import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.ResponseHandler;
import com.zimbra.cs.util.Zimbra;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class ConnectionManager {
    private Map<String, ImapConnection> connections =
        Collections.synchronizedMap(new HashMap<String, ImapConnection>());

    private static final ConnectionManager INSTANCE = new ConnectionManager();

    private static final boolean REUSE_CONNECTIONS =
        LC.data_source_imap_reuse_connections.booleanValue();

    private static final int IDLE_READ_TIMEOUT = 30 * 60; // 30 minutes

    private static final Log LOG = ZimbraLog.datasource;
    
    public static ConnectionManager getInstance() {
        return INSTANCE;
    }
    
    private ConnectionManager() {}

    /**
     * Opens a new IMAP connection or reuses an existing one if available
     * for specified data source. If a new connection is required the optional
     * authenticator, if specified, will be used with AUTHENTICATE, otherwise
     * the LOGIN command will be used. If a suspended connection is reused
     * and has been idling, then IDLE will be automatically terminated.
     *
     * @param ds the data source for the connection
     * @param auth optional authenticator, or null to use LOGIN
     * @return the IMAP connection to use
     * @throws ServiceException if an I/O or auth error occurred
     */
    public ImapConnection openConnection(DataSource ds, Authenticator auth)
        throws ServiceException {
        ImapConnection ic = connections.remove(ds.getId());
        if (ic == null || !resumeConnection(ic)) {
            ic = newConnection(ds, auth);
        }
        ic.getImapConfig().setMaxLiteralMemSize(ds.getMaxTraceSize());
        ic.setTraceEnabled(ds.isDebugTraceEnabled());
        return ic;
    }

    public ImapConnection openConnection(DataSource ds) throws ServiceException {
        return openConnection(ds, null);
    }

    /**
     * Releases existing connection so that it can be reused again. If the
     * IMAP server supports IDLE then the connection will go into IDLE state.
     * Otherwise, the connection will be kept active for as long as possible.
     *
     * @param ds the data source for the connection
     * @param ic the connection to release
     */
    public void releaseConnection(DataSource ds, ImapConnection ic) {
        LOG.debug("Releasing connection: " + ic);
        if (isReuseConnections(ds) && suspendConnection(ds, ic)) {
            if (connections.put(ds.getId(), ic) != null) {
                throw new AssertionError();
            }
        } else {
            LOG.debug("Closing connection: " + ic);
            ic.close();
        }
    }

    public void closeConnection(DataSource ds) {
        closeConnection(ds.getId());
    }

    /**
     * Closes any suspended connection associated with specified data source.
     * This must be called whenever data source is modified or deleted in order
     * to force a reconnect upon next use.
     *
     * @param dataSourceId the data source id for the connection
     */
    public void closeConnection(String dataSourceId) {
        ImapConnection ic = connections.remove(dataSourceId);
        if (ic != null) {
            LOG.debug("Closing connection: " + ic);
            ic.close();
        }
    }

    private boolean isReuseConnections(DataSource ds) {
        return ds.isOffline() && REUSE_CONNECTIONS;
    }
    
    private static ImapConnection newConnection(DataSource ds, Authenticator auth)
        throws ServiceException {
        ImapConnection ic = new ImapConnection(newImapConfig(ds));
        ic.setDataHandler(new FetchDataHandler());
        try {
            ic.connect();
            try {
                if (auth != null) {
                    ic.authenticate(auth);
                } else {
                    ic.login(ds.getDecryptedPassword());
                }
            } catch (CommandFailedException e) {
                throw new LoginException(e.getError());
            }
            if (isImportingSelf(ds, ic)) {
                throw ServiceException.INVALID_REQUEST(
                    "User attempted to import messages from his/her own mailbox", null);
            }
        } catch (ServiceException e) {
            ic.close();
            throw e;
        } catch (Exception e) {
            ic.close();
            throw ServiceException.FAILURE(
                "Unable to connect to IMAP server: " + ds, e);
        }
        LOG.debug("Created new connection: " + ic);
        return ic;
    }

    private static boolean isImportingSelf(DataSource ds, ImapConnection ic)
        throws IOException, ServiceException {
        if (!ds.isOffline() && ic.hasCapability(ImapCapabilities.ID)) {
            try {
                IDInfo id = ic.id();
                if ("Zimbra".equalsIgnoreCase(id.getName())) {
                    String user = id.get("user");
                    return user != null && user.equals(ds.getAccount().getName());
                }
            } catch (CommandFailedException e) {
                // Skip check if ID command fails
            }
        }
        return false;
    }

    // Handler for fetched message data which uses ParsedMessage to stream
    // the message data to disk if necessary.
    private static class FetchDataHandler implements DataHandler {
        public Object handleData(ImapData data) throws Exception {
            try {
                return MessageContent.read(data.getInputStream(), data.getSize());
            } catch (OutOfMemoryError e) {
                Zimbra.halt("Out of memory");
                return null;
            }
        }
    }
    
    private static ImapConfig newImapConfig(DataSource ds) {
        ImapConfig config = new ImapConfig();
        config.setHost(ds.getHost());
        config.setPort(ds.getPort());
        config.setAuthenticationId(ds.getUsername());
        config.setSecurity(getSecurity(ds));
        // bug 37982: Disable use of LITERAL+ due to problems with Yahoo IMAP.
        // Avoiding LITERAL+ also gives servers a chance to reject uploaded
        // messages that are too big, since the server must send a continuation
        // response before the literal data can be sent.
        config.setUseLiteralPlus(false);
        // Enable support for trace output
        config.setTrace(true);
        config.setTraceStream(ds.isOffline() ?
            new PrintStream(new LogOutputStream(ZimbraLog.imap), true) : System.out);
        config.setSocketFactory(SocketFactories.defaultSocketFactory());
        config.setSSLSocketFactory(SocketFactories.defaultSSLSocketFactory());
        config.setConnectTimeout(ds.getConnectTimeout(LC.javamail_imap_timeout.intValue()));
        config.setReadTimeout(ds.getReadTimeout(LC.javamail_imap_timeout.intValue()));
        LOG.debug("Connect timeout = %d, read timeout = %d",
                  config.getConnectTimeout(), config.getReadTimeout());
        return config;
    }


    private static MailConfig.Security getSecurity(DataSource ds) {
        ConnectionType type = ds.getConnectionType();
        if (type == null) {
            type = ConnectionType.cleartext;
        }
        switch (type) {
        case cleartext:
            // bug 44439: For ZCS import, if connection type is 'cleartext' we
            // still use localconfig property to determine if we should try
            // TLS. This maintains compatibility with 5.0.x since there is
            // still no UI setting to explicitly enable TLS. For desktop
            // this forced a plaintext connection since we have the UI options.
            return !ds.isOffline() && LC.javamail_imap_enable_starttls.booleanValue() ?
                Security.TLS_IF_AVAILABLE : Security.NONE;
        case ssl:
            return Security.SSL;
        case tls:
            return Security.TLS;
        case tls_if_available:
            return Security.TLS_IF_AVAILABLE;
        default:
            return Security.NONE;
        }
    }

    private static boolean suspendConnection(DataSource ds, ImapConnection ic) {
        // If IDLE supported then IDLE connection, otherwise just let it sit
        if (ic.isClosed()) {
            return false;
        }
        try {
            ic.setReadTimeout(IDLE_READ_TIMEOUT);
            if (ic.hasIdle()) {
                if (!ic.isSelected("INBOX")) {
                    ic.select("INBOX");
                }
                ic.idle(idleHandler(ds));
            } else if (ic.hasUnselect()) {
                ic.unselect();
            } else {
                ic.close_mailbox();
            }
            LOG.debug("Suspended connection: " + ic);
        } catch (IOException e) {
            LOG.warn("Error suspending connection", e);
        }
        return true;     
    }

    private static ResponseHandler idleHandler(final DataSource ds) {
        return new ResponseHandler() {
            public void handleResponse(ImapResponse res) throws Exception {
                if (res.getCCode() == CAtom.EXISTS) {
                    SyncState ss = SyncStateManager.getInstance().getOrCreateSyncState(ds);
                    if (ss != null) {
                        ss.setHasRemoteInboxChanges(true);
                    }
                }
            }
        };
    }

    private static boolean resumeConnection(ImapConnection ic) {
        if (ic.isClosed()) {
            return false;
        }
        try {
            ic.setReadTimeout(ic.getImapConfig().getReadTimeout());
            if (ic.isIdling()) {
                ic.stopIdle();
            } else {
                ic.noop();
            }
            LOG.debug("Resumed connection: " + ic);
        } catch (IOException e) {
            LOG.warn("Error resuming connection: " + ic, e);
            return false;
        }
        return true;
    }
    
}
