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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import static com.zimbra.cs.mailclient.imap.ImapData.asAString;
import org.apache.log4j.Logger;

public class ImapConnection extends MailConnection {
    private ImapCapabilities capabilities;
    private Mailbox mailbox;
    private ImapRequest request;
    private ImapResponse response;
    private Runnable reader;
    private int tagCount;

    private static final Logger LOGGER = Logger.getLogger(ImapConnection.class);
    
    private static final String TAG_FORMAT = "C%02d";
    
    public ImapConnection(ImapConfig config) {
        super(config);
    }

    protected MailInputStream getMailInputStream(InputStream is) {
        return new ImapInputStream(is, (ImapConfig) config);
    }

    protected MailOutputStream getMailInputStream(OutputStream os) {
        return new ImapOutputStream(os);
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
            case PREAUTH:
                authenticated = true;
            case OK:
                ResponseText rt = res.getResponseText();
                if (CAtom.CAPABILITY.atom().equals(rt.getCode())) {
                    capabilities = (ImapCapabilities) rt.getData();
                } else {
                    capability();
                }
                return;
            }
        }
        throw new MailException("Unexpected greeting response: " + res);
    }

    protected void sendLogin(String user, String pass) throws IOException {
        newRequest(CAtom.LOGIN, asAString(user), asAString(pass)).sendCheckStatus();
    }

    public void logout() throws IOException {
        newRequest(CAtom.LOGOUT).sendCheckStatus();
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

    public void append(String mbox, Flags flags, Date date, Literal data)
            throws IOException {
        ImapRequest req = newRequest(CAtom.APPEND, new MailboxName(mbox));
        if (flags != null) req.addParam(flags);
        if (date != null) req.addParam(date);
        req.addParam(data);
        req.sendCheckStatus();
    }

    public void expunge() throws IOException {
        newRequest(CAtom.EXPUNGE).sendCheckStatus();
    }

    public synchronized void mclose() throws IOException {
        newRequest(CAtom.CLOSE).sendCheckStatus();
        mailbox = null;
    }
    
    public Mailbox status(String name, Object... params) throws IOException {
        ImapRequest req = newRequest(CAtom.STATUS, new MailboxName(name), params);
        final Mailbox[] mbox = new Mailbox[0];
        req.setResponseHandler(new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.isUntagged() && res.getCode() == CAtom.STATUS) {
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
                if (res.isUntagged() && res.getCode() == CAtom.LIST) {
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
        newRequest("UID COPY", seq, new MailboxName(mbox)).sendCheckStatus();
    }
    
    public void fetch(String seq, Object param, ResponseHandler handler)
            throws IOException {
        ImapRequest req = newRequest(CAtom.FETCH, seq, param);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    public void uidFetch(String seq, Object param, ResponseHandler handler)
            throws IOException {
        ImapRequest req = newRequest("UID FETCH", seq, param);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }

    public void search(Object... params) throws IOException {
        doSearch(CAtom.SEARCH.atom(), params);
    }

    public void uidSearch(Object... params) throws IOException {
        doSearch(new Atom("UID SEARCH"), params);
    }
    
    private List<Long> doSearch(Atom cmd, Object... params) throws IOException {
        final List<Long> ids = new ArrayList<Long>();
        ImapRequest req = newRequest(cmd, params);
        req.setResponseHandler(new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.isUntagged() && res.getCode() == CAtom.SEARCH) {
                    ids.addAll((List<Long>) res.getData());
                    return true;
                }
                return false;
            }
        });
        req.sendCheckStatus();
        return ids;
    }

    public void store(String seq, String item, Flags flags,
                      ResponseHandler handler)
            throws IOException {
        ImapRequest req = newRequest(CAtom.STORE, seq, item, flags);
        req.setResponseHandler(handler);
        req.sendCheckStatus();
    }
    
    public void uidStore(String seq, String item, Flags flags,
                         ResponseHandler handler)
            throws IOException {
        ImapRequest req = newRequest("UID STORE", seq, item, flags);
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

    public boolean hasLiteralPlus() {
        return hasCapability(ImapCapabilities.LITERAL_PLUS);
    }

    // Wait for tagged or continuation response
    public ImapResponse waitForResponse(ImapRequest req) throws IOException {
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
            throw new IOException("Connection closed");
        } finally {
            request = null;
            response = null;
        }
    }

    private synchronized void setResponse(ImapResponse res) {
        if (request == null) {
            LOGGER.warn("Ignoring tagged or continuation response while no" +
                        " request pending: " + res);
        } else if (response != null) {
            LOGGER.warn("Ignoring unexpected tagged or continuation response: "
                        + res);
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
                } catch (IOException e) {
                    if (!isClosed()) {
                        LOGGER.error("I/O error in reader thread", e);
                    }
                }
                close();
            }
        };
        new Thread(reader).start();
    }

    @Override
    public synchronized void close() {
        super.close();
        notifyAll();
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
     * Process IMAP response. Returns true if not a tagged or continuation
     * response and reading should continue. Returns false if tagged,
     * untagged BAD, or continuation response.
     */
    private synchronized boolean processResponse(ImapResponse res)
        throws IOException {
        if (res.isContinuation() || res.isUntagged() && res.isBAD()) {
            return true;
        }
        if (request != null) {
            // Request pending, try response handler first
            ResponseHandler handler = request.getResponseHandler();
            if (handler != null) {
                try {
                    if (handler.handleResponse(res)) {
                        return res.isUntagged(); // Handler processed response
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
        fmt.format(TAG_FORMAT, tagCount++);
        return fmt.toString();
    }
}
