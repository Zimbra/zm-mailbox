/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;

/**
 * IMAP response text:
 *
 * resp-text       = ["[" resp-text-code "]" SP] text
 *
 * resp-text-code  = "ALERT" /
 *                   "BADCHARSET" [SP "(" astring *(SP astring) ")" ] /
 *                   capability-data / "PARSE" /
 *                   "PERMANENTFLAGS" SP "("
 *                   [flag-perm *(SP flag-perm)] ")" /
 *                   "READ-ONLY" / "READ-WRITE" / "TRYCREATE" /
 *                   "UIDNEXT" SP nz-number / "UIDVALIDITY" SP nz-number /
 *                   "UNSEEN" SP nz-number /
 *                   atom [SP 1*<any TEXT-CHAR except "]">]
 */
public final class ResponseText {
    private Atom code;     // response text code
    private Object data;   // optional response text data
    private String text;   // response text

    public static ResponseText read(ImapInputStream is) throws IOException {
        ResponseText rt = new ResponseText();
        is.skipSpaces();
        if (is.peek() == '[') {
            rt.readCode(is);
        }
        rt.text = is.readText();
        return rt;
    }

    private void readCode(ImapInputStream is) throws IOException {
        is.skipChar('[');
        code = is.readAtom();
        switch (code.getCAtom()) {
        case ALERT: case PARSE: case READ_ONLY: case READ_WRITE: case TRYCREATE:
            break;
        case UIDNEXT: case UIDVALIDITY:
            is.skipChar(' ');
            // RFC 3501 says these should both be nz-number but some servers
            // return 0 if mailbox is empty (bug 38521).
            data = is.readNumber();
            break;
        case UNSEEN:
            is.skipChar(' ');
            // RFC 3501 says this should be an nz-number but some servers
            // (i.e. GMail) return 0. 
            data = is.readNumber();
            break;
        case BADCHARSET:
            if (is.match(' ')) {
                is.skipSpaces();
                if (is.peekChar() == '(') {
                    data = readCharset(is);
                }
            }
            break;
        case PERMANENTFLAGS:
            is.skipChar(' ');
            data = Flags.read(is);
            break;
        case CAPABILITY:
            is.skipChar(' ');
            is.skipSpaces();
            data = ImapCapabilities.read(is);
            break;
        case APPENDUID:
            this.data = AppendResult.parse(is);
            break;
        case COPYUID:
            this.data = CopyResult.parse(is);
            break;
        default:
            if (is.match(' ')) {
                data = is.readText("]");
            }
        }
        is.skipSpaces();
        is.skipChar(']');
    }

    private String[] readCharset(ImapInputStream is) throws IOException {
        ArrayList<String> cs = new ArrayList<String>();
        is.skipChar('(');
        is.skipSpaces();
        while (!is.match(')')) {
            cs.add(is.readAString());
            is.skipSpaces();
        }
        return cs.toArray(new String[cs.size()]);
    }

    public Atom getCode() { return code; }
    public Object getData() { return data; }
    public String getText() { return text; }
    
    public CAtom getCCode() {
        return code != null ? code.getCAtom() : CAtom.UNKNOWN;
    }
}
