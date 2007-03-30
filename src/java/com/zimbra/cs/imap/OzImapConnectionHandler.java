/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ozserver.OzAllMatcher;
import com.zimbra.cs.ozserver.OzByteArrayMatcher;
import com.zimbra.cs.ozserver.OzByteBufferGatherer;
import com.zimbra.cs.ozserver.OzConnection;
import com.zimbra.cs.ozserver.OzConnectionHandler;
import com.zimbra.cs.ozserver.OzCountingMatcher;
import com.zimbra.cs.ozserver.OzMatcher;
import com.zimbra.cs.ozserver.OzServer;
import com.zimbra.cs.ozserver.OzTLSFilter;
import com.zimbra.cs.ozserver.OzUtil;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.Config;

/**
 *
 * @author dkarp
 */
public class OzImapConnectionHandler extends ImapHandler implements OzConnectionHandler {

    public static final String PROPERTY_ALLOW_CLEARTEXT_LOGINS = "imap.allows.cleartext.logins"; 
    public static final String PROPERTY_SECURE_SERVER = "imap.secure.server";

    final OzServer     mServer;
    final OzConnection mConnection;

    private OzByteBufferGatherer mCurrentData;
    private ArrayList<Object> mCurrentRequestData;
    private String mCurrentRequestTag;

    public OzImapConnectionHandler(OzConnection connection) {
        mConnection = connection;
        mServer = connection.getServer();
    }

    @Override
    void dumpState(Writer w) {
    	StringBuilder s = new StringBuilder("\n\tOzImapConnectionHandler ").append(this.toString()).append("\n");
    	if (mConnection == null) {
    		s.append("\t\tCONNECTION IS NULL\n");
    	} else {
    		s.append("\t\t").append(mConnection.toString()).append(" matcher=");
    		OzMatcher matcher = mConnection.getMatcher();
    		s.append(matcher.toString()).append('\n');
    	}

    	s.append("\t\tStartedTLS=").append(mStartedTLS ? "true" : "false").append('\n');
    	s.append("\t\tGoodbyeSent=").append(mGoodbyeSent ? "true" : "false").append('\n');
    	
    	try {
    		w.write(s.toString());
    	} catch(IOException e) {};
    }
    
    void encodeState(Element parent) {
        Element e = parent.addElement("OZIMAP");
        if (mConnection != null)
            e.addElement("connection").setText(mConnection.toString());
        e.addAttribute("startedTls", mStartedTLS);
        e.addAttribute("goodbyeSent", mGoodbyeSent);
    }

    @Override
    Object getServer() {
        return mServer;
    }

    @Override protected boolean setupConnection(Socket connection)  { return true; }
    @Override protected boolean authenticate()                      { return true; }
    @Override protected void notifyIdleConnection()                 { }

    @Override
    protected boolean processCommand() throws IOException {
        OzImapRequest req = new OzImapRequest(mCurrentRequestTag, mCurrentRequestData, this);
        boolean keepGoing = CONTINUE_PROCESSING;
        String logPushedUsername = null;
        try {
            if (mCredentials != null) {
                logPushedUsername = mCredentials.getUsername();
                ZimbraLog.pushbackContext(ZimbraLog.C_NAME, logPushedUsername);
            }

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

            keepGoing = executeRequest(req);
        } catch (ImapParseException ipe) {
            if (ipe.mTag == null)
                sendUntagged("BAD " + ipe.getMessage(), true);
            else if (ipe.mCode != null)
                sendNO(ipe.mTag, '[' + ipe.mCode + "] " + ipe.getMessage());
            else if (ipe.mNO)
                sendNO(ipe.mTag, ipe.getMessage());
            else
                sendBAD(ipe.mTag, ipe.getMessage());
        } finally {
            if (logPushedUsername != null)
                ZimbraLog.popbackContext(ZimbraLog.C_NAME, logPushedUsername);
        }

        return keepGoing;
    }


