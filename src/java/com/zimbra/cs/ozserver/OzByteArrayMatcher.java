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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OzByteArrayMatcher implements OzMatcher {
   
    private static Log mLog = LogFactory.getLog(OzByteArrayMatcher.class);

    public static final byte CR = 13;
    public static final byte LF = 10;
    public static final byte DOT = '.';
    
    public static final byte[] CRLF = new byte[]{ CR, LF };
    public static final byte[] CRLFDOTCRLF = new byte[] { CR, LF, DOT, CR, LF };

    private final byte[] mMatchSequence;
    private final int mMatchSequenceLength;
    private int mMatched;
    
    public OzByteArrayMatcher(byte[] endSequence) {
        mMatchSequence = endSequence;
        mMatchSequenceLength = endSequence.length;
        mMatched = 0;
    }
    
    public boolean match(ByteBuffer buf) {
        assert(mMatched < mMatchSequenceLength);
        final boolean trace = true; // mLog.isTraceEnabled(); 
        
        int n = buf.remaining();
        if (trace) mLog.trace("new bytes to look at=" + n + ", already matched=" + mMatched);
        
        StringBuilder tsb;
        if (trace) tsb = new StringBuilder("byte array matcher trace ");
        
        for (int i = 0; i < n; i++) {
            byte b = buf.get();
            
            if (trace) {
                if (b >= 32 && b <=126) tsb.append("'" + (char)b + "'/"); 
                if (trace) tsb.append((int)b + " ");
            }

            if (mMatchSequence[mMatched] == b) {
                mMatched++;
                if (trace) tsb.append("+" + mMatched + " ");
                if (mMatched == mMatchSequenceLength) {
                    if (trace) mLog.trace(tsb.toString());
                    return true;
                }
            } else {
                mMatched = 0; // break the match
                if (mMatchSequence[mMatched] == b) { // but now does it match start of sequence?
                    mMatched++;
                    if (trace) tsb.append("+" + mMatched + " ");
                    if (mMatched == mMatchSequenceLength) {
                        if (trace) mLog.trace(tsb.toString());
                        return true;
                    }
                }
            }
        }
        if (trace) mLog.trace(tsb.toString());
        return false;
    }

    public void reset() {
        mMatched = 0;
    }

    public int trailingTrimLength() {
        assert(matched());
        return mMatched;
    }

    public boolean matched() {
        return mMatched == mMatchSequenceLength;
    }
}
