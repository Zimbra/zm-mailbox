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
        case UIDNEXT: case UIDVALIDITY: case UNSEEN:
            is.skipChar(' ');
            data = is.readNZNumber();
            break;
        case BADCHARSET:
            if (is.match(' ')) {
                data = readCharset(is);
            }
            break;
        case PERMANENTFLAGS:
            is.skipChar(' ');
            data = Flags.read(is);
            break;
        case CAPABILITY:
            is.skipChar(' ');
            data = ImapCapabilities.read(is);
            break;
        default:
            if (is.match(' ')) {
                data = is.readText(']');
            }
        }
        is.skipChar(']');
    }

    private String[] readCharset(ImapInputStream is) throws IOException {
        is.skipChar(' ');
        ArrayList<String> cs = new ArrayList<String>();
        do {
            cs.add(is.readAString());
        } while (is.match(' '));
        is.skipChar(')');
        return cs.toArray(new String[cs.size()]);
    }
           
    public Atom getCode() { return code; }
    public Object getData() { return data; }
    public String getText() { return text; }
}
