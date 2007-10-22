/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.TcpServerInputStream;
import com.zimbra.cs.util.Config;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TcpImapHandler extends ImapHandler {
    private Socket mConnection;
    private TcpServerInputStream mInputStream;
    private String         mRemoteAddress;
    private TcpImapRequest mIncompleteRequest = null;

    TcpImapHandler(ImapServer server) {
        super(server);
    }
                                                      
    @Override
    protected boolean setupConnection(Socket connection) throws IOException {
        mConnection = connection;
        mRemoteAddress = connection.getInetAddress().getHostAddress();
        INFO("connected");

        mInputStream = new TcpServerInputStream(connection.getInputStream());
        mOutputStream = new BufferedOutputStream(connection.getOutputStream());
        mStartedTLS = mConfig.isSSLEnabled();

        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug("dropping connection because user services are disabled");
            dropConnection();
            return false;
        }

        sendUntagged(mConfig.getBanner(), true);
        return true;
    }

    @Override
    protected boolean authenticate() {
        // we auth with the LOGIN command (and more to come)
        return true;
    }

    @Override
    protected void setIdle(boolean idle) {
        super.setIdle(idle);
        if (mSelectedFolder != null)
            mSelectedFolder.updateAccessTime();
    }

    @Override
    protected boolean processCommand() throws IOException {
        TcpImapRequest req = null;
        boolean keepGoing = CONTINUE_PROCESSING;
        ZimbraLog.clearContext();

        try {
            // FIXME: throw an exception instead?
            if (mInputStream == null)
                return STOP_PROCESSING;

            if (mCredentials != null)
                ZimbraLog.addAccountNameToContext(mCredentials.getUsername());
            if (mSelectedFolder != null)
                ZimbraLog.addMboxToContext(mSelectedFolder.getMailbox().getId());
            ZimbraLog.addIpToContext(mRemoteAddress);
            
            String origRemoteIp = getOrigRemoteIpAddr();
            if (origRemoteIp != null)
                ZimbraLog.addOrigIpToContext(origRemoteIp);
            
            req = mIncompleteRequest;
            if (req == null)
                req = new TcpImapRequest(mInputStream, this);
            req.continuation();
            
            if (req.isMaxRequestSizeExceeded()) {
                req.sendNO("[TOOBIG] request too long");
                setIdle(false);
                mIncompleteRequest = null;
                return CONTINUE_PROCESSING;
            }

            long start = ZimbraPerf.STOPWATCH_IMAP.start();

            // check account status before executing command
            if (mCredentials != null) {
                try {
                    Account account = mCredentials.getAccount();
                    if (account == null || !account.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE)) {
                        ZimbraLog.imap.warn("account missing or not active; dropping connection");
                        return STOP_PROCESSING;
                    }
                } catch (ServiceException e) {
                    ZimbraLog.imap.warn("error checking account status; dropping connection", e);
                    return STOP_PROCESSING;
                }
            }

            // check target folder's aowner's account status before executing command
            if (mSelectedFolder != null && !mCredentials.getAccountId().equalsIgnoreCase(mSelectedFolder.getTargetAccountId())) {
                try {
                    Account account = Provisioning.getInstance().get(Provisioning.AccountBy.id, mSelectedFolder.getTargetAccountId());
                    if (account == null || !account.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE)) {
                        ZimbraLog.imap.warn("target account missing or not active; dropping connection");
                        return STOP_PROCESSING;
                    }
                } catch (ServiceException e) {
                    ZimbraLog.imap.warn("error checking target account status; dropping connection", e);
                    return STOP_PROCESSING;
                }
            }

            if (mAuthenticator != null && !mAuthenticator.isComplete())
                keepGoing = continueAuthentication(req);
            else
                keepGoing = executeRequest(req);
            setIdle(false);
            mIncompleteRequest = null;

            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (mLastCommand != null) {
                sActivityTracker.addStat(mLastCommand.toUpperCase(), start);
            }
        } catch (TcpImapRequest.ImapContinuationException ice) {
            mIncompleteRequest = req.rewind();
            if (ice.sendContinuation)
                sendContinuation("send literal data");
        } catch (TcpImapRequest.ImapTerminatedException ite) {
            mIncompleteRequest = null;
            keepGoing = STOP_PROCESSING;
        } catch (ImapParseException ipe) {
            mIncompleteRequest = null;
            handleImapParseException(ipe);
        } finally {
            ZimbraLog.clearContext();
        }

        return keepGoing;
    }

    @Override
    boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED))
            return CONTINUE_PROCESSING;
        else if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return CONTINUE_PROCESSING;
        }
        sendOK(tag, "Begin TLS negotiation now");

        SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket tlsconn = (SSLSocket) fac.createSocket(mConnection, mConnection.getInetAddress().getHostName(), mConnection.getPort(), true);
        tlsconn.setUseClientMode(false);
        tlsconn.startHandshake();
        ZimbraLog.imap.debug("suite: " + tlsconn.getSession().getCipherSuite());
        mInputStream = new TcpServerInputStream(tlsconn.getInputStream());
        mOutputStream = new BufferedOutputStream(tlsconn.getOutputStream());
        mStartedTLS = true;

        return CONTINUE_PROCESSING;
    }

    @Override
    public void dropConnection() {
        dropConnection(true);
    }

    @Override
    protected void dropConnection(boolean sendBanner) {
        if (mSelectedFolder != null) {
            mSelectedFolder.setHandler(null);
            SessionCache.clearSession(mSelectedFolder);
            mSelectedFolder = null;
        }

        // wait at most 10 seconds for the untagged BYE to be sent, then force the stream closed
        new Thread() {
            @Override public void run() {
                if (mOutputStream == null)
                    return;

                try {
                    sleep(10 * Constants.MILLIS_PER_SECOND);
                } catch (InterruptedException ie) { }

                OutputStream os = mOutputStream;
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ioe) { }
                }
            }
        }.start();

        try {
            if (mOutputStream != null) {
                if (sendBanner) {
                    if (!mGoodbyeSent) {
                        try {
                            sendUntagged(mConfig.getGoodbye(), true);
                        } catch (IOException ioe) { }
                    }
                    mGoodbyeSent = true;
                }
                mOutputStream.close();
                mOutputStream = null;
            }
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mAuthenticator != null) {
                mAuthenticator.dispose();
                mAuthenticator = null;
            }
        } catch (IOException e) {
            INFO("exception while closing connection", e);
        }
    }

    @Override
    protected void notifyIdleConnection() {
        // we can, and do, drop idle connections after the timeout

        // TODO in the TcpServer case, is this duplicated effort with
        // session timeout code that also drops connections?
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    @Override
    protected void completeAuthentication() throws IOException {
        sendCapability();
        mAuthenticator.sendSuccess();
        if (mAuthenticator.isEncryptionEnabled()) {
            // Switch to encrypted streams
            mInputStream = new TcpServerInputStream(
                mAuthenticator.unwrap(mConnection.getInputStream()));
            mOutputStream = mAuthenticator.wrap(mConnection.getOutputStream());
        }
    }
    
    @Override
    protected void enableInactivityTimer() {
        // Already enabled when connection was established.
        // TODO Fix watchdog to allow this timeout to be changed dynamically.
    }

    @Override
    protected void flushOutput() throws IOException {
        mOutputStream.flush();
    }

    void sendLine(String line, boolean flush) throws IOException {
        // FIXME: throw an exception instead?
        if (mOutputStream == null)
            return;
        mOutputStream.write(line.getBytes());
        mOutputStream.write(LINE_SEPARATOR_BYTES);
        if (flush)
            mOutputStream.flush();
    }

    void INFO(String message, Throwable e) {
        if (ZimbraLog.imap.isInfoEnabled())
            ZimbraLog.imap.info(withClientInfo(message), e); 
    }

    void INFO(String message) {
        if (ZimbraLog.imap.isInfoEnabled())
            ZimbraLog.imap.info(withClientInfo(message));
    }

    private StringBuilder withClientInfo(String message) {
        int length = 64;
        if (message != null)
            length += message.length();
        return new StringBuilder(length).append("[").append(mRemoteAddress).append("] ").append(message);
    }


    public static void main(String[] args) throws IOException, ImapParseException {
        List<ImapPartSpecifier> parts = new ArrayList<ImapPartSpecifier>();
        List<String> pieces = new ArrayList<String>();
        Set<String> patterns = new HashSet<String>();
        TcpImapHandler handler = new TcpImapHandler(null);
        handler.mOutputStream = System.out;

        System.out.println("> A001 CAPABILITY");
        handler.doCAPABILITY("A001");

        System.out.println("> A002 LOGIN \"user1@example.zimbra.com\" \"test123\"");
        handler.doLOGIN("A002", "user1@example.zimbra.com", "test123");

        System.out.println("> B002 ID NIL");
        handler.doID("B002", null);

        System.out.println("> A003 LIST \"\" \"\"");
        patterns.clear();  patterns.add("");
        handler.doLIST("A003", "", patterns, (byte) 0, (byte) 0);

        System.out.println("> B003 CREATE \"/test/slap\"");
        handler.doCREATE("B003", new ImapPath("/test/slap", null));

        System.out.println("> A004 LIST \"/\" \"%\"");
        patterns.clear();  patterns.add("[^/]*");
        handler.doLIST("A004", "/", patterns, (byte) 0, (byte) 0);

        System.out.println("> B004 DELETE \"/test/slap\"");
        handler.doDELETE("B004", new ImapPath("/test/slap", null));

        System.out.println("> A005 LIST \"/\" \"*\"");
        patterns.clear();  patterns.add(".*");
        handler.doLIST("A005", "/", patterns, (byte) 0, (byte) 0);

        System.out.println("> B005 LIST \"/\" \"inbox\"");
        patterns.clear();  patterns.add("INBOX");
        handler.doLIST("B005", "/", patterns, (byte) 0, (byte) 0);

        System.out.println("> C005 LIST \"/\" \"$NBOX+?\"");
        patterns.clear();  patterns.add("\\$NBOX\\+\\?");
        handler.doLIST("C005", "/", patterns, (byte) 0, (byte) 0);

        System.out.println("> D005 LIST \"/\" \"%/sub()\"");
        patterns.clear();  patterns.add("[^/]*/SUB\\(\\)");
        handler.doLIST("D005", "/", patterns, (byte) 0, (byte) 0);

        System.out.println("> A006 SELECT \"/floo\"");
        handler.doSELECT("A006", new ImapPath("/floo", null), (byte) 0);

        System.out.println("> B006 SELECT \"/INBOX\"");
        handler.doSELECT("B006", new ImapPath("/INBOX", null), (byte) 0);

        System.out.println("> A007 STATUS \"/Sent\" (UNSEEN UIDVALIDITY MESSAGES)");
        handler.doSTATUS("A007", new ImapPath("/Sent", null), STATUS_UNSEEN | STATUS_UIDVALIDITY | STATUS_MESSAGES);

        System.out.println("> B007 STATUS \"/INBOX\" (UNSEEN UIDVALIDITY MESSAGES)");
        handler.doSTATUS("B007", new ImapPath("/INBOX", null), STATUS_UNSEEN | STATUS_UIDVALIDITY | STATUS_MESSAGES);

        System.out.println("> A008 FETCH 1:3,*:1234 FULL");
        handler.doFETCH("A008", "1:3,*:1234", FETCH_FULL, parts, -1, false);

        System.out.println("> A009 UID FETCH 444,288,602:593 FULL");
        handler.doFETCH("A009", "444,288,602:593", FETCH_FULL, parts, -1, true);

        System.out.println("> A010 FETCH 7 (ENVELOPE BODY.PEEK[1] BODY[HEADER.FIELDS (DATE SUBJECT)]");
        List<String> headers = new LinkedList<String>();  headers.add("date");  headers.add("subject");
        parts.clear();  parts.add(new ImapPartSpecifier("BODY", "1", ""));  parts.add(new ImapPartSpecifier("BODY", "", "HEADER.FIELDS").setHeaders(headers));
        handler.doFETCH("A010", "7", FETCH_ENVELOPE, parts, -1, false);

        System.out.println("> A011 STORE 1 +FLAGS ($MDNSent)");
        List<String> flags = new ArrayList<String>();  flags.add("$MDNSENT");
        handler.doSTORE("A011", "1", flags, StoreAction.ADD, false, -1, false);

        ImapRequest req = new TcpImapRequest("X001 LOGIN user1@example.zimbra.com \"\\\\\\\"test123\\\"\\\\\"", handler);
        pieces.clear();  pieces.add(req.readTag());  req.skipSpace();  pieces.add(req.readATOM());  req.skipSpace();  pieces.add(req.readAstring());  req.skipSpace();  pieces.add(req.readAstring());  assert(req.eof());
        System.out.println(pieces);

        req = new TcpImapRequest("X002 CREATE ~peter/mail/&U,BTFw-/&ZeVnLIqe-", handler);
        pieces.clear();  pieces.add(req.readTag());  req.skipSpace();  pieces.add(req.readATOM());  req.skipSpace();  pieces.add(req.readFolder());  assert(req.eof());
        System.out.println(pieces);
    }
}
