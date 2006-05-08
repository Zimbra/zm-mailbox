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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import org.apache.commons.logging.Log;

public class OzUtil {

    public static String toString(ByteBuffer bb) {
        return "[" + bb.position() + "," + bb.limit() + "," + bb.capacity() + "]";
    }
    
    public static String byteBufferDebugDump(String bufferName, ByteBuffer orig, boolean flip) {
        ByteBuffer flipped = orig.duplicate();
        if (flip) {
            flipped.flip();
        }

        StringBuilder sb = new StringBuilder(flipped.remaining() * 5);
        
        sb.append(bufferName);
        if (flip) {
            sb.append(" [flipped]");
        } else {
            sb.append(" [unflipped]");
        }
        sb.append(" pos=").append(flipped.position());
        if (flip) sb.append('/').append(orig.position());
        sb.append(" lim=").append(flipped.limit());
        if (flip) sb.append('/').append(orig.limit());
        sb.append(" cap=").append(flipped.capacity());
        if (flip) sb.append('/').append(orig.capacity());
        sb.append(" rem=").append(flipped.hasRemaining());
        if (flip) sb.append('/').append(orig.hasRemaining());
        sb.append('\n');

        int n = 1;
        int remaining = flipped.remaining();
        sb.append("[").append(intToDecString(0, 10, ' ')).append("] ");
        
        while (flipped.hasRemaining()) {
            int ch = (flipped.get() & 0xFF);
            if (ch >= 33 && ch <= 126) {
                sb.append(" " + (char)ch);
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == ' ') {
                sb.append("\\b");
            } else {
                sb.append(intToHexString(ch, 2, '0'));
            }
            if (n != remaining) {
                if ((n % 22) == 0) {
                    sb.append('\n');
                    sb.append("[").append(intToDecString(n, 10, ' ')).append("] ");
                } else {
                    sb.append(' ');
                }
            }
            n++;
        }
        return sb.toString();
    }

    public static String intToHexString(int value, int width, char pad) {
        StringBuilder sb = new StringBuilder(width);
        String sv = Integer.toHexString(value).toUpperCase();
        int numPad = width - sv.length();
        for (int i = 0; i < numPad; i++) {
            sb.append(pad);
        }
        sb.append(sv);
        return sb.toString();
    }

    public static String intToDecString(int value, int width, char pad) {
        StringBuilder sb = new StringBuilder(width);
        String sv = Integer.toString(value);
        int numPad = width - sv.length();
        for (int i = 0; i < numPad; i++) {
            sb.append(pad);
        }
        sb.append(sv);
        return sb.toString();
    }

    /** Caller must lock selectionkey, or this method may except. */
    static void logKey(Log log, SelectionKey selectionKey, String where) {
        if (selectionKey.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug(where +
                          " interest=" + OzUtil.opsToString(selectionKey.interestOps()) + 
                          " ready=" + OzUtil.opsToString(selectionKey.readyOps()) + 
                          " key=" + Integer.toHexString(selectionKey.hashCode()));
            }
        } else {
            log.warn(where + " invalid key=" + Integer.toHexString(selectionKey.hashCode()));
        }
    }

    private static String opsToString(int ops) {
        StringBuilder sb = new StringBuilder();
        if ((ops & SelectionKey.OP_READ) != 0) {
            sb.append("READ,");
        }
        if ((ops & SelectionKey.OP_ACCEPT) != 0) {
            sb.append("ACCEPT,");
        }
        if ((ops & SelectionKey.OP_CONNECT) != 0) {
            sb.append("CONNECT,");
        }
        if ((ops & SelectionKey.OP_WRITE) != 0) {
            sb.append("WRITE,");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }
    
}
