/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.MailConnection;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.cs.mailclient.MailInputStream;
import com.zimbra.cs.mailclient.MailOutputStream;
import com.zimbra.cs.mailclient.ParseException;
import com.zimbra.cs.mailclient.CommandFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zimbra.cs.mailclient.imap.ImapData.asAString;
import org.apache.log4j.Logger;

public final class ImapConnection extends MailConnection {
    private ImapCapabilities capabilities;
    private Mailbox mailbox;
    private ImapRequest request;
    private ImapResponse response;
    private Runnable reader;
    private Throwable error;
    private final AtomicInteger tagCount = new AtomicInteger();

    private static final Logger LOGGER = Logger.getLogger(ImapConnection.class);
    
    private static final String TAG_FORMAT = "C%02d";

    private static final String UID_COPY = "UID COPY";
    private static final String UID_FETCH = "UID FETCH";
    private static final String UID_STORE = "UID STORE";
    private static final String UID_SEARCH = "UID SEARCH";

    
    public ImapConnection(ImapConfig config) {
        super(config);
    }

    protected MailInputStream getMailInputStream(InputStream is) {
        return new ImapInputStream(is, (ImapConfig) config);
    }

    protected MailOutputStream getMailInputStream(OutputStream os) {
        return new ImapOutputStream(os);
    }

    public Logger getLogger() {
        return LOGGER;
    }
    
    @Override
    public synchronized void connect() throws IOException {
        super.connect();
        if (!config.isRawMode()) {
            startReader();
        }
    }
    
