/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

class ImapPartSpecifier {
    String mCommand;
    String mPart;
    String mModifier;
    List   mHeaders;
    int    mOctetStart = -1, mOctetEnd = -1;

    ImapPartSpecifier(String cmd, String part, String modifier) {
        mCommand = cmd;  mPart = part;  mModifier = modifier;
    }
    ImapPartSpecifier(String cmd, String part, String modifier, int start, int count) {
        mCommand = cmd;  mPart = part;  mModifier = modifier;
        if (start >= 0 && count >= 0) {
            mOctetStart = start;  mOctetEnd = start + count;
        }
    }

    int getOctetStart(byte[] content) {
        return (mOctetStart == -1 ? 0 : Math.min(mOctetStart, content.length));
    }
    int getOctetEnd(byte[] content) {
        return (mOctetEnd == -1 ? content.length : Math.min(mOctetEnd, content.length));
    }

    private static final String[] NO_HEADERS = new String[0];

    ImapPartSpecifier setHeaders(List headers)  { mHeaders = headers;  return this; }
    String[] getHeaders() {
        if (mHeaders == null || mHeaders.isEmpty())
            return NO_HEADERS;
        String[] headers = new String[mHeaders.size()];
        for (int i = 0; i < mHeaders.size(); i++)
            headers[i] = (String) mHeaders.get(i);
        return headers;
    }

    public String toString() {
        StringBuffer response = new StringBuffer(mCommand);
        if (mCommand.equals("BODY")) {
            response.append('[').append(mPart).append(mPart.equals("") || mModifier.equals("") ? "" : ".").append(mModifier);
            if (mHeaders != null) {
                boolean first = true;  response.append(" (");
                for (Iterator it = mHeaders.iterator(); it.hasNext(); first = false)
                    response.append(first ? "" : " ").append(((String) it.next()).toUpperCase());
                response.append(')');
            }
            response.append(']');
            // 6.4.5: "BODY[]<0.2048> of a 1500-octet message will return
            //         BODY[]<0> with a literal of size 1500, not BODY[]."
            if (mOctetStart != -1)
                response.append('<').append(mOctetStart).append('>');
        }
        return response.toString();
    }
    
    void write(PrintStream ps, OutputStream os, byte[] content) throws IOException {
        if (content == null) {
            ps.print(this);  ps.print(" NIL");
        } else {
            int start = getOctetStart(content);
            int end   = getOctetEnd(content);
            ps.print(this);  ps.print(" {");  ps.print(end - start);  ps.write('}');
            if (os != null) {
                os.write(ImapHandler.LINE_SEPARATOR_BYTES);  os.write(content, start, end - start);
            }
        }
    }
    
    void write(PrintStream ps, byte[] content) {
        if (content == null) {
            ps.print(this);  ps.print(" NIL");
        } else {
            int start = getOctetStart(content);
            int end   = getOctetEnd(content);
            ps.print(this);
            ps.print(" {");
            ps.print(end - start);
            ps.write('}');
            ps.write(ImapHandler.LINE_SEPARATOR_BYTES, 0, ImapHandler.LINE_SEPARATOR_BYTES.length);
            ps.write(content, start, end - start);
        }
    }
}
