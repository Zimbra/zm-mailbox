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
    private String tag;         // Response tag
    private long number = -1;   // Optional message number
    private CAtom code;         // Response code or status
    private Object data;        // Optional response data

    public static final String CONTINUATION = "+";
    public static final String UNTAGGED = "*";

    public static ImapResponse read(ImapInputStream is) throws IOException {
        ImapResponse ir = new ImapResponse();
        ir.readResponse(is);
        return ir;
    }

    private void readResponse(ImapInputStream is) throws IOException {
        tag = is.readText(' ');
        is.skipChar(' ');
        if (tag.equals(CONTINUATION)) {
            data = is.readText();
        } else if (tag.equals(UNTAGGED)) {
            readUntagged(is);
        } else if (Chars.isTag(tag)) {
            readTagged(is);
        } else {
            throw new ParseException("Invalid response tag: " + tag);
        }
        is.skipCRLF();
    }

    private void readUntagged(ImapInputStream is) throws IOException {
        Atom code = is.readAtom();
        number = code.getNumber();
        if (number != -1) {
            is.skipChar(' ');
            code = is.readAtom();
        }
        this.code = code.getCAtom();
        switch (this.code) {
        case OK: case BAD: case NO: case BYE:
            is.skipChar(' ');
            data = ResponseText.read(is);
            break;
        case CAPABILITY:
            is.skipChar(' ');
            data = Capabilities.read(is);
            break;           
        case FLAGS:
            // "FLAGS" SP flag-list
            is.skipChar(' ');
            data = Flags.read(is);
            break;
        case LIST: case LSUB:
            // "LIST" SP mailbox-list / "LSUB" SP mailbox-list
            // mailbox-list    = "(" [mbx-list-flags] ")" SP
            //                   (DQUOTE QUOTED-CHAR DQUOTE / nil) SP mailbox
            is.skipChar(' ');
            data = MailboxList.read(is);
            break;
        case SEARCH:
            // "SEARCH" *(SP nz-number)
            if (is.match(' ')) {
                data = readSearchData(is);
            }
            break;
        case STATUS:
            // "STATUS" SP mailbox SP "(" [status-att-list] ")"
            data = Mailbox.readStatus(is);
            break;
        case FETCH:
            // message-data    = nz-number SP ("EXPUNGE" / ("FETCH" SP msg-att))
            // msg-att         = "(" (msg-att-dynamic / msg-att-static)
            //                    *(SP (msg-att-dynamic / msg-att-static)) ")"
            is.skipChar(' ');
            data = MessageData.read(is);
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
        Atom atom = is.readAtom();
        code = atom.getCAtom();
        is.skipChar(' ');
        switch (code) {
        case OK: case NO: case BAD:
            data = ResponseText.read(is);
            break;
        default:
            throw new ParseException("Invalid tagged response code: " + atom);
        }
    }

    public boolean isContinuation() {
        return CONTINUATION.equals(tag);
    }

    public boolean isUntagged() {
        return UNTAGGED.equals(tag);
    }

    public boolean isTagged() {
        return !isContinuation() && !isUntagged();
    }

    public String getTag() {
        return tag;
    }

    public long getNumber() {
        return number;
    }

    public CAtom getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }

    public boolean isOK()  { return code == CAtom.OK; }
    public boolean isBAD() { return code == CAtom.BAD; }
    public boolean isNO()  { return code == CAtom.NO; }
    public boolean isBYE() { return code == CAtom.BYE; }
    
    public boolean isStatus() {
        switch (code) {
        case OK: case BAD: case NO: case BYE:
            return true;
        }
        return false;
    }

    public boolean isWarning() {
        return isNO() && !isTagged();
    }

    public boolean isError() {
        return isBAD() || (isNO() && isTagged());
    }

    public ResponseText getResponseText() {
        return isStatus() ? (ResponseText) data : null;
    }

    public String getContinuation() {
        return isContinuation() ? (String) data : null;
    }
    
    public void dispose() {
        if (code == CAtom.FETCH) {
            ((MessageData) data).dispose();
        }
    }
}