    protected void processGreeting() throws IOException {
        ImapResponse res = readResponse();
        if (res.isUntagged()) {
            switch (res.getCode()) {
            case BYE:
                throw new MailException(
                    "IMAP connection refused: "+ res.getResponseText().getText());
            case PREAUTH: case OK:
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

    protected void sendLogin(String user, String pass) throws IOException {
        newRequest(CAtom.LOGIN, asAString(user), asAString(pass)).sendCheckStatus();
    }

    public synchronized void logout() throws IOException {
        if (state == State.LOGOUT) return;
        setState(State.LOGOUT);
        try {
            newRequest(CAtom.LOGOUT).sendCheckStatus();
        } catch (CommandFailedException e) {
            getLogger().warn("Logout failed, force closing connection", e);
            close();
        }
    }

    protected void sendAuthenticate(boolean ir) throws IOException {
        ImapRequest req = newRequest(CAtom.AUTHENTICATE, config.getMechanism());
        if (ir) {
            byte[] response = authenticator.getInitialResponse();
            req.addParam(encodeBase64(response));
        }
        ImapResponse res = req.send();
        while (res.isContinuation()) {
            processContinuation(res.getContinuation());
        }
        req.checkStatus(res);
    }

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
    
    public synchronized Mailbox select(String name) throws IOException {
        mailbox = doSelectOrExamine(CAtom.SELECT, name);
        setState(State.SELECTED);
        return mailbox;
    }

    public Mailbox examine(String name) throws IOException {
        return doSelectOrExamine(CAtom.EXAMINE, name);
    }

    private Mailbox doSelectOrExamine(CAtom cmd, String name) throws IOException {
        Mailbox mbox = new Mailbox(name);
        ImapRequest req = newRequest(cmd, new MailboxName(name));
        req.setResponseHandler(mbox);
        req.sendCheckStatus();
        return mbox;
    }

    public void create(String name) throws IOException {
        newRequest(CAtom.CREATE, new MailboxName(name)).sendCheckStatus();
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

    public long append(String mbox, Flags flags, Date date, Literal data)
        throws IOException {
        ImapRequest req = newRequest(CAtom.APPEND, new MailboxName(mbox));
        if (flags != null) req.addParam(flags);
        if (date != null) req.addParam(date);
        req.addParam(data);
        ImapResponse res = req.sendCheckStatus();
        ResponseText rt = res.getResponseText();
        if (CAtom.APPENDUID.atom().equals(rt.getCode())) {
            // Supports UIDPLUS (RFC 2359):
            // resp_code_apnd ::= "APPENDUID" SPACE nz_number SPACE uniqueid
            String[] s = ((String) rt.getData()).split(" ");
            if (s.length == 2) {
                return Chars.getNumber(s[1]); // message UID
            }
        }
        return 0;
    }

    public void expunge() throws IOException {
        newRequest(CAtom.EXPUNGE).sendCheckStatus();
    }

    public synchronized void mclose() throws IOException {
        newRequest(CAtom.CLOSE).sendCheckStatus();
        mailbox = null;
        setState(State.AUTHENTICATED);
    }
    
    public Mailbox status(String name, Object... params) throws IOException {
        ImapRequest req = newRequest(CAtom.STATUS, new MailboxName(name), params);
        final Mailbox[] mbox = new Mailbox[0];
        req.setResponseHandler(new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.getCode() == CAtom.STATUS) {
                    mbox[0] = (Mailbox) res.getData();
                    return true;
                }
                return false;
            }
        });
        req.sendCheckStatus();
        if (mbox[0] == null) {
            throw new MailException("Missing STATUS response data");
        }
        mbox[0].setName(name);
        return mbox[0];
    }

    public List<ListData> list(String reference, String mailbox) throws IOException {
        return doList(CAtom.LIST, reference, mailbox);
    }

    public List<ListData> lsub(String reference, String mailbox) throws IOException {
        return doList(CAtom.LSUB, reference, mailbox);
    }
    
    private List<ListData> doList(CAtom cmd, String reference, String mailbox)
        throws IOException {
        ImapRequest req = newRequest(cmd, reference, mailbox);
        final List<ListData> results = new ArrayList<ListData>();
        req.setResponseHandler(new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.getCode() == CAtom.LIST) {
                    results.add((ListData) res.getData());
                    return true;
                }
                return false;
            }
        });
        req.sendCheckStatus();
        return results;
    }

    
    public void copy(String seq, String mbox) throws IOException {
        newRequest(CAtom.COPY, seq, new MailboxName(mbox)).sendCheckStatus();
    }

    public void uidCopy(String seq, String mbox) throws IOException {
        newRequest(UID_COPY, seq, new MailboxName(mbox)).sendCheckStatus();
    }
    
    public void fetch(String seq, Object param, ResponseHandler handler)
        throws IOException {
        fetch(CAtom.FETCH.name(), seq, param, handler);
    }

    public void uidFetch(String seq, Object param, ResponseHandler handler)
        throws IOException {
        fetch(UID_FETCH, seq, param, handler);
    }

    public List<MessageData> fetch(String seq, Object param)
        throws IOException {
        return fetch(CAtom.FETCH.name(), seq, param);
    }

    public List<MessageData> uidFetch(String seq, Object param)
        throws IOException {
        return fetch(UID_FETCH, seq, param);
    }
    
    private List<MessageData> fetch(String cmd, String seq, Object param)
        throws IOException {
        final List<MessageData> mds = new ArrayList<MessageData>();
        fetch(cmd, seq, param, new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.getCode() == CAtom.FETCH) {
                    mds.add((MessageData) res.getData());
                    return true;
                }
                return false;
            }
        });
        return mds;
    }

    private void fetch(String cmd, String seq, Object param,
                       ResponseHandler handler) throws IOException {
        ImapRequest req = newRequest(cmd, seq, param);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    public List<Long> search(Object... params) throws IOException {
        return doSearch(CAtom.SEARCH.name(), params);
    }

    public List<Long> uidSearch(Object... params) throws IOException {
        return doSearch(UID_SEARCH, params);
    }

    @SuppressWarnings("unchecked")
    private List<Long> doSearch(String cmd, Object... params) throws IOException {
        final List<Long> ids = new ArrayList<Long>();
        ImapRequest req = newRequest(cmd, params);
        req.setResponseHandler(new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.getCode() == CAtom.SEARCH) {
                    ids.addAll((List<Long>) res.getData());
                    return true;
                }
                return false;
            }
        });
        req.sendCheckStatus();
        return ids;
    }

    public void store(String seq, String item, Object flags) throws IOException {
        store(seq, item, flags, null);
    }
    
    public void store(String seq, String item, Object flags,
                      ResponseHandler handler)
        throws IOException {
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
        ImapRequest req = newRequest(UID_STORE, seq, item, flags);
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

    public ImapCapabilities getCapabilities() {
        return capabilities;
    }

    public Mailbox getMailbox() {
        return mailbox;
    }
    
    public boolean hasCapability(String cap) {
        return capabilities != null && capabilities.hasCapability(cap);
    }

    // Called from ImapRequest
    synchronized ImapResponse sendRequest(ImapRequest req)
        throws IOException {
        if (isClosed()) {
            throw new IllegalStateException("Connection is closed");
        }
        req.write((ImapOutputStream) mailOut);
        return waitForResponse(req);
    }

    // Called from ImapRequest
    void writeLiteral(ImapRequest req, Literal lit) throws IOException {
        boolean lp = hasCapability(ImapCapabilities.LITERAL_PLUS);
        ImapOutputStream out = (ImapOutputStream) mailOut; 
        lit.writePrefix(out, lp);
        if (!lp) {
            out.flush();
            ImapResponse res = waitForResponse(req);
            if (!res.isContinuation()) {
                throw new ParseException(
                    "Expected a literal continuation response");
            }
        }
        lit.writeData(out);
    }
    
    // Wait for tagged or continuation response
    private ImapResponse waitForResponse(ImapRequest req) throws IOException {
        if (request != null) {
            throw new IllegalStateException("Request already pending");
        }
        request = req;
        try {
            if (reader == null) {
                // Reader thread not active, so read response inline
                return nextResponse();
            }
            response = null;
            while (response == null && !isClosed()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new IOException("Thread interrupted");
                }
            }
            if (response != null) {
                return response;
            }
            throw (IOException) new IOException(
                "Exception in response handler").initCause(error);
        } finally {
            request = null;
            response = null;
        }
    }

    private synchronized void setResponse(ImapResponse res) {
        if (request == null) {
            getLogger().warn("Ignoring tagged or continuation response while" +
                             " no request pending: " + res);
        } else if (response != null) {
            getLogger().warn("Ignoring unexpected tagged or continuation" +
                             " response: " + res);
        }
        response = res;
        notifyAll();
    }
    
    private void startReader() {
        reader = new Runnable() {
            public void run() {
                try {
                    while (!isClosed()) {
                        setResponse(nextResponse());
                    }
                } catch (Throwable e) {
                    readError(e);
                }
            }
        };
        Thread t = new Thread(reader);
        t.setDaemon(true);
        t.start();
    }

    private synchronized void readError(Throwable e) {
        if (!(e instanceof IOException && isShutdown())) {
            // Only record an error if shutting down
            this.error = e;
        }
        super.close();
        notifyAll();
    }

    private boolean isShutdown() {
        return isClosed() || state == State.LOGOUT;
    }
    
    /*
     * Read and process responses until next tagged or continuation response
     * has been received. Throws EOFException if end of stream has been
     * reached.
     */
    private ImapResponse nextResponse() throws IOException {
        ImapResponse res;
        do {
            res = readResponse();
        } while (processResponse(res));
        return res;
    }

    private ImapResponse readResponse() throws IOException {
        return ImapResponse.read((ImapInputStream) mailIn);
    }
    
    /*
     * Process IMAP response. Returns true if this is not a tagged or
     * continuation response and reading should continue. Returns false
     * if tagged, untagged BAD, or continuation response.
     */
    private synchronized boolean processResponse(ImapResponse res)
        throws IOException {
        if (res.isContinuation() || res.isUntagged() && res.isBAD()) {
            return true;
        }
        if (request != null) {
            // Request pending, try response handler first
            ResponseHandler handler = request.getResponseHandler();
            if (res.isUntagged() && handler != null) {
                try {
                    if (handler.handleResponse(res)) {
                        return true; // Handler processed response
                    }
                } catch (Throwable e) {
                    throw new MailException("Exception in response handler", e);
                }
            }
        } else if (res.isTagged()) {
            // If no pending request, then must be untagged response
            throw new MailException(
                "Received tagged response with no request pending: " + res);

        }
        if (res.isOK()) {
            ResponseText rt = res.getResponseText();
            Atom code = rt.getCode();
            if (code != null && code.getCAtom() == CAtom.CAPABILITY) {
                capabilities = (ImapCapabilities) rt.getData();
            }
        } else if (res.getCode() == CAtom.CAPABILITY) {
            capabilities = (ImapCapabilities) res.getData();
        } else if (mailbox != null) {
            mailbox.handleResponse(res);
        }
        return res.isUntagged();
    }

    public String newTag() {
        Formatter fmt = new Formatter();
        fmt.format(TAG_FORMAT, tagCount.incrementAndGet());
        return fmt.toString();
    }
}
