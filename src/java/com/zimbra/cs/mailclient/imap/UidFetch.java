package com.zimbra.cs.mailclient.imap;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailclient.CommandFailedException;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import java.io.EOFException;
import java.io.IOException;

public class UidFetch {
    private ImapConfig config;

    private static final boolean DEBUG = false;
    
    public UidFetch(ImapConfig config) {
        this.config = config;
    }

    public UidFetch() {
        config = new ImapConfig();
    }

    public void fetch(IMAPFolder folder, final String seq, final Handler handler)
            throws IOException {
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
        void handleResponse(MessageData md) throws Exception;
    }

    private Object doFETCH(IMAPProtocol protocol, String seq, Handler handler)
            throws IOException, ProtocolException {
        String tag = protocol.writeCommand(
            "UID FETCH " + seq + " (BODY.PEEK[] UID INTERNALDATE)", null);
        ImapInputStream is =
            new ImapInputStream(protocol.getInputStream().getRealInputStream(), config);
        while (true) {
            ImapResponse res = ImapResponse.read(is);
            if (res == null) throw new EOFException();
            try {
                if (res.isError()) {
                    throw new CommandFailedException(
                        "FETCH", res.getResponseText().getText());
                }
                if (res.isWarning()) {
                    ZimbraLog.datasource.warn(res.getResponseText().getText());
                    continue;
                }
                if (res.isTagged()) {
                    assert res.isOK();
                    if (!tag.equals(res.getTag())) {
                        throw new IOException("Protocol error: FETCH failed (incorrect tag)");
                    }
                    break;
                }
                if (res.getCode() == CAtom.FETCH) {
                    handleResponse((MessageData) res.getData(), handler);
                }
            } finally {
                res.dispose();
            }
        }
        return null;
    }

    private static void handleResponse(MessageData md, Handler handler)
            throws CommandFailedException {
        try {
            handler.handleResponse(md);
        } catch (Exception e) {
            throw (CommandFailedException)
                new CommandFailedException("FETCH", "handler failed").initCause(e);
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
        // store.connect("mail.mac.com", 143, "dwconnelly", "Dthx1138");
        store.connect("imap.aol.com", 143, "dacztest", "Dthx1138");
        // store.connect("localhost", 7143, "user1", "test123");
        IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        //FetchProfile fp = new FetchProfile();
        //fp.add(UIDFolder.FetchProfileItem.UID);
        //fp.add(UIDFolder.FetchProfileItem.FLAGS);
        //fp.add(UIDFolder.FetchProfileItem.ENVELOPE);
        Message[] msgs = folder.getMessages();
        pd("Found %d messages in %s", msgs.length, folder.getName());
        for (Message msg : msgs) {
            IMAPMessage im = (IMAPMessage) msg;
            pd("msg %s: %s", im.getMessageNumber(), im.getSentDate());
        }
        ImapConfig config = new ImapConfig();
        if (args.length > 0) {
            config.setMaxLiteralMemSize(Integer.parseInt(args[0]));
        }
        new UidFetch(config).fetch(folder, "1:*", new Handler() {
            public void handleResponse(MessageData md) {
                Body b = md.getBodySections()[0];
                pd("Fetched message: uid = %d, size = %d, date = %s",
                    md.getUid(), b.getSize(), md.getInternalDate());
            }
        });
    }
}
