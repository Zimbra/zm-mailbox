/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * IMAP response message.
 */
public final class ImapResponse {
    private String tag;             // Response tag
    private long number = -1;       // Optional message number
    private Atom code;              // Response code or status
    private CAtom ccode;            // Response code or status as CAtom
    private Object data;            // Optional response data

    public static final String CONTINUATION = "+";
    public static final String UNTAGGED = "*";

    public static ImapResponse read(ImapInputStream is) throws IOException {
        ImapResponse res = new ImapResponse();
        res.readResponse(is);
        is.trace();
        return res;
    }

    private ImapResponse() {}

    private void readResponse(ImapInputStream is) throws IOException {
        tag = is.readText(' ');
        is.skipChar(' ');
        if (tag.equals(CONTINUATION)) {
            data = ResponseText.read(is);
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
        code = is.readAtom();
        number = code.getNumber();
        if (number != -1) {
            is.skipChar(' ');
            code = is.readAtom();
        }
        ccode = code.getCAtom();
        switch (ccode) {
        case OK: case BAD: case NO: case BYE:
            is.skipChar(' ');
            data = ResponseText.read(is);
            break;
        case CAPABILITY:
            if (is.match(' ')) {
                data = ImapCapabilities.read(is);
            } else {
                data = new ImapCapabilities();
            }
            break;
        case FLAGS:
            // "FLAGS" SP flag-list
            is.skipChar(' ');
            data = Flags.read(is);
            break;
        case LIST: case LSUB: case XLIST:
            // "LIST" SP mailbox-list / "LSUB" SP mailbox-list
            // mailbox-list    = "(" [mbx-list-flags] ")" SP
            //                   (DQUOTE QUOTED-CHAR DQUOTE / nil) SP mailbox
            is.skipChar(' ');
            data = ListData.read(is);
            break;
        case SEARCH: case SORT:
            // "SEARCH" *(SP nz-number)
            data = readSearchData(is);
            break;
        case STATUS:
            // "STATUS" SP mailbox SP "(" [status-att-list] ")"
            is.skipChar(' ');
            data = MailboxInfo.readStatus(is);
            break;
        case FETCH:
            // message-data    = nz-number SP ("EXPUNGE" / ("FETCH" SP msg-att))
            // msg-att         = "(" (msg-att-dynamic / msg-att-static)
            //                    *(SP (msg-att-dynamic / msg-att-static)) ")"
            is.skipChar(' ');
            data = MessageData.read(is, number);
            break;
        case EXISTS: case RECENT: case EXPUNGE:
            break;
        case ID:
            //bug 57859; at least one IMAP server incorrectly excludes space here
            is.skipOptionalChar(' ');
            data = IDInfo.read(is);
            break;
        case NOOP:
            break;
        default:
            throw new ParseException("Unknown response code: " + code);
        }
    }

    private List<Long> readSearchData(ImapInputStream is) throws IOException {
        ArrayList<Long> ids = new ArrayList<Long>();
        while (is.match(' ')) {
            is.skipSpaces();
            if (!is.isEOL()) {
                ids.add(is.readNZNumber());
            }
        }
        return ids;
    }

    private void readTagged(ImapInputStream is) throws IOException {
        code = is.readAtom();
        is.skipChar(' ');
        ccode = code.getCAtom();
        switch (ccode) {
        case OK: case NO: case BAD:
            data = ResponseText.read(is);
            break;
        default:
            throw new ParseException("Invalid tagged response code: " + code);
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

    public Atom getCode() {
        return code;
    }

    public CAtom getCCode() {
        return ccode;
    }

    public Object getData() {
        return data;
    }

    public boolean isOK()  { return ccode == CAtom.OK; }
    public boolean isBAD() { return ccode == CAtom.BAD; }
    public boolean isNO()  { return ccode == CAtom.NO; }
    public boolean isBYE() { return ccode == CAtom.BYE; }

    public boolean isStatus() {
        switch (ccode) {
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
        return (ResponseText) data;
    }

    public String getContinuation() {
        return isContinuation() ? getResponseText().getText() : null;
    }

    public void dispose() {
        if (data instanceof MessageData) {
            ((MessageData) data).dispose();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(tag);
        sb.append(' ');
        if (number != -1) sb.append(number).append(' ');
        if (code != null) sb.append(code);
        if (data != null) sb.append(" <data>");
        return sb.toString();
    }
}
