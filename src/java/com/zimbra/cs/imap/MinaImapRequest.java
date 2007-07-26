package com.zimbra.cs.imap;

import org.apache.mina.common.IoSession;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaHandler;

public class MinaImapRequest extends ImapRequest implements MinaRequest {
    private ByteBuffer buf;   // Buffer for current line or literal bytes
    private State state;      // Current request state
    private int count;        // Remaining byte count for current literal
    private long size;        // Current message size
    private boolean blocking; // Current literal is blocking continuation

    // Expecting command line, literal, message body, or the message is complete
    private enum State { LINE, LITERAL, COMPLETE }

    public MinaImapRequest(MinaHandler handler) {
        super((MinaImapHandler) handler);
        state = State.LINE;
    }

    public MinaImapHandler getHandler() {
        return (MinaImapHandler) mHandler;
    }

    public void parse(IoSession session, ByteBuffer bb) throws IOException {
        while (state != State.COMPLETE && bb.hasRemaining()) {
            switch (state) {
            case LINE:
                try {
                    parseLine(bb);
                } catch (ImapParseException e) {
                    getHandler().sendBAD(mTag, e.toString());
                }
                break;
            case LITERAL:
                parseLiteral(bb);
                break;
            }
        }
    }

    public boolean isComplete() {
        return state == State.COMPLETE;
    }

    private void parseLiteral(ByteBuffer bb) throws IOException {
        int n = Math.min(count, bb.remaining());
        buf.put((ByteBuffer) bb.slice().limit(n));
        bb.position(bb.position() + n);
        count -= n;
        if (count == 0) {
            mParts.add(buf.array());
            buf = null;
            state = State.LINE;
        } else if (blocking) {
            getHandler().sendContinuation("send literal data");
        }
    }

    private void parseLine(ByteBuffer bb)
            throws IOException, ImapParseException {
        int pos = findEOL(bb);
        if (pos == -1) {
            // No end of line found, so just add remaining bytes to buffer
            buf = put(buf, bb);
            return;
        }
        int len = pos - bb.position();
        ByteBuffer lbb = (ByteBuffer) bb.slice().limit(len);
        bb.position(pos + 1);
        if (lbb.hasRemaining() && lbb.get(len - 1) == '\r') lbb.limit(--len);
        if (buf != null) {
            // Append to accumulated line bytes
            lbb = put(buf, lbb);
            buf = null;
        }
        String line = getString(lbb);
        incrementSize(line.length());
        mParts.add(line);
        parseLine(line);
    }

    private void parseLine(String line) throws IOException, ImapParseException {
        int i;
        if (!line.endsWith("}") || (i = line.lastIndexOf('{')) == -1) {
            state = State.COMPLETE;
            return;
        }
        int j = line.length() - 2;
        if (line.charAt(j) == '+') {
            --j;
        } else {
            blocking = true;
            getHandler().sendContinuation("send literal data");
        }
        state = State.LITERAL;
        count = parseNumber(line, i + 1, j);
        if (count == -1) {
            throw new ImapParseException(mTag, "Bad literal format: " + line);
        }
        incrementSize(count);
        buf = ByteBuffer.allocate(count);
    }

    private void incrementSize(int increment) throws ImapParseException {
        // TODO Make sure this cannot wrap around
        size += increment;
        // TODO Can max request size be treated as a constant?
        if (size > getMaxRequestLength()) {
            throw new ImapParseException(mTag, "request too long");
        }
    }
    
    private static int parseNumber(String s, int i, int j) {
        int result = 0;
        while (i <= j) {
            int n = Character.digit(s.charAt(i++), 10);
            if (n == -1) return -1;
            result = result * 10 + n;
            if (result < 0) return -1; // Overflow
        }
        return result;
    }

    private static String getString(ByteBuffer bb) {
        int len = bb.remaining();
        char[] cs = new char[len];
        for (int i = 0; i < len; i++) {
            cs[i] = (char) ((int) bb.get(i) & 0xff);
        }
        return new String(cs);
    }

    private static ByteBuffer put(ByteBuffer buf, ByteBuffer bb) {
        buf = expand(buf, bb.remaining());
        return buf.put(bb);
    }

    private static ByteBuffer expand(ByteBuffer buf, int size) {
        if (buf == null) {
            buf = ByteBuffer.allocate(size);
        } else if (size > buf.remaining()) {
            ByteBuffer tmp = ByteBuffer.allocate(buf.position() + size);
            tmp.put((ByteBuffer) buf.flip());
            buf = tmp;
        }
        return buf;
    }

    private int findEOL(ByteBuffer bb) {
        int limit = bb.limit();
        for (int i = bb.position(); i < limit; i++) {
            if (bb.get(i) == '\n') return i;
        }
        return -1;
    }

    public byte[] readLiteral() throws ImapParseException {
        if (mIndex + 1 >= mParts.size()) {
            throw new ImapParseException(mTag, "no next literal");
        }
        Object part = mParts.get(mIndex + 1);
        if (!(part instanceof byte[]))
            throw new ImapParseException(mTag, "in string next not literal");
        mIndex += 2;
        mOffset = 0;
        return (byte[]) part;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Object part : mParts) {
            if (part instanceof String) {
                sb.append(part).append("\r\n");
            } else if (part instanceof byte[]) {
                byte[] b = (byte[]) part;
                try {
                    sb.append(new String(b, "US-ASCII"));
                } catch (UnsupportedEncodingException e) {
                    throw new InternalError();
                }
            } else {
                throw new AssertionError();
            }
        }
        return sb.toString();
    }
}
