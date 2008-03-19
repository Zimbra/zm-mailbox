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
    private Flags mFlags;
    private Envelope mEnvelope;
    private Date mInternalDate;
    private ImapData mRfc822Header;
    private ImapData mRfc822Text;
    private long mRfc822Size = -1;
    private BodyStructure mBodyStructure;
    private List<Body> mBodySections;
    private long mUid = -1;

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
            if (mBodySections == null) {
                mBodySections = new ArrayList<Body>();
            }
            mBodySections.add(Body.read(is));
            return;
        }
        is.skipChar(' ');
        switch (attr.getCAtom()) {
        case FLAGS:
            mFlags = Flags.read(is);
            break;
        case ENVELOPE:
            mEnvelope = Envelope.read(is);
            break;
        case INTERNALDATE:
            mInternalDate = readInternalDate(is);
            break;
        case RFC822_HEADER:
            mRfc822Header = is.readNStringData();
            break;
        case RFC822_TEXT:
            mRfc822Text = is.readNStringData();
            break;
        case RFC822_SIZE:
            mRfc822Size = is.readNumber();
            break;
        case BODYSTRUCTURE:
            mBodyStructure = BodyStructure.read(is, true);
            break;
        case BODY:
            mBodyStructure = BodyStructure.read(is, false);
            break;
        case UID:
            mUid = is.readNZNumber();
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

    public Flags getFlags() { return mFlags; }
    public Envelope getEnvelope() { return mEnvelope; }
    public Date getInternalDate() { return mInternalDate; }
    public ImapData getRfc822Header() { return mRfc822Header; }
    public ImapData getRfc822Text() { return mRfc822Text; }
    public long getRfc822Size() { return mRfc822Size; }
    public BodyStructure getBodyStructure() { return mBodyStructure; }
    public long getUid() { return mUid; }
    public Body[] getBodySections() {
        return mBodySections.toArray(new Body[mBodySections.size()]);
    }

    public void dispose() {
        if (mRfc822Header != null) mRfc822Header.dispose();
        if (mRfc822Text != null) mRfc822Text.dispose();
        if (mBodySections != null) {
            for (Body body : mBodySections) body.dispose();
        }
    }
}
