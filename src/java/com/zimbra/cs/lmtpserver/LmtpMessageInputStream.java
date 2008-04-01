/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.lmtpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.LinkedList;

/**
 * Wraps an <tt>InputStream</tt> and reads an LMTP message
 * from the current position in the stream according to the
 * rules specified in RFC 2821:
 * 
 * <ul>
 *   <li>If <tt>CRLF.CRLF</tt> is encountered, returns everything
 *     up to the first <tt>CRLF</tt>, strips the trailing <tt>.CRLF</tt>
 *     and stops reading.</li>
 *   <li>If <tt>CRLF.</tt> is encountered without a trailing <tt>CRLF</tt>,
 *     removes the <tt>.</tt> from the stream (transparency, section 4.5.2)</li>
 * </ul>
 * 
 * The transparency implementation allows a user-entered line
 * that begins with a dot.<p>
 *
 * @throws IOException if <tt>EOF</tt> is encountered before <tt>CRLF.CRLF</tt>
 * or there's an error reading from the stream.
 * @author bburtin
 */
public class LmtpMessageInputStream extends InputStream {

    private static final int CR = 13;
    private static final int LF = 10;

    private PushbackInputStream mIn;
    private int mMessageSize = 0;
    private boolean mDone = false;
    private LinkedList<Integer> mPrefix;
    
    // Default mGotCR and mGotLF to true.  The <CRLF> after the DATA command applies, according
    // to section 4.1.1.4 of RFC 2821.
    
    /**
     * <tt>true</tt> if the last character was <tt>CR</tt> or
     * the last two characters were <tt>CRLF</tt>.
     */
    private boolean mGotCR = true;
    
    /**
     * <tt>true</tt> if the last two characters were <tt>CRLF</tt>.
     */
    private boolean mGotLF = true;
    
    public LmtpMessageInputStream(InputStream in, String prefix) {
        mIn = new PushbackInputStream(in);
        
        if (prefix != null) {
            mPrefix = new LinkedList<Integer>();
            byte[] bytes = prefix.getBytes();
            for (byte b : bytes) {
                mPrefix.add((int) b);
            }
            mMessageSize = bytes.length;
        }
    }
    
    public int getMessageSize() {
        return mMessageSize;
    }

    @Override
    public int available() throws IOException {
        if (mDone) {
            return 0;
        }
        return mIn.available();
    }
    
    @Override
    public int read() throws IOException {
        if (mDone) {
            return -1;
        }
        
        // Return data from the prefix if it's available.
        if (mPrefix != null) {
            if (mPrefix.size() > 0) {
                return mPrefix.remove();
            } else {
                mPrefix = null;
            }
        }
        
        // Read the next character.
        int c = mIn.read();
        if (c == -1) {
            mDone = true;
            throw new IOException("End of stream encountered when looking for <CR><LF>.<CR><LF>");
        }
        
        if (c == CR) {
            mGotCR = true;
            mGotLF = false;
            mMessageSize++;
            return c;
        }
        if (c == LF) {
            if (mGotCR) {
                mGotLF = true;
            }
            mMessageSize++;
            return c;
        }
        if (!(mGotCR && mGotLF)) {
            mGotCR = false;
            mGotLF = false;
            mMessageSize++;
            return c;
        }
        
        // Got '<CRLF>'.
        if (c != '.') {
            mGotCR = false;
            mGotLF = false;
            mMessageSize++;
            return c;
        }
        
        // Got '<CRLF>.'.
        c = mIn.read();
        if (c == -1) {
            mDone = true;
            throw new IOException("End of stream encountered after '.' when looking for '<CR><LF>'");
        }
        if (c != CR) {
            // Strip the dot and return the next character (transparency).
            mGotCR = false;
            mGotLF = false;
            mMessageSize++;
            return c;
        }
        
        // Got '<CRLF>.<CR>'.
        c = mIn.read();
        if (c == -1) {
            mDone = true;
            throw new IOException("End of stream encountered after '<CR><LF>.<CR>' when looking for '<LF>'");
        }
        if (c != LF) {
            // Edge case: '<CRLF>.<CR>' followed by something other than LF.
            mIn.unread(c);
            mGotCR = true;
            mGotLF = false;
            mMessageSize++;
            return CR;
        }
        
        // Got '<CRLF>.<CRLF>'
        mDone = true;
        return -1;
    }
}
