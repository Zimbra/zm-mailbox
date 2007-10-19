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
import java.util.LinkedList;

/**
 * Wraps an <tt>InputStream</tt> and reads an LMTP message
 * from the current position in the stream.  Stops reading
 * after encountering <tt>CRLF.CRLF</tt>.
 * 
 * @author bburtin
 */
public class LmtpMessageInputStream extends InputStream {

    private static final int CR = 13;
    private static final int LF = 10;
    private static final int[] EOM = new int[] { CR, LF, '.', CR, LF };
    private static final int EOMLEN = EOM.length;

    private InputStream mIn;
    // We start our state as though \r\n was already matched - so if
    // the first line is ".\r\n" we recognize that as end of message.
    private int mMatched = 1;
    private boolean mInitialPhantomMatch = true;
    private int mMessageSize = 0;
    private boolean mDone = false;
    private LinkedList<Integer> mBuffer = new LinkedList<Integer>();
    
    public LmtpMessageInputStream(InputStream in, String prefix) {
        mIn = in;
        if (prefix != null) {
            byte[] bytes = prefix.getBytes();
            for (int i = 0; i < bytes.length; i++) {
                mBuffer.add((int) bytes[i]);
            }
            mMessageSize = bytes.length;
        }
    }
    
    public int getMessageSize() {
        return mMessageSize;
    }
    
    @Override
    public int read() throws IOException {
        if (!mBuffer.isEmpty()) {
            return mBuffer.remove();
        }
        if (mDone) {
            return -1;
        }
        
        while (true) {
            int ch = mIn.read();

            if (ch == -1) {
                throw new IOException("EOF when looking for <CR><LF>.<CR><LF>");
            }
            
            if (ch == EOM[mMatched + 1]) {
                mMatched++;
                if (mMatched == (EOMLEN-1)) {
                    // see bug 6326 and RFC 2821 section 4.1.1.4 for why we need
                    // to preserve the CRLF that was part of the <CRLF>.<CRLF>
                    mBuffer.add(LF);
                    mMessageSize += 2;
                    mDone = true;
                    return CR;
                } else {
                    continue; // match more characters
                }
            }
            
            // Flush sequence that started looking like EOM, but wasn't.
            if (mMatched > -1) {
                
                int flushFrom = 0;
                if (mInitialPhantomMatch) {
                    mInitialPhantomMatch = false;
                    flushFrom = 2;
                }

                for (int i = flushFrom; i <= mMatched; i++) {
                    if (i == 2) {
                        // We encountered "\r\n." but it did not lead to EOM.
                        // Swallow "." so we end up removing SMTP transparency.
                        continue;
                    }
                    mBuffer.add(EOM[i]);
                    mMessageSize++;
                }
                
                // Reset match counter.
                mMatched = -1;
                
                // We might be at the beginning of EOM.
                if (ch == EOM[0]) {
                    mMatched++;
                    continue;
                }
            }

            mBuffer.add(ch);
            mMessageSize++;
            return mBuffer.remove();
        }
    }
}
