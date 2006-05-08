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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class OzSmtpTransparency {
    
    public static ByteBuffer apply(ByteBuffer orig) {
        
        ByteBuffer work = orig.duplicate();
        
        OzByteArrayMatcher matcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLFDOTCRLF, null);
        
        List positions = null;
        
        while (work.hasRemaining()) {
            if (matcher.match(work)) {
                if (positions == null) {
                    positions = new LinkedList();
                }
                positions.add(new Integer(work.position()));
            } else {
                break;
            }
            matcher.reset();
        }
        
        if (positions == null) {
            return orig;
        }
        
        
        ByteBuffer result = ByteBuffer.allocate(orig.remaining() + positions.size());
        int lastPosition = 0;
        for (Iterator iter = positions.iterator(); iter.hasNext();) {
            int i = ((Integer)iter.next()).intValue();
            ByteBuffer part = orig.duplicate();
            part.position(lastPosition);
            part.limit(i - 2);
            lastPosition = i;
            result.put(part);
            result.put(OzByteArrayMatcher.DOT);
            result.put(OzByteArrayMatcher.CR);
            result.put(OzByteArrayMatcher.LF);
        }
        ByteBuffer rest = orig.duplicate();
        rest.position(lastPosition);
        rest.limit(orig.limit());
        result.put(rest);
        return result;
    }

    public static void main(String[] args) throws IOException {

        testApply(false, "abcd");
        testApply(false, "abcd\r\n");
        testApply(false, "abcd\r\nabcd");
        testApply(false, "abcd\r\nabcd\r\n");
        testApply(false, "abcd.\r\n");
        testApply(false, "abcd.\r\n.");
        testApply(false, "abcd.\r\n.\r");
        testApply(false, "abcd.\r\nefgh");
        
        testApply(true, "\r\n.\r\n");
        testApply(true, "a\r\n.\r\n");
        testApply(true, "\r\n.\r\nb");
        testApply(true, "a\r\n.\r\nb");
        testApply(true, "a\r\n.\r\nb\r\n.\r\n");
        testApply(true, "a\r\n.\r\nb\r\n.\r\n\r\n.\r\n\r\n.\r\n");
        testApply(true, "a\r\n.\r\nb\r\na\r\n.\r\nb\r\na\r\n.\r\nb\r\na\r\n.\r\nb\r\na\r\n.\r\nb\r\na\r\n.\r\nb\r\n");
    }

    private static void testApply(boolean mustApply, String data) throws IOException {
        ByteBuffer in = ByteBuffer.wrap(data.getBytes());
        ByteBuffer result = apply(in);
        System.out.println("===input (apply expected: " + mustApply + ")");
        System.out.println(data);
        if (in == result) {
            if (mustApply) {
                System.err.println("failed, did not apply transparency where expected");
                System.exit(-1);
            }
            System.out.println("====PASS: No transparency needed (output suppressed)");
        } else {
            if (!mustApply) {
                System.err.println("failed, applied transparency where not required");
                System.exit(-1);
            }
            System.out.println("====PASS: Transparency applied");
            System.out.write(result.array());
        }
    }

}
