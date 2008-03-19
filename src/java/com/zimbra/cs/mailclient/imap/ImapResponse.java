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

import java.io.IOException;
import java.util.ArrayList;

/**
 * IMAP response message.
 */
public final class ImapResponse {
    private String mTag;        // Response tag
    private long mNumber = -1;  // Optional message number
    private CAtom mCode;        // Response code (or status)
    private Object mData;       // Optional response data

    public static final String CONTINUATION = "+";
    public static final String UNTAGGED = "*";

    public static ImapResponse read(ImapInputStream is) throws IOException {
        ImapResponse ir = new ImapResponse();
        ir.readResponse(is);
        return ir;
    }

    private void readResponse(ImapInputStream is) throws IOException {
        mTag = is.readText(' ');
        is.skipChar(' ');
        if (mTag.equals(CONTINUATION)) {
            mData = is.readText();
        } else if (mTag.equals(UNTAGGED)) {
            readUntagged(is);
        } else if (Chars.isTag(mTag)) {
            readTagged(is);
        } else {
            throw new ParseException("Invalid response tag: " + mTag);
        }
        is.skipCRLF();
    }

    private void readUntagged(ImapInputStream is) throws IOException {
        Atom code = is.readAtom();
        mNumber = code.getNumber();
        if (mNumber != -1) {
            is.skipChar(' ');
            code = is.readAtom();
        }
        mCode = code.getCAtom();
        switch (mCode) {
        case OK: case BAD: case NO: case BYE:
            is.skipChar(' ');
            mData = ResponseText.read(is);
            break;
        case CAPABILITY:
            is.skipChar(' ');
            mData = Capabilities.read(is);
            break;           
        case FLAGS:
            // "FLAGS" SP flag-list
            is.skipChar(' ');
            mData = Flags.read(is);
            break;
        case LIST: case LSUB:
            // "LIST" SP mailbox-list / "LSUB" SP mailbox-list
            // mailbox-list    = "(" [mbx-list-flags] ")" SP
            //                   (DQUOTE QUOTED-CHAR DQUOTE / nil) SP mailbox
            is.skipChar(' ');
            mData = MailboxList.read(is);
            break;
        case SEARCH:
            // "SEARCH" *(SP nz-number)
            if (is.match(' ')) {
                mData = readSearchData(is);
            }
            break;
        case STATUS:
            // "STATUS" SP mailbox SP "(" [status-att-list] ")"
            mData = MailboxStatus.read(is);
            break;
        case FETCH:
            // message-data    = nz-number SP ("EXPUNGE" / ("FETCH" SP msg-att))
            // msg-att         = "(" (msg-att-dynamic / msg-att-static)
            //                    *(SP (msg-att-dynamic / msg-att-static)) ")"
            is.skipChar(' ');
            mData = MessageData.read(is);
            break;
        case EXISTS: case RECENT: case EXPUNGE:
            break;
        default:
            throw new ParseException("Unknown response code: " + code);
        }
    }

    private Long[] readSearchData(ImapInputStream is) throws IOException {
        ArrayList<Long> numbers = new ArrayList<Long>();
        do {
            numbers.add(is.readNZNumber());
        } while (is.match(' '));
        return numbers.toArray(new Long[numbers.size()]);
    }
    
    private void readTagged(ImapInputStream is) throws IOException {
        Atom code = is.readAtom();
        mCode = code.getCAtom();
        is.skipChar(' ');
        switch (mCode) {
        case OK: case NO: case BAD:
            mData = ResponseText.read(is);
            break;
        default:
            throw new ParseException("Invalid tagged response code: " + code);
        }
    }

    public boolean isContinuation() {
        return CONTINUATION.equals(mTag);
    }

    public boolean isUntagged() {
        return UNTAGGED.equals(mTag);
    }

    public boolean isTagged() {
        return !isContinuation() && !isUntagged();
    }

    public String getTag() {
        return mTag;
    }

    public long getNumber() {
        return mNumber;
    }

    public CAtom getCode() {
        return mCode;
    }

    public Object getData() {
        return mData;
    }

    public boolean isOK()  { return mCode == CAtom.OK; }
    public boolean isBAD() { return mCode == CAtom.BAD; }
    public boolean isNO()  { return mCode == CAtom.NO; }
    public boolean isBYE() { return mCode == CAtom.BYE; }
    
    public boolean isStatus() {
        switch (mCode) {
        case OK: case BAD: case NO: case BYE:
            return true;
        }
        return false;
    }

    public boolean isFailure() {
        return isStatus() && !isOK();
    }

    public ResponseText getResponseText() {
        return isStatus() ? (ResponseText) mData : null;
    }

    public String getContinuation() {
        return isContinuation() ? (String) mData : null;
    }
    
    public void dispose() {
        if (mCode == CAtom.FETCH) {
            ((MessageData) mData).dispose();
        }
    }
}
