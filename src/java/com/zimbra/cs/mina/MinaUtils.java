package com.zimbra.cs.mina;

import java.nio.ByteBuffer;

public final class MinaUtils {
    public static final byte CR = '\r';
    public static final byte LF = '\n';
    public static final byte DOT = '.';

    public static final int INITIAL_CAPACITY = 32;
    
    /*
     * Copies bytes from 'fromBuf' to 'toBuf' up to and including a terminating
     * newline character. If no newline character was found then all remaining
     * source bytes are copied. The destination buffer is resized as needed.
     */
    public static ByteBuffer copyLine(ByteBuffer toBuf, ByteBuffer fromBuf) {
        int pos = findLF(fromBuf);
        if (pos == -1) {
            // No end of line found, so just add remaining bytes to buffer
            return expand(toBuf, fromBuf.remaining()).put(fromBuf);
        }
        int len = pos - fromBuf.position();
        ByteBuffer bb = fromBuf.slice();
        bb.limit(len);
        fromBuf.position(pos + 1);
        return expand(toBuf, len).put(bb);
    }

    /*
     * If specified buffer is a line ending with LF then return the
     * line as a string otherwise return null. The returned string does
     * not include the trailing LF or any preceding CR.
     */
    public static String getLine(ByteBuffer bb) {
        int pos = bb.position() - 1;
        if (pos >= 0 && bb.get(pos) == LF) {
            while (pos > 0 && bb.get(--pos) == CR) {}
            bb.duplicate().flip().limit(pos);
            return getString(bb);
        }
        return null;
    }

    public static ByteBuffer expand(ByteBuffer bb, int size) {
        if (bb == null) {
            bb = ByteBuffer.allocate(Math.max(size, INITIAL_CAPACITY));
        } else if (size > bb.remaining()) {
            int capacity = (bb.capacity() * 3) / 2 + 1;
            ByteBuffer tmp = ByteBuffer.allocate(
                Math.max(capacity, bb.position() + size));
            bb.flip();
            tmp.put(bb);
            bb = tmp;
        }
        return bb;
    }

    public static String getString(ByteBuffer bb) {
        int len = bb.remaining();
        char[] cs = new char[len];
        for (int i = 0; i < len; i++) {
            cs[i] = (char) ((int) bb.get(i) & 0xff);
        }
        return new String(cs);
    }


    public static int findLF(ByteBuffer bb) {
        int limit = bb.limit();
        for (int i = bb.position(); i < limit; i++) {
            if (bb.get(i) == LF) return i;
        }
        return -1;
    }
}
