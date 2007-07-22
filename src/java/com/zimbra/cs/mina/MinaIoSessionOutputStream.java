package com.zimbra.cs.mina;

import org.apache.mina.common.IoSession;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An output stream that writes to an associated MINA IoSession. This
 * implementation also provides buffering.
 */
public class MinaIoSessionOutputStream extends OutputStream {
    private final IoSession session;
    private ByteBuffer buffer;
    
    public MinaIoSessionOutputStream(IoSession session, int bufSize) {
        this.session = session;
        this.buffer = ByteBuffer.allocate(bufSize);
    }

    public MinaIoSessionOutputStream(IoSession session) {
        this(session, 1024);
    }
    
    @Override
    public synchronized void write(byte[] b, int off, int len)
            throws IOException {
        checkClosed();
        for (int total = 0; total < len; ) {
            int count = Math.min(len - total, buffer.remaining());
            buffer.put(b, off, count);
            if (!buffer.hasRemaining()) flushBytes();
            total += count;
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    @Override
    public synchronized void flush() throws IOException {
        checkClosed();
        flushBytes();
    }

    @Override
    public synchronized void close() throws IOException {
        flush();
    }
    
    private void flushBytes() throws IOException {
        if (buffer.position() <= 0) return;
        buffer.flip();
        session.write(buffer);
        buffer = ByteBuffer.allocate(buffer.capacity());
    }
    
    private void checkClosed() throws IOException {
        if (!session.isConnected()) {
            throw new IOException("Session has been closed"); 
        }
    }
}