    @Override
    boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) {
            return CONTINUE_PROCESSING;
        } else if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return CONTINUE_PROCESSING;
        }
        sendOK(tag, "Begin TLS negotiation now");

        boolean debugLogging = LC.nio_imap_debug_logging.booleanValue();
        mConnection.addFilter(new OzTLSFilter(mConnection, debugLogging, ZimbraLog.nio));

        mStartedTLS = true;

        return CONTINUE_PROCESSING;
    }

    @Override
    void disableUnauthConnectionAlarm() throws ServiceException {
        // Session timeout will take care of closing an IMAP connection with no activity.
        ZimbraLog.imap.debug("disabling unauth connection alarm");
        mConnection.cancelAlarm();

        int lcMax = LC.nio_imap_write_queue_max_size.intValue();
        com.zimbra.cs.account.Config config = Provisioning.getInstance().getConfig();            
        int msgMax = config.getIntAttr(Provisioning.A_zimbraMtaMaxMessageSize, lcMax);
        mConnection.setWriteQueueMaxCapacity(Math.max(lcMax, msgMax));
    }

    private class ConnectionOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] ba, int off, int len) throws IOException {
            mConnection.write(ByteBuffer.wrap(ba, off, len));
        }

        @Override
        public void flush() throws IOException {
            mConnection.flush();
        }
    }

    @Override
    OutputStream getFetchOutputStream() {
        return new BufferedOutputStream(new ConnectionOutputStream(), 4000);
    }


    @Override void flushOutput()  { }

    @Override
    void sendLine(String line, boolean flush) throws IOException {
        mConnection.writeAsciiWithCRLF(line);
    }


    private static final int MAX_COMMAND_LENGTH = 20480;

    private OzByteArrayMatcher mCommandMatcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLF, null);
    private OzCountingMatcher mLiteralMatcher = new OzCountingMatcher();

    private enum ConnectionState { READLITERAL, READLINE, CLOSED, UNKNOWN };

    private ConnectionState mState = ConnectionState.UNKNOWN;

    private void gotoReadLineState(boolean newRequest) {
        mState = ConnectionState.READLINE;
        mCommandMatcher.reset();
        mConnection.setMatcher(mCommandMatcher);
        mConnection.enableReadInterest();
        mCurrentData = new OzByteBufferGatherer(256, MAX_COMMAND_LENGTH);
        if (newRequest) {
            mCurrentRequestData = new ArrayList<Object>();
            mCurrentRequestTag = null;
        }

        if (ZimbraLog.nio.isDebugEnabled())
            ZimbraLog.nio.debug("entered command read state");
    }

    private void gotoReadLiteralState(int target) {
        mState = ConnectionState.READLITERAL;
        mLiteralMatcher.target(target);
        mLiteralMatcher.reset();
        mConnection.setMatcher(mLiteralMatcher);
        mConnection.enableReadInterest();
        mCurrentData = new OzByteBufferGatherer(target, target);

        if (ZimbraLog.nio.isDebugEnabled())
            ZimbraLog.nio.debug("entered literal read state target=" + target);
    }

    private Object mCloseLock = new Object();
    
    private void gotoClosedStateInternal(boolean sendBanner) throws IOException {
        synchronized (mCloseLock) {
            // Close only once
            if (mState == ConnectionState.CLOSED)
                throw new IllegalStateException("connection already closed");

            try {
                mConnection.setMatcher(new OzAllMatcher());

                if (mSelectedFolder != null) {
                    mSelectedFolder.setHandler(null);
                    SessionCache.clearSession(mSelectedFolder);
                    mSelectedFolder = null;
                }

                if (sendBanner) {
                    if (!mGoodbyeSent)
                        sendUntagged(ImapServer.getGoodbye(), true);
                    mGoodbyeSent = true;
                }
            } finally {
                mState = ConnectionState.CLOSED;
            }
        }
    }
    
    private void gotoClosedState(boolean sendBanner) {
        try {
            gotoClosedStateInternal(sendBanner);
        } catch (IOException ioe) {
            ZimbraLog.imap.info("exception occurred when closing IMAP connection", ioe);
        } finally {
            mConnection.close();
            if (ZimbraLog.imap.isDebugEnabled()) ZimbraLog.imap.debug("entered closed state: banner=" + sendBanner);
        }
    }

    public void handleDisconnect() {
        assert(mState != ConnectionState.UNKNOWN);
        ZimbraLog.imap.info("connection closed by client");
        try {
            gotoClosedStateInternal(false);
        } catch (IOException ioe) {
            // Can't happen because send banner is false
            ZimbraLog.imap.info("exception occurred when disconnecting IMAP connection", ioe);
        }
    }

    @Override
    public void dropConnection(boolean sendBanner) {
        gotoClosedState(sendBanner);
    }

    public void handleConnect() throws IOException {
        assert(mState == ConnectionState.UNKNOWN);
        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug("dropping connection because user services are disabled");
            gotoClosedState(true);
            return;
        }

        if (mConnection.getProperty(PROPERTY_SECURE_SERVER, null) != null)
            mStartedTLS = true;

        sendUntagged(ImapServer.getBanner(), true);
        gotoReadLineState(true);
    }

    public void handleAlarm() {
        ZimbraLog.imap.info("dropping connection due to unauthenticated idle connection timeout");
        gotoClosedState(true);
    }

    public void handleInput(ByteBuffer buffer, boolean matched) throws IOException {
        if (mState == ConnectionState.CLOSED) {
            ZimbraLog.nio.info(OzUtil.byteBufferDebugDump("input arrived on closed connection", buffer, false));
            mCurrentData.clear();
            return;
        }
                
        if (mSelectedFolder != null)
            mSelectedFolder.updateAccessTime();

        mCurrentData.add(buffer);

        if (!matched)
            return;

        mCurrentData.trim(mConnection.getMatcher().trailingTrimLength());

        if (mState == ConnectionState.READLITERAL) {
            mCurrentRequestData.add(mCurrentData.array());
            /*
			 * at the end of a literal there is more of the command or there is
			 * just CRLF (which is the empty part of the command)
			 */
            gotoReadLineState(false);
            return;
        } else if (mState == ConnectionState.READLINE && mAuthenticator != null) {
            boolean authFinished = true;
            try {
                if (mCurrentData.overflowed()) {
                    sendBAD(mAuthenticator.mTag, "AUTHENTICATE failed: request too long");
                    gotoReadLineState(true);
                    return;
                } else if (mCurrentData.size() == 1 && mCurrentData.get(0) == '*') {
                    // 6.2.2: "If the client wishes to cancel an authentication exchange, it issues a line
                    //         consisting of a single "*".  If the server receives such a response, it MUST
                    //         reject the AUTHENTICATE command by sending a tagged BAD response."
                    sendBAD(mAuthenticator.mTag, "AUTHENTICATE aborted");
                    gotoReadLineState(true);
                    return;
                }

                mCurrentRequestData.add(mCurrentData.toAsciiString());
                OzImapRequest req = new OzImapRequest(mAuthenticator.mTag, mCurrentRequestData, this);
                if (continueAuthentication(req))
                    gotoReadLineState(true);
                else
                    gotoClosedState(true);
                return;
            } finally {
                if (authFinished)
                    mAuthenticator = null;
            }
        } else if (mState == ConnectionState.READLINE) {
            if (mCurrentData.overflowed()) {
                ZimbraLog.imap.info("input request too long");
                sendUntagged("BAD request too long", true);
                gotoReadLineState(true);
                return;
            }
            String line = mCurrentData.toAsciiString();
            /*
             * Is this the first line of this request? If so, then let's try
             * to parse the tag from it so we can report tagged BADs if
             * needed. TODO: should we also strip tag here - so we don't
             * parse it twice?
             */
            if (mCurrentRequestData.size() == 0) {
                logCommand(line);
                try {
                    mCurrentRequestTag = OzImapRequest.readTag(line);
                } catch (ImapParseException ipe) {
                    sendUntagged("BAD " + ipe.getMessage(), true);
                    gotoReadLineState(true);
                    return;
                }
            }

            /* See if there is a literal at the end of this line. */
            ImapLiteral literal;
            try {
                literal = ImapLiteral.parse(mCurrentRequestTag, line, extensionEnabled("LITERAL+"));
            } catch (ImapParseException ipe) {
                sendBAD(mCurrentRequestTag, ipe.getMessage());
                gotoReadLineState(true);
                return;
            }

            /* Add either the literal removed line or a no-literal present at all
             * line (ie, it's line in either case) to the request in flight. */ 
            mCurrentRequestData.add(line);

            if (literal.octets() > 0) {
                if (literal.blocking())
                    sendContinuation();
                gotoReadLiteralState(literal.octets());
                return;
            } else {
                if (processCommand())
                    gotoReadLineState(true);
                else
                    gotoClosedState(true);
                return;
            }
        }

        throw new RuntimeException("internal error in IMAP server: bad state " + mState);
    }

    private void logCommand(String command) {
        if (!ZimbraLog.imap.isDebugEnabled())
            return;

        int space = command.indexOf(' ');
        if ((space + "LOGIN ".length()) < command.length()) {
            String possiblyLogin = command.substring(space + 1, space + 1 + "LOGIN ".length());
            if (possiblyLogin.equalsIgnoreCase("LOGIN ")) {
                int endLogin = space + 1 + "LOGIN ".length();
                int endUser = command.indexOf(' ', endLogin);
                if (endUser > 0) {
                    command = command.substring(0, endUser) + "...";
                }
            }
        }
        ZimbraLog.imap.debug("  C: " + command);
    }
}
