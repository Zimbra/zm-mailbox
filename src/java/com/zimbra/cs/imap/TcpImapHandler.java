/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import com.zimbra.common.util.Constants;
import com.zimbra.common.util.NetUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.TcpServerInputStream;
import com.zimbra.cs.util.Config;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

class TcpImapHandler extends ImapHandler {
    private TcpServerInputStream mInputStream;
    private String         mRemoteAddress;
    private TcpImapRequest mRequest;

    TcpImapHandler(ImapServer server) {
        super(server);
    }
                                                      
    @Override protected boolean setupConnection(Socket connection) throws IOException {
        connection.setSoTimeout(mConfig.getMaxIdleSeconds() * 1000);        
        mRemoteAddress = connection.getInetAddress().getHostAddress();
        INFO("connected");

        mInputStream = new TcpServerInputStream(connection.getInputStream());
        mOutputStream = new BufferedOutputStream(connection.getOutputStream());

        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug("dropping connection because user services are disabled");
            dropConnection();
            return false;
        }

        sendGreeting();

        return true;
    }

    @Override protected boolean authenticate() {
        // we auth with the LOGIN command (and more to come)
        return true;
    }

    @Override protected void setIdle(boolean idle) {
        super.setIdle(idle);
        ImapFolder i4selected = mSelectedFolder;
        if (i4selected != null)
            i4selected.updateAccessTime();
    }

    @Override protected boolean processCommand() throws IOException {
        // FIXME: throw an exception instead?
        if (mInputStream == null)
            return STOP_PROCESSING;
        
        setUpLogContext(mRemoteAddress);

        if (mRequest == null)
            mRequest = new TcpImapRequest(mInputStream, this);

        try {
            mRequest.continuation();
            if (mRequest.isMaxRequestSizeExceeded()) {
                setIdle(false); // FIXME Why for only this error?
                throw new ImapParseException(
                    mRequest.getTag(), "maximum request size exceeded", false);
            }

            long start = ZimbraPerf.STOPWATCH_IMAP.start();
            // check account status before executing command
            if (!checkAccountStatus()) {
                return STOP_PROCESSING;
            }
            boolean keepGoing;
            if (mAuthenticator != null && !mAuthenticator.isComplete())
                keepGoing = continueAuthentication(mRequest);
            else
                keepGoing = executeRequest(mRequest);
            // FIXME Shouldn't we do these before executing the request??
            setIdle(false);
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (mLastCommand != null)
                ZimbraPerf.IMAP_TRACKER.addStat(mLastCommand.toUpperCase(), start);
            clearRequest();
            return keepGoing;
        } catch (TcpImapRequest.ImapContinuationException ice) {
            mRequest.rewind();
            if (ice.sendContinuation)
                sendContinuation("send literal data");
            return CONTINUE_PROCESSING;
        } catch (TcpImapRequest.ImapTerminatedException ite) {
            return STOP_PROCESSING;
        } catch (ImapParseException ipe) {
            clearRequest();
            handleParseException(ipe);
            return CONTINUE_PROCESSING;
        } finally {
            ZimbraLog.clearContext();
        }
    }

    private void clearRequest() {
        if (mRequest != null) {
            mRequest.cleanup();
            mRequest = null;
        }
    }
    
    @Override boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) {
            return CONTINUE_PROCESSING;
        } else if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return CONTINUE_PROCESSING;
        }
        sendOK(tag, "Begin TLS negotiation now");

        SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket tlsconn = (SSLSocket) fac.createSocket(mConnection, mConnection.getInetAddress().getHostName(), mConnection.getPort(), true);
        NetUtil.setSSLEnabledCipherSuites(tlsconn, mConfig.getSslExcludedCiphers());
        tlsconn.setUseClientMode(false);
        tlsconn.startHandshake();
        ZimbraLog.imap.debug("suite: " + tlsconn.getSession().getCipherSuite());
        mInputStream = new TcpServerInputStream(tlsconn.getInputStream());
        mOutputStream = new BufferedOutputStream(tlsconn.getOutputStream());
        mStartedTLS = true;

        return CONTINUE_PROCESSING;
    }
    
    @Override protected void dropConnection(boolean sendBanner) {
        clearRequest();
        try {
            unsetSelectedFolder(false);
        } catch (Exception e) { }

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

        ZimbraLog.addIpToContext(mRemoteAddress);
        try {
            OutputStream os = mOutputStream;
            if (os != null) {
                if (sendBanner && !mGoodbyeSent) {
                    sendBYE();
                }
                os.close();
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
            if (ZimbraLog.imap.isDebugEnabled()) {
                ZimbraLog.imap.info("I/O error while closing connection", e);
            } else {
                ZimbraLog.imap.info("I/O error while closing connection: " + e);
            }
        } finally {
            ZimbraLog.clearContext();
        }
    }

    @Override protected void notifyIdleConnection() {
        // we can, and do, drop idle connections after the timeout

        // TODO in the TcpServer case, is this duplicated effort with
        // session timeout code that also drops connections?
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    @Override protected void completeAuthentication() throws IOException {
        mAuthenticator.sendSuccess();
        if (mAuthenticator.isEncryptionEnabled()) {
            // switch to encrypted streams
            mInputStream = new TcpServerInputStream(mAuthenticator.unwrap(mConnection.getInputStream()));
            mOutputStream = mAuthenticator.wrap(mConnection.getOutputStream());
        }
    }
    
    @Override protected void enableInactivityTimer() throws SocketException {
        mConnection.setSoTimeout(mConfig.getMaxIdleSeconds() * 1000);
    }

    @Override protected void flushOutput() throws IOException {
        mOutputStream.flush();
    }

    @Override void sendLine(String line, boolean flush) throws IOException {
        OutputStream os = mOutputStream;
        if (os == null)
            return;
        os.write(line.getBytes());
        os.write(LINE_SEPARATOR_BYTES);
        if (flush)
            os.flush();
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
}
