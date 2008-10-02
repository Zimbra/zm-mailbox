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

import com.zimbra.cs.mailclient.ParseException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Comparator;
import java.util.Collections;
import java.io.IOException;

/**
 * IMAP FETCH response data:
 *
 * msg-att         = "(" (msg-att-dynamic / msg-att-static)
 *                   *(SP (msg-att-dynamic / msg-att-static)) ")"
 *
 * msg-att-dynamic = "FLAGS" SP "(" [flag-fetch *(SP flag-fetch)] ")"
 *                   ; MAY change for a message
 *
 * msg-att-static  = "ENVELOPE" SP envelope / "INTERNALDATE" SP date-time /
 *                   "RFC822" [".HEADER" / ".TEXT"] SP nstring /
 *                   "RFC822.SIZE" SP number /
 *                   "BODY" ["STRUCTURE"] SP body /
 *                   "BODY" section ["<" number ">"] SP nstring /
 *                   "UID" SP uniqueid
 *                   ; MUST NOT change for a message
 */
public final class MessageData {
    private Flags flags;
    private Envelope envelope;
    private Date internalDate;
    private ImapData rfc822Header;
    private ImapData rfc822Text;
    private long rfc822Size = -1;
    private BodyStructure bodyStructure;
    private List<Body> bodySections;
    private long uid = -1;

    public static final Comparator<MessageData> UID_ORDER = 
        new Comparator<MessageData>() {
            public int compare(MessageData md1, MessageData md2) {
                return (int) (md1.getUid() - md2.getUid());
            }
        };

    public static final Comparator<MessageData> REVERSE_UID_ORDER =
        Collections.reverseOrder(UID_ORDER);
    
    private static final SimpleDateFormat INTERNALDATE_FORMAT =
        new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);

    public static MessageData read(ImapInputStream is) throws IOException {
        MessageData md = new MessageData();
        md.readResponse(is);
        return md;
    }
    
    private void readResponse(ImapInputStream is) throws IOException {
        is.skipChar('(');
        do {
            readAttribute(is);
        } while (is.match(' '));
        is.skipChar(')');
    }

    private void readAttribute(ImapInputStream is) throws IOException {
        // Need to special case BODY[] since '[' is also a valid atom char
        String s = is.readChars(Chars.FETCH_CHARS);
        if (s.length() == 0) {
            throw new ParseException("Zero-length attribute");
        }
        Atom attr = new Atom(s);
        CAtom cattr = attr.getCAtom();
        if (cattr == CAtom.BODY && is.peek() == '[') {
            if (bodySections == null) {
                bodySections = new ArrayList<Body>();
            }
            bodySections.add(Body.read(is));
            return;
        }
        is.skipChar(' ');
        switch (attr.getCAtom()) {
        case FLAGS:
            flags = Flags.read(is);
            break;
        case ENVELOPE:
            envelope = Envelope.read(is);
            break;
        case INTERNALDATE:
            internalDate = readInternalDate(is);
            break;
        case RFC822_HEADER:
            rfc822Header = is.readNStringData();
            break;
        case RFC822_TEXT:
            rfc822Text = is.readNStringData();
            break;
        case RFC822_SIZE:
            rfc822Size = is.readNumber();
            break;
        case BODYSTRUCTURE:
            bodyStructure = BodyStructure.read(is, true);
            break;
        case BODY:
            bodyStructure = BodyStructure.read(is, false);
            break;
        case UID:
            uid = is.readNZNumber();
            break;
        default:
            throw new ParseException("Invalid message data attribute: " + attr);
        }
    }

    private static Date readInternalDate(ImapInputStream is) throws IOException {
        String s = is.readQuoted().toString().trim();
        if (s.length() == 0) {
            // Workaround for YMail IMAP server issue where INTERNALDATE is
            // sometimes missing (bug 31818). Return null in this case so the
            // caller can choose a different value.
            return null;
        }
        synchronized (INTERNALDATE_FORMAT) {
            try {
                return INTERNALDATE_FORMAT.parse(s);
            } catch (java.text.ParseException e) {
                throw new ParseException("Invalid INTERNALDATE value: " + s);
            }
        }
    }

    public Flags getFlags() { return flags; }
    public Envelope getEnvelope() { return envelope; }
    public Date getInternalDate() { return internalDate; }
    public ImapData getRfc822Header() { return rfc822Header; }
    public ImapData getRfc822Text() { return rfc822Text; }
    public long getRfc822Size() { return rfc822Size; }
    public BodyStructure getBodyStructure() { return bodyStructure; }
    public long getUid() { return uid; }
    
    public Body[] getBodySections() {
        return bodySections.toArray(new Body[bodySections.size()]);
    }

    public void addFields(MessageData md) {
        if (md.flags != null) flags = md.flags;
        if (md.envelope != null) envelope = md.envelope;
        if (md.internalDate != null) internalDate = md.internalDate;
        if (md.rfc822Header != null) rfc822Header = md.rfc822Header;
        if (md.rfc822Text != null) rfc822Text = md.rfc822Text;
        if (md.rfc822Size != -1) rfc822Size = md.rfc822Size;
        if (md.bodyStructure != null) bodyStructure = md.bodyStructure;
        if (md.bodySections != null) bodySections = md.bodySections;
        if (md.uid != -1) uid = md.uid;
    }
    
    public void dispose() {
        if (rfc822Header != null) rfc822Header.dispose();
        if (rfc822Text != null) rfc822Text.dispose();
        if (bodySections != null) {
            for (Body body : bodySections) body.dispose();
        }
    }
}
