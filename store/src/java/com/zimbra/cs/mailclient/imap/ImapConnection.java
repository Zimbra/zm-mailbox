/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.login.LoginException;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.MailOutputStream;
import com.zimbra.cs.mailclient.ParseException;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.util.Ascii;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;

public final class ImapConnection extends MailConnection {
    private ImapCapabilities capabilities;
    private MailboxInfo mailbox;
    private ImapRequest request;
    private DataHandler dataHandler;
    private Character delimiter;

    private final AtomicInteger tagCount = new AtomicInteger();

    private static final String TAG_FORMAT = "C%02d";

    public ImapConnection(ImapConfig config) {
        super(config);
    }

    public void setDataHandler(DataHandler handler) {
        dataHandler = handler;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    @Override
    protected MailInputStream newMailInputStream(InputStream is) {
        if (getLogger().isTraceEnabled()) {
            return new ImapInputStream(is, this, getLogger());
        } else {
            return new ImapInputStream(is, this);
        }
    }

    @Override
    protected MailOutputStream newMailOutputStream(OutputStream os) {
        if (getLogger().isTraceEnabled()) {
            return new ImapOutputStream(os, getLogger());
        } else {
            return new ImapOutputStream(os);
        }
    }

    @Override
    protected void processGreeting() throws IOException {
        ImapResponse res = readResponse();
        if (res.isUntagged()) {
            greeting = res.getResponseText().getText();
            switch (res.getCCode()) {
            case BYE:
                throw new MailException(greeting);
            case PREAUTH:
            case OK:
                setState(res.isOK() ?
                    State.NOT_AUTHENTICATED : State.AUTHENTICATED);
                ResponseText rt = res.getResponseText();
                if (CAtom.CAPABILITY.atom().equals(rt.getCode())) {
                    capabilities = (ImapCapabilities) rt.getData();
                } else {
                    capability();
                }
                return;
            }
        }
        throw new MailException("Expected server greeting but got: " + res);
    }

    @Override
    protected void sendLogin(String user, String pass) throws IOException {
        newRequest(CAtom.LOGIN, ImapData.asAString(user), ImapData.asAString(pass)).sendCheckStatus();
    }

    @Override
    public synchronized void logout() throws IOException {
        if (isShutdown()) return;
        if (request != null) {
            throw new IllegalStateException("Request pending");
        }
        setState(State.LOGOUT);
        try {
            newRequest(CAtom.LOGOUT).sendCheckStatus();
        } catch (CommandFailedException e) {
            getLogger().warn("Logout failed, force closing connection", e);
            close();
        }
    }

    @Override
    protected void sendAuthenticate(boolean ir) throws IOException {
        ImapRequest req = newRequest(
            CAtom.AUTHENTICATE, authenticator.getMechanism());
        if (ir) {
            byte[] response = authenticator.getInitialResponse();
            req.addParam(Ascii.toString(Base64.encodeBase64(response)));
        }
        req.sendCheckStatus();
    }

    @Override
    protected void sendStartTls() throws IOException {
        newRequest(CAtom.STARTTLS).sendCheckStatus();
    }

    public ImapCapabilities capability() throws IOException {
        newRequest(CAtom.CAPABILITY).sendCheckStatus();
        return capabilities;
    }

    public void noop() throws IOException {
        newRequest(CAtom.NOOP).sendCheckStatus();
    }

    public void noop(ResponseHandler handler) throws IOException {
        ImapRequest req = newRequest(CAtom.NOOP);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    public void idle(ResponseHandler handler) throws IOException {
        ImapRequest req = newRequest(CAtom.IDLE);
        req.setResponseHandler(handler);
        ImapResponse res = req.send();
        if (res != null) {
            if (res.isOK()) {
                throw new MailException(
                    "Expected IDLE continuation but got final response");
            }
            req.checkStatus(res);
        }
    }

    public void check() throws IOException {
        newRequest(CAtom.CHECK).sendCheckStatus();
    }

    public void xatom(String cmd, Object... params) throws IOException {
        newRequest(cmd, params).sendCheckStatus();
    }

    public IDInfo id(IDInfo info) throws IOException {
        ImapRequest req = newRequest(CAtom.ID, info == null ? CAtom.NIL : info);
        List<IDInfo> results = new ArrayList<IDInfo>(1);
        req.setResponseHandler(new BasicResponseHandler(CAtom.ID, results));
        req.sendCheckStatus();
        return results.isEmpty() ? new IDInfo() : results.get(0);
    }

    public synchronized boolean isSelected(String name) {
        return mailbox != null && mailbox.getName().equals(name);
    }

    public synchronized MailboxInfo select(String name) throws IOException {
        mailbox = doSelectOrExamine(CAtom.SELECT, name);
        setState(State.SELECTED);
        return getMailboxInfo();
    }

    public MailboxInfo examine(String name) throws IOException {
        return doSelectOrExamine(CAtom.EXAMINE, name);
    }

    private MailboxInfo doSelectOrExamine(CAtom cmd, String name) throws IOException {
        MailboxInfo mbox = new MailboxInfo(name);
        ImapRequest req = newRequest(cmd, new MailboxName(name));
        req.setResponseHandler(mbox);
        mbox.handleResponse(req.sendCheckStatus());
        return mbox;
    }

    public void create(String name) throws IOException {
        newRequest(CAtom.CREATE, new MailboxName(name)).sendCheckStatus();
    }

    /** See https://tools.ietf.org/html/rfc4314 IMAP4 Access Control List (ACL) Extension */
    public void setacl(String name, String who, String caps) throws IOException {
        newRequest(CAtom.SETACL, new MailboxName(name), who, caps).sendCheckStatus();
    }

    public void delete(String name) throws IOException {
        newRequest(CAtom.DELETE, new MailboxName(name)).sendCheckStatus();
    }

    public void rename(String from, String to) throws IOException {
        newRequest(CAtom.RENAME, new MailboxName(from),
                new MailboxName(to)).sendCheckStatus();
    }

    public void subscribe(String name) throws IOException {
        newRequest(CAtom.SUBSCRIBE, new MailboxName(name)).sendCheckStatus();
    }

    public void unsubscribe(String name) throws IOException {
        newRequest(CAtom.UNSUBSCRIBE, new MailboxName(name)).sendCheckStatus();
    }

    public AppendResult append(String mbox, Flags flags, Date date, Literal data)
        throws IOException {
        return append(mbox, new AppendMessage(flags, date, data));
    }

    public AppendResult append(String mbox, AppendMessage... msgs) throws IOException {
        return append(mbox, Arrays.asList(msgs));
    }

    public AppendResult append(String mbox, Collection<AppendMessage> msgs)
        throws IOException {
        ImapRequest req = newRequest(CAtom.APPEND, new MailboxName(mbox));
        for (AppendMessage msg : msgs) {
            req.addParam(msg);
        }
        ImapResponse res = req.sendCheckStatus();
        ResponseText rt = res.getResponseText();
        return rt.getCCode() == CAtom.APPENDUID ? (AppendResult) rt.getData() : null;
    }

    public void expunge() throws IOException {
        newRequest(CAtom.EXPUNGE).sendCheckStatus();
    }

    public void uidExpunge(String seq) throws IOException {
        newUidRequest(CAtom.EXPUNGE, seq).sendCheckStatus();
    }

    public synchronized void close_mailbox() throws IOException {
        newRequest(CAtom.CLOSE).sendCheckStatus();
        mailbox = null;
        setState(State.AUTHENTICATED);
    }

    public synchronized void unselect() throws IOException {
        newRequest(CAtom.UNSELECT).sendCheckStatus();
        mailbox = null;
        setState(State.AUTHENTICATED);
    }

    public MailboxInfo status(String name, Object... params) throws IOException {
        ImapRequest req = newRequest(CAtom.STATUS, new MailboxName(name), params);
        List<MailboxInfo> results = new ArrayList<MailboxInfo>(1);
        req.setResponseHandler(new BasicResponseHandler(CAtom.STATUS, results));
        req.sendCheckStatus();
        if (results.isEmpty()) {
            throw new MailException("Missing STATUS response data");
        }
        return results.get(0);
    }

    public List<ListData> list(String ref, String mbox) throws IOException {
        return doList(CAtom.LIST, ref, mbox);
    }

    public List<ListData> lsub(String ref, String mbox) throws IOException {
        return doList(CAtom.LSUB, ref, mbox);
    }

    public boolean exists(String mbox) throws IOException {
        return !list("", mbox).isEmpty();
    }

    private List<ListData> doList(CAtom cmd, String ref, String mbox)
        throws IOException {
        ImapRequest req = newRequest(cmd, new MailboxName(ref), new MailboxName(mbox));
        List<ListData> results = new ArrayList<ListData>();
        req.setResponseHandler(new BasicResponseHandler(cmd, results));
        req.sendCheckStatus();
        return results;
    }

    public char getDelimiter() throws IOException {
        if (delimiter == null) {
            List<ListData> ld = list("", "");
            delimiter = ld.isEmpty() ? 0 : ld.get(0).getDelimiter();
        }
        return delimiter;
    }

    public CopyResult copy(String seq, String mbox) throws IOException {
        ImapRequest req = newRequest(CAtom.COPY, seq, new MailboxName(mbox));
        ResponseText rt = req.sendCheckStatus().getResponseText();
        return rt.getCCode() == CAtom.COPYUID ? (CopyResult) rt.getData() : null;
    }

    public CopyResult uidCopy(String seq, String mbox) throws IOException {
        ImapRequest req = newUidRequest(CAtom.COPY, seq, new MailboxName(mbox));
        ResponseText rt = req.sendCheckStatus().getResponseText();
        return rt.getCCode() == CAtom.COPYUID ? (CopyResult) rt.getData() : null;
    }

    public void fetch(String seq, Object param, ResponseHandler handler)
        throws IOException {
        fetch(CAtom.FETCH.name(), seq, param, handler);
    }

    public void uidFetch(String seq, Object param, ResponseHandler handler)
        throws IOException {
        ImapRequest req = newUidRequest(CAtom.FETCH, seq, param);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    private void fetch(String cmd, String seq, Object param,
                       ResponseHandler handler) throws IOException {
        ImapRequest req = newRequest(cmd, seq, param);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    public List<Long> getUids(String seq) throws IOException {
        final List<Long> uids = new ArrayList<Long>();
        uidFetch(seq, "UID", new FetchResponseHandler() {
            @Override
            public void handleFetchResponse(MessageData md) {
                uids.add(md.getUid());
            }
        });
        return uids;
    }

    public Map<Long, MessageData> fetch(String seq, Object param)
        throws IOException {
        final Map<Long, MessageData> results = new HashMap<Long, MessageData>();
        fetch(seq, param, new FetchResponseHandler(false) {
            @Override
            public void handleFetchResponse(MessageData md) {
                long msgno = md.getMsgno();
                if (msgno > 0) {
                    MessageData omd = results.get(msgno);
                    if (omd != null) {
                        omd.addFields(md);
                    } else {
                        results.put(msgno, md);
                    }
                }
            }
        });
        return results;
    }

    public MessageData fetch(long msgno, Object param) throws IOException {
        return fetch(String.valueOf(msgno), param).get(msgno);
    }

    public Map<Long, MessageData> uidFetch(String seq, Object param)
        throws IOException {
        final Map<Long, MessageData> results = new HashMap<Long, MessageData>();
        uidFetch(seq, param, new FetchResponseHandler(false) {
            @Override
            public void handleFetchResponse(MessageData md) {
                long uid = md.getUid();
                if (uid > 0) {
                    MessageData omd = results.get(uid);
                    if (omd != null) {
                        omd.addFields(md);
                    } else {
                        results.put(uid, md);
                    }
                }
            }
        });
        return results;
    }

    public MessageData uidFetch(long uid, Object param) throws IOException {
        return uidFetch(String.valueOf(uid), param).get(uid);
    }

    public List<Long> search(Object... params) throws IOException {
        return doSearch(CAtom.SEARCH.name(), params);
    }

    public List<Long> uidSearch(Object... params) throws IOException {
        return doSearch("UID SEARCH", params);
    }

    @SuppressWarnings("unchecked")
    private List<Long> doSearch(String cmd, Object... params) throws IOException {
        final List<Long> results = new ArrayList<Long>();
        ImapRequest req = newRequest(cmd, params);
        req.setResponseHandler(new ResponseHandler() {
            @Override
            public void handleResponse(ImapResponse res) {
                if (res.getCCode() == CAtom.SEARCH) {
                    results.addAll((List<Long>) res.getData());
                }
            }
        });
        req.sendCheckStatus();
        return results;
    }

    public void store(String seq, String item, Object flags) throws IOException {
        store(seq, item, flags, null);
    }

    public void store(String seq, String item, Object flags,
                      ResponseHandler handler) throws IOException {
        ImapRequest req = newRequest(CAtom.STORE, seq, item, flags);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    public void uidStore(String seq, String item, Object flags)
            throws IOException {
        uidStore(seq, item, flags, null);
    }

    public void uidStore(String seq, String item, Object flags,
                         ResponseHandler handler) throws IOException {
        ImapRequest req = newUidRequest(CAtom.STORE, seq, item, flags);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    public ImapRequest newRequest(CAtom cmd, Object... params) {
        return new ImapRequest(this, cmd.atom(), params);
    }

    public ImapRequest newRequest(Atom cmd, Object... params) {
        return new ImapRequest(this, cmd, params);
    }

    public ImapRequest newRequest(String cmd, Object... params) {
        return new ImapRequest(this, new Atom(cmd), params);
    }

    public ImapRequest newUidRequest(CAtom cmd, Object... params) {
        return newRequest("UID " + cmd.toString(), params);
    }


    public ImapCapabilities getCapabilities() {
        return capabilities;
    }

    public MailboxInfo getMailboxInfo() {
        // Make sure we return a copy of the actual mailbox since it can
        // be modified in-place in response to unsolicited messages from
        // the server.
        return mailbox != null ? new MailboxInfo(mailbox) : null;
    }

    public boolean hasCapability(String cap) {
        return capabilities != null && capabilities.hasCapability(cap);
    }

    public boolean hasIdle() {
        return hasCapability(ImapCapabilities.IDLE);
    }

    public boolean hasUnselect() {
        return hasCapability(ImapCapabilities.UNSELECT);
    }

    public boolean hasMechanism(String method) {
        return hasCapability("AUTH=" + method);
    }

    public boolean hasUidPlus() {
        return hasCapability(ImapCapabilities.UIDPLUS);
    }

    // Called from ImapRequest
    synchronized ImapResponse sendRequest(ImapRequest req) throws IOException {
        if (isClosed()) {
            throw new IOException("Connection is closed");
        }
        if (request != null) {
            ZimbraLog.imap_client.debug("sendRequest '%s' disallowed as Request '%s' already pending", req, request);
            throw new IllegalStateException(String.format("Request '%s' already pending", request.getCommand()));
        }
        if (req.isIdle()) {
            return sendIdle(req);
        }
        request = req;
        try {
            try {
                req.write(getImapOutputStream());
            } catch (LiteralException e) {
                return e.res;
            }
            // Wait for final response, handle continuation response
            while (true) {
                ImapResponse res = waitForResponse();
                if (res.isTagged()) {
                    return res;
                }
                assert res.isContinuation();
                if (!req.isAuthenticate()) {
                    throw req.failed("Unexpected continuation response");
                }
                processContinuation(res.getResponseText().getText());
            }
        } finally {
            request = null;
        }
    }

    public synchronized boolean isIdling() {
        return request != null && request.isIdle();
    }

    private ImapResponse sendIdle(ImapRequest req) {
        request = req;
        try {
            req.write(getImapOutputStream());
            ImapResponse res = waitForResponse();
            if (res.isTagged()) {
                return res;
            }
            assert res.isContinuation();
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    idleHandler();
                }
            });
            t.setName("IMAP IDLE thread");
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            request = null;
        }
        return null;
    }

    private void idleHandler() {
        try {
            ImapResponse res = waitForResponse();
            if (res.isContinuation()) {
                throw new IOException("Unexpected continuation response");
            }
            assert res.isTagged();
        } catch (SocketTimeoutException e) {
            getLogger().debug("Timed-out during IDLE", e);
        } catch (IOException e) {
            if (!isClosed()) {
                getLogger().error("IDLE failed", e);
            }
        }
        synchronized (this) {
            request = null;
            notifyAll();
        }
    }

    public synchronized boolean stopIdle() throws IOException {
        if (isIdling()) {
            ImapOutputStream out = getImapOutputStream();
            out.writeLine("DONE");
            out.flush();
            long waitTime = (getImapConfig().getReadTimeout() > 0 ?
                getImapConfig().getReadTimeout() : 30) * Constants.MILLIS_PER_SECOND;
            while (isIdling()) {
                long waitStart = System.currentTimeMillis();
                try {
                    wait(waitTime); //give server a chance to handle DONE normally
                } catch (InterruptedException e) {
                    // Ignore
                }
                waitTime = waitTime - (System.currentTimeMillis() - waitStart);
                if (waitTime <= 0 && isIdling()) {
                    close();
                    return false; //close and return false so connection is not reused
                }
            }
        }
        return !isIdling();
    }

    private ImapOutputStream getImapOutputStream() {
        return (ImapOutputStream) mailOut;
    }

    // Called from ImapRequest
    void writeLiteral(Literal lit) throws IOException {
        boolean lp = getImapConfig().isUseLiteralPlus() &&
                     hasCapability(ImapCapabilities.LITERAL_PLUS);
        ImapOutputStream out = (ImapOutputStream) mailOut;
        lit.writePrefix(out, lp);
        if (!lp) {
            out.flush();
            ImapResponse res = waitForResponse();
            if (!res.isContinuation()) {
                assert res.isTagged();
                throw new LiteralException(res);
            }
        }
        lit.writeData(out);
    }

    public ImapConfig getImapConfig() {
        return (ImapConfig) config;
    }

    // Exception thrown if we get an unexpected response to literal data
    private static class LiteralException extends IOException {
        final ImapResponse res;
        LiteralException(ImapResponse res) {
            this.res = res;
        }
    }

    private boolean isShutdown() {
        return isClosed() || isLogout();
    }

    /*
    * Read and process responses until next tagged or continuation response
    * has been received. Throws EOFException if end of stream has been
    * reached.
    */
    private ImapResponse waitForResponse() throws IOException {
        ImapResponse res;
        do {
            res = readResponse();
        } while (processResponse(res));
        return res;
    }

    private ImapResponse readResponse() throws IOException {
        try {
            return ImapResponse.read((ImapInputStream) mailIn);
        } catch (ParseException pe) {
            //read rest of the line so TraceInputStream dumps it for debugging
            mailIn.readLine();
            mailIn.trace();
            throw pe;
        }
    }

    /*
    * Process IMAP response. Returns true if this is not a final response and
    * reading should continue. Otherwise, returns false if tagged, untagged BAD,
    * or continuation response.
    */
    private synchronized boolean processResponse(ImapResponse res)
        throws IOException {
        if (res.isUntagged() && res.isBAD()) {
            getLogger().error("Untagged BAD response: " + res);
            return true;
        }
        if (res.isContinuation() || res.isUntagged() && res.isBAD()) {
            return false;
        }
        if (res.isUntagged()) {
            processUntagged(res);
            res.dispose(); // Clean up associated literal data

        }
        if (res.isOK()) {
            ResponseText rt = res.getResponseText();
            Atom code = rt.getCode();
            if (code != null && code.getCAtom() == CAtom.CAPABILITY) {
                capabilities = (ImapCapabilities) rt.getData();
            }
        } else if (res.getCCode() == CAtom.CAPABILITY) {
            capabilities = (ImapCapabilities) res.getData();
        } else if (mailbox != null && !request.isSelectOrExamine()) {
            mailbox.handleResponse(res);
        }
        return res.isUntagged();
    }

    /*
     * Process untagged response. Returns true if request handler processed
     * response otherwise returns false.
     */
    private boolean processUntagged(ImapResponse res) throws IOException {
        ResponseHandler handler = request.getResponseHandler();
        if (handler != null) {
            try {
                handler.handleResponse(res);
            } catch (Throwable e) {
                throw new MailException("Exception in response handler", e);
            }
        }
        return false;
    }

    public String newTag() {
        Formatter fmt = new Formatter();
        fmt.format(TAG_FORMAT, tagCount.incrementAndGet());
        return fmt.toString();
    }

    @Override
    public String toString() {
        return String.format("{host=%s,port=%d,type=%s,state=%s,folder=%s}",
            config.getHost(), config.getPort(), config.getSecurity(), state, mailbox == null ? "null" : mailbox.getName());
    }

    public void addAccountLogger(Account account, String category, String level) throws IOException {
        ImapRequest req = newRequest(CAtom.ZIMBRA_ADD_ACCOUNT_LOGGER, account.getName(), category, level);
        req.sendCheckStatus();
    }

    public void flushCache(String cacheTypes) throws IOException {
        Object[] typeParams = new Object[] { cacheTypes.split(",") };
        ImapRequest req = newRequest(CAtom.ZIMBRA_FLUSHCACHE, typeParams);
        req.sendCheckStatus();
    }

    public void flushCache(String cacheTypes, CacheEntry[] entries) throws IOException {
        String[] params = new String[entries.length * 2];
        for (int i = 0; i < entries.length; i++) {
            params[2*i] = entries[i].mEntryBy.toString();
            params[2*i+1] = entries[i].mEntryIdentity;
        }
        ImapRequest req = newRequest(CAtom.ZIMBRA_FLUSHCACHE, cacheTypes.split(","), params);
        req.sendCheckStatus();
    }

    public void reloadLocalConfig() throws IOException {
        ImapRequest req = newRequest(CAtom.ZIMBRA_RELOADLC);
        req.sendCheckStatus();
    }

    public static ImapConnection getZimbraConnection(Server server, String userName, AuthToken authToken) throws ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.adminName, userName);
        AuthenticatorFactory authFactory = new AuthenticatorFactory();
        authFactory.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
        ImapConfig config = new ImapConfig(server.getServiceHostname());
        config.setMechanism(ZimbraAuthenticator.MECHANISM);
        config.setAuthenticatorFactory(authFactory);
        config.setPort(server.getRemoteImapBindPort());
        config.setAuthenticationId(acct.getName());
        ImapConnection connection = new ImapConnection(config);
        try {
            connection.connect();
            connection.authenticate(authToken.getEncoded());
        } catch (IOException | LoginException | AuthTokenException e) {
            throw ServiceException.FAILURE("unable to create an IMAP connection as zimbra user", e);
        }
        return connection;
    }
}
