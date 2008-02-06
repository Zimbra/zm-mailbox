package com.zimbra.cs.mailclient.imap;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.protocol.IMAPProtocol;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MailDateFormat;

import java.io.EOFException;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

public class UidFetch {
    private ImapConfig mConfig;

    private static final boolean DEBUG = false;
    
    private static MailDateFormat mailDataFormat = new MailDateFormat();

    public UidFetch(ImapConfig config) {
        mConfig = config;
    }

    public UidFetch() {
        mConfig = new ImapConfig();
    }

    public void fetch(IMAPFolder folder, final String seq,
                      final Handler handler) throws IOException {
        try {
            folder.doCommand(new IMAPFolder.ProtocolCommand() {
                public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                    try {
                        return doFETCH(protocol, seq, handler);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new ProtocolException("FETCH failed: " + e);
                    }
                }
            });
        } catch (MessagingException e) {
            throw (IOException) new IOException("FETCH failed").initCause(e);
        }
    }

    public static interface Handler {
        void handleResponse(Literal literal, long uid, Date receivedDate) throws Exception;
    }

    private Object doFETCH(IMAPProtocol protocol, String seq, Handler handler)
            throws IOException, ProtocolException {
        String tag = protocol.writeCommand(
            "UID FETCH " + seq + " (BODY.PEEK[] UID INTERNALDATE)", null);
        ImapParser parser =
            new ImapParser(protocol.getInputStream().getRealInputStream(), mConfig);
        while (true) {
            ImapResponse ir = ImapResponse.read(parser);
            if (ir == null) throw new EOFException();
            try {
                if (ir.isBYE()) {
                    throw new IOException("Got BYE: " + ir.getError()); // XXX Fix this
                }
                if (ir.isTagged()) {
                    if (!tag.equals(ir.getTag())) {
                        throw new IOException("FETCH failed: Incorrect tag '" + ir.getTag() + '"');
                    }
                    if (!ir.isOK()) {
                        throw new IOException("FETCH failed: " + ir.getAtom() + " " + ir.getError());
                    }
                    break;
                }
                handleFetchResponse(ir, handler);
            } finally {
                ir.cleanup();
            }
        }
        return null;
    }

    private static void handleFetchResponse(ImapResponse ir, Handler handler)
            throws IOException {
        if (ir.getAtom() != Atom.FETCH) {
            throw new IOException("Not a fetch response");
        }
        long uid = 0;
        Date date = null;
        Literal lit = null;
        try {
            ImapData data = ir.getData()[0];
            for (Iterator it = data.getListValue().iterator(); it.hasNext(); ) {
                ImapData d = (ImapData) it.next();
                if (d.isAtom(Atom.UID)) {
                    uid = ((ImapData) it.next()).getLongValue();
                } else if (d.isAtom("BODY[]")) {
                    lit = ((ImapData) it.next()).getLiteralValue();
                } else if (d.isAtom(Atom.INTERNALDATE)) {
                	date = mailDataFormat.parse(((ImapData)it.next()).getStringValue());
                }
            }
            if (DEBUG) pd("fetched: uid = %s, data = %s", uid, data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Invalid FETCH request");
        }
        try {
            handler.handleResponse(lit, uid, date);
        } catch (Exception e) {
            throw (IOException)
                new IOException("FETCH handler failed").initCause(e);
        }
    }

    private static void pd(String fmt, Object... args) {
        System.out.print("[DEBUG] ");
        System.out.printf(fmt, args);
        System.out.println();
    }
    
    public static void main(String[] args) throws Exception {
        Session session = Session.getDefaultInstance(System.getProperties());
        session.setDebug(false);
        Store store = session.getStore("imap");
        // store.connect("imap.mail.yahoo.com", 143, "jjztest", "test1234");
        IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        FetchProfile fp = new FetchProfile();
        fp.add(UIDFolder.FetchProfileItem.UID);
        fp.add(UIDFolder.FetchProfileItem.FLAGS);
        fp.add(UIDFolder.FetchProfileItem.ENVELOPE);
        Message[] msgs = folder.getMessages();
        pd("Found %d messages in %s", msgs.length, folder.getName());
        for (Message msg : msgs) {
            IMAPMessage im = (IMAPMessage) msg;
            pd("msg %s: %s", im.getMessageID(), im.getSentDate());
        }
        ImapConfig config = new ImapConfig();
        if (args.length > 0) {
            config.setMaxLiteralMemSize(Integer.parseInt(args[0]));
        }
        new UidFetch(config).fetch(folder, "1:*", new Handler() {
            public void handleResponse(Literal l, long uid, Date receivedDate) {
                pd("Fetched message: uid = %d, size = %d, received = %s", uid, l.getSize(), receivedDate.toString());
            }
        });
    }
}
