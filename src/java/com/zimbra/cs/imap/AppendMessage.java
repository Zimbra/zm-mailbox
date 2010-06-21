/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MailDateFormat;
import javax.mail.MessagingException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.text.ParseException;

/**
 * Encapsulates append message data for an APPEND request.
 */
class AppendMessage {
    final ImapHandler handler;
    final String tag;

    private Date date;
    private boolean catenate;
    private List<Part> parts;
    private Blob content;
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
            parts = new ArrayList<Part>(5);
            while (req.peekChar() != ')') {
                if (!parts.isEmpty())
                    req.skipSpace();
                String type = req.readATOM();
                req.skipSpace();
                if (type.equals("TEXT"))
                    parts.add(new Part(req.readLiteral()));
                else if (type.equals("URL"))
                    parts.add(new Part(new ImapURL(tag, handler, req.readAstring())));
                else
                    throw new ImapParseException(tag, "unknown CATENATE cat-part: " + type);
            }
            req.skipChar(')');
        } else {
            parts = Arrays.asList(new Part(req.readLiteral8()));
        }
    }

    public void checkFlags(Mailbox mbox, ImapFlagCache flagSet, ImapFlagCache tagSet, List<Tag> newTags)
    throws ServiceException {
        if (flagNames == null)
            return;

        for (String name : flagNames) {
            ImapFlagCache.ImapFlag i4flag = flagSet.getByName(name);
            if (i4flag != null && !i4flag.mListed)
                i4flag = null;
            else if (i4flag == null && !name.startsWith("\\"))
                i4flag = tagSet.getByName(name);

            if (i4flag == null)
                i4flag = tagSet.createTag(mbox, handler.getContext(), name, newTags);

            if (i4flag != null) {
                if (!i4flag.mPermanent)               sflags |= i4flag.mBitmask;
                else if (Tag.validateId(i4flag.mId))  tags |= i4flag.mBitmask;
                else if (i4flag.mPositive)            flags |= i4flag.mBitmask;
                else                                  flags &= ~i4flag.mBitmask;
            }
        }
        flagNames = null;
    }

    public int storeContent(Object mboxObj, Object folderObj) throws IOException, ServiceException, ImapParseException {
        try {
            checkDate(content);
            if (mboxObj instanceof Mailbox)
                return store((Mailbox) mboxObj, (Folder) folderObj);
            else
                return store((ZMailbox) mboxObj, (ZFolder) folderObj);
        } finally {
            cleanup();
        }
    }

    private int store(Mailbox mbox, Folder folder) throws ServiceException, IOException {
        boolean idxAttach = mbox.attachmentsIndexingEnabled();
        Long receivedDate = date != null ? date.getTime() : null;
        ParsedMessage pm = new ParsedMessage(content, receivedDate, idxAttach);
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

    private int store(ZMailbox mbox, ZFolder folder) throws IOException, ServiceException {
        InputStream is = content.getInputStream();
        String id = mbox.addMessage(folder.getId(), Flag.bitmaskToFlags(flags), null, date.getTime(), is, content.getRawSize(), true);
        return new ItemId(id, getCredentials().getAccountId()).getId();
    }

    public void checkContent() throws IOException, ImapParseException, ServiceException {
        content = catenate ? doCatenate() : parts.get(0).literal.getBlob();
        if (content.getRawSize() > handler.getConfig().getMaxMessageSize()) {
            cleanup();
            if (catenate) {
                throw new ImapParseException(tag, "TOOBIG", "maximum message size exceeded", false);
            } else {
                throw new ImapParseException(tag, "maximum message size exceeded", true);
            }
        }
    }
    
    private Blob doCatenate() throws IOException, ImapParseException, ServiceException {
        // translate CATENATE (...) directives into Blob
        BlobBuilder bb = StoreManager.getInstance().getBlobBuilder();
        try {
            for (Part part : parts) {
                copyBytes(part.getInputStream(), bb);
                part.cleanup();
            }
            return bb.finish();
        } finally {
            for (Part part : parts)
                part.cleanup();
            parts = null;
        }
    }

    public void cleanup() {
        if (content != null) {
            StoreManager.getInstance().quietDelete(content);
            content = null;
        }
    }
    
    private void copyBytes(InputStream is, BlobBuilder bb) throws IOException {
        try {
            bb.append(is);
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    private void checkDate(Blob blob) throws IOException, ServiceException {
        // if we're using Thunderbird, try to set INTERNALDATE to the message's Date: header
        if (date == null && getCredentials().isHackEnabled(ImapCredentials.EnabledHack.THUNDERBIRD))
            date = getSentDate(blob);

        // server uses UNIX time, so range-check specified date (is there a better place for this?)
        // FIXME: Why is this different from INTERNALDATE range check?
        if (date != null && date.getTime() > Integer.MAX_VALUE * 1000L) {
            ZimbraLog.imap.info("APPEND failed: date out of range");
            throw ServiceException.FAILURE("APPEND failed (date out of range)", null);
        }
    }

    private static Date getSentDate(Blob content) throws IOException {
        InputStream is = new BufferedInputStream(content.getInputStream());
        try {
            // inefficient, but must be done before creating the ParsedMessage
            return getSentDate(new InternetHeaders(is));
        } catch (MessagingException e) {
            return null;
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    public static Date getSentDate(InternetHeaders ih) throws MessagingException {
	String s = ih.getHeader("Date", null);
	if (s != null) {
            try {
                return new MailDateFormat().parse(s);
	    } catch (ParseException pex) {
		return null;
	    }
	}
	return null;
    }

    private ImapCredentials getCredentials() {
        return handler.getCredentials();
    }

    /** APPEND message part, either literal data or IMAP URL. */
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
            if (literal != null)
                return literal.getInputStream();
            else
                return url.getContentAsStream(handler, handler.getCredentials(), tag).getSecond();
        }

        void cleanup() {
            if (literal != null)
                literal.cleanup();
        }
    }
}
