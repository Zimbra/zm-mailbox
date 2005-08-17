package com.zimbra.cs.ozserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class OzSmtpTransparency {
    
    public static ByteBuffer apply(ByteBuffer orig) {
        
        ByteBuffer work = orig.duplicate();
        
        OzByteArrayMatcher matcher = new OzByteArrayMatcher(OzByteArrayMatcher.CRLFDOTCRLF);
        
        List positions = null;
        
        while (work.hasRemaining()) {
            int matchPos = matcher.match(work);
            if (matchPos == -1) {
                break;
            } else {
                if (positions == null) {
                    positions = new LinkedList();
                }
                positions.add(new Integer(matchPos));
            }
            matcher.clear();
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
