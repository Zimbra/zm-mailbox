package com.zimbra.cs.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaIoSessionOutputStream;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.filter.SSLFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.Socket;

public class MinaImapHandler extends ImapHandler implements MinaHandler {
    private MinaImapServer server;    
    private IoSession session;

    MinaImapHandler(MinaImapServer server, IoSession session) {
        this.server = server;
        this.session = session;
    }

    @Override void dumpState(Writer w) {
        try {
            w.write(toString());
        } catch (IOException e) {
        }
    }

    @Override void encodeState(Element parent) {
        Element e = parent.addElement("OZIMAP");
        e.addElement("session").setText(session.toString());
        e.addAttribute("startedTls", mStartedTLS);
        e.addAttribute("goodbyeSent", mGoodbyeSent);
    }

    @Override Object getServer() {
        return server;
    }

    @Override boolean doSTARTTLS(String tag) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED)) return true;
        if (mStartedTLS) {
            sendNO(tag, "TLS already started");
            return true;
        }
        SSLFilter filter = new SSLFilter(MinaImapServer.getSSLContext());
        session.getFilterChain().addFirst("ssl", filter);
        session.setAttribute(SSLFilter.DISABLE_ENCRYPTION_ONCE, true);
        sendOK(tag, "Begin TLS negotiation now");
        mStartedTLS = true;
        return true;
    }

    @Override void disableUnauthConnectionAlarm() throws ServiceException {}

    @Override OutputStream getFetchOutputStream() {
        return new MinaIoSessionOutputStream(session);
    }

    public void connectionOpened() throws IOException {
        if (!Config.userServicesEnabled()) {
            ZimbraLog.imap.debug(
              "Dropping connection (user services are disabled)");
            // TODO Is there a better way of handling this?
            dropConnection();
            return;
        }
        sendUntagged(ImapServer.getBanner(), true);
        mStartedTLS = server.isSSLEnabled();
        session.setIdleTime(IdleStatus.BOTH_IDLE,
            (int) (ImapServer.IMAP_UNAUTHED_CONNECTION_MAX_IDLE_MILLISECONDS / 1000));
    }

    @Override protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }

    public void requestReceived(MinaRequest req) throws IOException {
        assert req instanceof MinaImapRequest;
        if (mCredentials != null) {
            ZimbraLog.addAccountNameToContext(mCredentials.getUsername());
        }
        if (mSelectedFolder != null) {
            ZimbraLog.addMboxToContext(mSelectedFolder.getMailbox().getId());
            mSelectedFolder.updateAccessTime();
        }
        ZimbraLog.addIpToContext(session.getRemoteAddress().toString());
        long start = ZimbraPerf.STOPWATCH_IMAP.start();
        try {
            if (!processRequest((MinaImapRequest) req)) dropConnection();
        } finally {
            ZimbraPerf.STOPWATCH_IMAP.stop(start);
            if (mLastCommand != null) {
                sActivityTracker.addStat(mLastCommand.toUpperCase(), start);
            }
            ZimbraLog.clearContext();
        }
    }

    private boolean processRequest(MinaImapRequest req) throws IOException {
        if (!checkAccountStatus()) return false;
        if (mAuthenticator != null) return authenticate(req);
        try {
            return executeRequest(req);
        } catch (ImapParseException e) {
            handleParseException(e);
            return true;
        }
    }

    private boolean authenticate(MinaImapRequest req) throws IOException {
        boolean cont = continueAuthentication(req);
        if (mAuthenticator == null) {
            // Authentication succeeded
            session.setIdleTime(IdleStatus.BOTH_IDLE,
                (int) (ImapFolder.IMAP_IDLE_TIMEOUT_MSEC / 1000));
        }
        return cont;
    }

    private void handleParseException(ImapParseException e) throws IOException {
        if (e.mTag == null) {
            sendUntagged("BAD " + e.getMessage(), true);
        } else if (e.mCode != null) {
            sendNO(e.mTag, '[' + e.mCode + "] " + e.getMessage());
        } else if (e.mNO) {
            sendNO(e.mTag, e.getMessage());
        } else {
            sendBAD(e.mTag, e.getMessage());
        }
    }

    // TODO Consider moving to ImapHandler base class
    private boolean checkAccountStatus() {
        if (mCredentials == null) return true;
        // Check authenticated user's account status before executing command
        try {
            Account account = mCredentials.getAccount();
            if (account == null || !isAccountStatusActive(account)) {
                ZimbraLog.imap.warn(
                    "account missing or not active; dropping connection");
                return false;
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn(
                "error checking account status; dropping connection", e);
            return false;
        }
        // Check target folder owner's account status before executing command
        if (mSelectedFolder == null) return true;
        String id = mSelectedFolder.getTargetAccountId();
        if (mCredentials.getAccountId().equalsIgnoreCase(id)) return true;
        try {
            Account account =
                Provisioning.getInstance().get(Provisioning.AccountBy.id, id);
            if (account == null || !isAccountStatusActive(account)) {
                ZimbraLog.imap.warn(
                    "target account missing or not active; dropping connection");
                return false;
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn(
                "error checking target account status; dropping connection", e);
            return false;
        }
        return true;
    }

    // TODO Consider adding method to Account base class
    private boolean isAccountStatusActive(Account account) {
        return account.getAccountStatus().equals(
            Provisioning.ACCOUNT_STATUS_ACTIVE);
    }
    
    /**
     * Called when connection is closed. No need to worry about concurrent
     * execution since requests are processed in sequence for any given
     * connection.
     */
    @Override void dropConnection(boolean sendBanner) {
        ZimbraLog.imap.debug("dropConnection: sendBanner = %s\n", sendBanner);
        if (mSelectedFolder != null) {
            mSelectedFolder.setHandler(null);
            SessionCache.clearSession(mSelectedFolder);
            mSelectedFolder = null;
        }
        if (session == null) return; // Already closed...
        try {
            if (sendBanner && !mGoodbyeSent) {
                sendUntagged(ImapServer.getGoodbye(), true);
                mGoodbyeSent = true;
            }
            session.close();
            session = null;
        } catch (IOException e) {
            info("exception while closing connection", e);
        }
    }

    public void connectionClosed() {
        dropConnection();
    }

    public void connectionIdle() {
        notifyIdleConnection();
    }
    
    @Override protected boolean setupConnection(Socket connection) {
        throw new UnsupportedOperationException();
    }
    
    @Override protected boolean authenticate() {
        throw new UnsupportedOperationException();
    }

    @Override protected void notifyIdleConnection() {
        ZimbraLog.imap.debug("dropping connection for inactivity");
        dropConnection();
    }

    @Override void flushOutput() {
        // TODO Do we need to do anything here?
    }
    
    @Override void sendLine(String line, boolean flush) throws IOException {
        if (session == null) throw new IOException("Stream is closed");
        session.write(line + "\r\n");
        if (flush) flushOutput();
    }

    private void info(String msg, Throwable e) {
        if (!ZimbraLog.imap.isInfoEnabled()) return;
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(session.getRemoteAddress()).append("] ")
          .append(msg);
        if (e != null) {
            ZimbraLog.imap.info(sb.toString(), e);
        } else {
            ZimbraLog.imap.info(sb.toString());
        }
    }

    private void info(String msg) {
        info(msg, null);
    }
}
