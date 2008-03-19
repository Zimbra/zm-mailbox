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
        Atom attr = new Atom(is.readChars(Chars.FETCH_CHARS));
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

    public void dispose() {
        if (rfc822Header != null) rfc822Header.dispose();
        if (rfc822Text != null) rfc822Text.dispose();
        if (bodySections != null) {
            for (Body body : bodySections) body.dispose();
        }
    }
}
