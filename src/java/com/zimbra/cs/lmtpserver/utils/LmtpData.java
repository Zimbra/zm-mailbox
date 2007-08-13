package com.zimbra.cs.lmtpserver.utils;

import java.nio.ByteBuffer;

import static com.zimbra.cs.mina.MinaUtils.*;

public class LmtpData {
    private ByteBuffer mBuffer; // Line or data buffer
    private int mMatched;       // Number of bytes in EOM sequence mMatched so far
    private boolean complete;
    
    public LmtpData(int size) {
        mBuffer = ByteBuffer.allocate(size);
        mMatched = 2; // Start as if CRLF already matched
    }

    public LmtpData() {
        this(512);
    }

    public void parse(ByteBuffer bb) {
        if (isComplete()) return;
        mBuffer = expand(mBuffer, bb.remaining());
        while (bb.hasRemaining()) {
            byte c = bb.get();
            mBuffer.put(c);
            if (mMatched > 0) {
                if (matches(c)) {
                    mMatched++;
                    if (mMatched == 5) { // end of message
                        // Skip trailing "." + CRLF
                        mBuffer.position(mBuffer.position() - 3);
                        if (mBuffer.position() == 0) {
                            // If data was only "." + CRLF, then include CRLF
                            // for consistency with LmtpInputStream.
                            mBuffer.put(CR).put(LF);
                        }
                        mBuffer.flip();
                        complete = true;
                        break;
                    }
                } else {
                    if (mMatched == 3) {
                        // Special handling for data transparency. If any line
                        // begins with '.' and is not followed immediately by
                        // CRLF to indicate EOM, the leading '.' is omitted.
                        mBuffer.position(mBuffer.position() - 2);
                        mBuffer.put(c);
                    }
                    mMatched = (c == CR) ? 1 : 0;
                }
            } else if (c == CR) {
                mMatched = 1; // Start new sequence
            }
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public void append(byte[] b, int off, int len) {
        mBuffer = expand(mBuffer, len);
        mBuffer.put(b, off, len);
    }

    public void append(byte[] b) {
        append(b, 0, b.length);
    }
    
    // Returns true if byte matches next expected character in EOM sequence
    private boolean matches(byte c) {
        switch (c) {
        case CR:    return mMatched == 3;
        case LF:    return mMatched == 1 || mMatched == 4;
        case DOT:   return mMatched == 2;
        default:    return false;
        }
    }

    public byte[] getBytes() {
        checkComplete();
        if (mBuffer == null) return null;
        if (mBuffer.limit() == mBuffer.capacity()) {
            return mBuffer.array(); // Buffer was correctly sized
        }
        ByteBuffer bb = mBuffer.duplicate();
        byte[] b = new byte[bb.limit()];
        bb.get(b, 0, b.length);
        return b;
    }

    public ByteBuffer getByteBuffer() {
        checkComplete();
        return mBuffer;
    }
    
    private void checkComplete() {
        if (!isComplete()) {
            throw new IllegalStateException("data is not complete");
        }
    }
}
