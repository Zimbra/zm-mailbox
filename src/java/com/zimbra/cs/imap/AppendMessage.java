package com.zimbra.cs.imap;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsulates append message data for an APPEND request.
 */
class AppendMessage {
    private final ImapHandler handler;
    private final String tag;
    private Date date;
    private boolean catenate;
    private List<Part> parts;
    private List<String> flagNames;
    private int flags = Flag.BITMASK_UNREAD;
    private long tags;
    private short sflags;

    public static AppendMessage parse(ImapHandler handler, String tag, ImapRequest req)
        throws ImapParseException, IOException {
        AppendMessage append = new AppendMessage(handler, tag);
        append.parse(req);
        return append;
    }

    private AppendMessage(ImapHandler handler, String tag) {
        this.handler = handler;
        this.tag = tag;
    }
    
    private void parse(ImapRequest req) throws ImapParseException, IOException {
        req.skipSpace();
        if (req.peekChar() == '(') {
            flagNames = req.readFlags();
            req.skipSpace();
        }
        if (req.peekChar() == '"') {
            date = req.readDate(true, true);
            req.skipSpace();
        }
        if ((req.peekChar() == 'c' || req.peekChar() == 'C') && handler.extensionEnabled("CATENATE")) {
            req.skipAtom("CATENATE");
            req.skipSpace();
            req.skipChar('(');
            catenate = true;
            parts = new ArrayList<Part>();
            while (req.peekChar() != ')') {
                if (!parts.isEmpty())
                    req.skipSpace();
                String type = req.readATOM();
                req.skipSpace();
                if (type.equals("TEXT"))
                    parts.add(new Part(req.readLiteral()));
                else if (type.equals("URL"))
                    parts.add(new Part(new ImapURL(tag, handler, req.readAstring())));
                else throw new ImapParseException(tag, "unknown CATENATE cat-part: " + type);
            }
            req.skipChar(')');
        } else {
            parts = new ArrayList<Part>(1);
            parts.add(new Part(req.readLiteral8()));
        }
    }

    public void checkFlags(ImapFlagCache flagSet, ImapFlagCache tagSet, List<Tag> newTags)
        throws ServiceException {
        if (flagNames == null) return;
        for (String name : flagNames) {
            ImapFlagCache.ImapFlag i4flag = flagSet.getByName(name);
            if (i4flag != null && !i4flag.mListed)
                i4flag = null;
            else if (i4flag == null && !name.startsWith("\\"))
                i4flag = tagSet.getByName(name);
            if (i4flag == null)
                i4flag = tagSet.createTag(handler.getContext(), name, newTags);

            if (i4flag != null) {
                if (!i4flag.mPermanent)               sflags |= i4flag.mBitmask;
                else if (Tag.validateId(i4flag.mId))  tags |= i4flag.mBitmask;
                else if (i4flag.mPositive)            flags |= i4flag.mBitmask;
                else                                  flags &= ~i4flag.mBitmask;
            }
        }
        flagNames = null;
    }

    public int storeContent(Object mboxObj, Object folderObj)
        throws IOException, ServiceException, ImapParseException {
        Blob blob = getContent();
        try {
            return mboxObj instanceof Mailbox ?
                store(blob, (Mailbox) mboxObj, (Folder) folderObj) :
                store(blob, (ZMailbox) mboxObj, (ZFolder) folderObj);
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.toString(), e);
        } finally {
            StoreManager.getInstance().quietDelete(blob);
        }
    }

    private int store(Blob content, Mailbox mbox, Folder folder)
        throws ServiceException, IOException, ImapParseException {
        boolean idxAttach = mbox.attachmentsIndexingEnabled();
        Long receivedDate = date != null ? date.getTime() : null;
        ParsedMessage pm = new ParsedMessage(content.getFile(), receivedDate, idxAttach);
        try {
            if (!pm.getSender().equals("")) {
                InternetAddress ia = new InternetAddress(pm.getSender());
                if (AccountUtil.addressMatchesAccount(mbox.getAccount(), ia.getAddress()))
                    flags |= Flag.BITMASK_FROM_ME;
            }
        } catch (Exception e) { }
        Message msg = mbox.addMessage(handler.getContext(), pm, folder.getId(), true, flags, Tag.bitmaskToTags(tags));
        if (msg != null && sflags != 0 && handler.getState() == ImapHandler.State.SELECTED) {
            ImapFolder selectedFolder = handler.getSelectedFolder();
            ImapMessage i4msg = selectedFolder.getById(msg.getId());
            if (i4msg != null)
                i4msg.setSessionFlags(sflags, selectedFolder);
        }
        return msg == null ? -1 : msg.getId();
    }

    private int store(Blob content, ZMailbox mbox, ZFolder folder)
        throws IOException, ServiceException {
        InputStream is = content.getInputStream();
        String id = mbox.addMessage(folder.getId(), Flag.bitmaskToFlags(flags), null, date.getTime(), is, content.getRawSize(), true);
        return new ItemId(id, getCredentials().getAccountId()).getId();
    }
    
    private Blob getContent() throws IOException, ImapParseException, ServiceException {
        if (!catenate) {
            assert parts.size() == 1;
            return parts.get(0).literal.getBlob();
        }
        // translate CATENATE (...) directives into Blob
        BlobBuilder bb = FileBlobStore.getInstance().getBlobBuilder();
        boolean success = false;
        try {
            for (Part part : parts) {
                copyBytes(part.getInputStream(), bb);
                part.cleanup();
            }
            Blob blob = bb.finish();
            if (blob.getRawSize() > handler.getConfig().getMaxRequestSize()) {
                throw new ImapParseException(tag, "TOOBIG", "request too long", false);
            }
            checkDate(blob);
            success = true;
            return blob;
        } finally {
            for (Part part : parts) {
                part.cleanup();
            }
            parts = null;
            if (!success) {
                bb.dispose();
            }
        }
    }

    private void copyBytes(InputStream is, BlobBuilder bb) throws IOException {
        byte[] b = new byte[8192];
        int len;
        try {
            while ((len = is.read(b)) != -1) {
                bb.append(b, 0, len);
            }
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    private void checkDate(Blob content) throws IOException, ServiceException {
        if (date == null && getCredentials().isHackEnabled(ImapCredentials.EnabledHack.THUNDERBIRD)) {
            date = getSentDate(content);
        }
        // server uses UNIX time, so range-check specified date (is there a better place for this?)
        // FIXME: Why is this different from INTERNALDATE range check?
        if (date != null && date.getTime() > Integer.MAX_VALUE * 1000L) {
            ZimbraLog.imap.info("APPEND failed: date out of range");
            throw ServiceException.FAILURE("APPEND failed (date out of range)", null);
        }
    }

    private static Date getSentDate(Blob content) throws IOException {
        InputStream is = content.getInputStream();
        try {
            // inefficient, but must be done before creating the ParsedMessage
            return new MimeMessage(JMSession.getSession(), is).getSentDate();
        } catch (MessagingException e) {
            return null;
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    private ImapCredentials getCredentials() {
        return handler.getCredentials();
    }

    /*
    * Append message part, either literal data or IMAP URL.
    */
    private class Part {
        Literal literal;
        ImapURL url;

        Part(Literal literal) {
            this.literal = literal;
        }

        Part(ImapURL url) {
            this.url = url;
        }

        InputStream getInputStream() throws IOException, ImapParseException {
            return literal != null ? literal.getInputStream() :
                url.getContentAsStream(handler, getCredentials(), tag).getSecond();
        }
        void cleanup() {
            if (literal != null) literal.cleanup();
        }
    }
}
