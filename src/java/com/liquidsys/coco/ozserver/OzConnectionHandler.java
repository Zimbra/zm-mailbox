package com.liquidsys.coco.ozserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.liquidsys.coco.util.LiquidLog;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

public class OzConnectionHandler implements Runnable {

    private OzProtocolHandler mProtocolHandler;

    private OzServer mServer;
    
    private SocketChannel mChannel;
    
    private OzMatcher mMatcher;

    private SelectionKey mSelectionKey; 

    OzProtocolHandler getProtocolHandler() {
        return mProtocolHandler;
    }

    private void logSelectionKey(String where) {
        OzUtil.logSelectionKey(mSelectionKey, mId, where);
    }
    
    private void enableReadInterest() {
        int iops = mSelectionKey.interestOps();
        mSelectionKey.interestOps(iops | SelectionKey.OP_READ);
        logSelectionKey("enabled read interest");
        mSelectionKey.selector().wakeup();
    }
    
    private void disableReadInterest() {
        int iops = mSelectionKey.interestOps();
        mSelectionKey.interestOps(iops & (~SelectionKey.OP_READ));
        logSelectionKey("disabled read interest");
    }
    
    private void enableWriteInterest() {
        int iops = mSelectionKey.interestOps();
        mSelectionKey.interestOps(iops | SelectionKey.OP_WRITE);
        logSelectionKey("enabled write interest");
        mSelectionKey.selector().wakeup();
    }
    
    private void disableWriteInterest() {
        int iops = mSelectionKey.interestOps();
        logSelectionKey("disabled write interest");
        mSelectionKey.interestOps(iops & (~SelectionKey.OP_WRITE));
    }
    
    private static SynchronizedInt mIdCounter = new SynchronizedInt(0);
    
    private int mId;

    private boolean mInvalid;
    
    public OzConnectionHandler(OzServer server, SocketChannel channel) throws IOException {
        mId = mIdCounter.increment();
        mChannel = channel;
        mServer = server;
        mReadBuffer = OzBufferManager.getBuffer();
        mSelectionKey = channel.register(server.getSelector(), 0, this); 
        mProtocolHandler = server.newProtocolHandler();
        mProtocolHandler.handleConnect(this);
        if (mChannel.isOpen()) {
            enableReadInterest();
        }
    }
    
    public void close() {
        Runnable task = new Runnable() {
            public void run() {
                LiquidLog.ozserver.info("closing cid=" + mId);
                OzBufferManager.returnBuffer(mReadBuffer);
                try {
                    mChannel.close();
                } catch (IOException ioe) {
                    LiquidLog.ozserver.warn("error closing cid=" + mId, ioe);
                }
                mSelectionKey.cancel();
            }
        };
        synchronized (this) {
            mInvalid = true;
        }
        mServer.runTaskInIoThread(task);
    }
    
    public void setMatcher(OzMatcher matcher) {
        mMatcher = matcher;
    }

    private ByteBuffer mReadBuffer;

    public void run() {
        try {
            read();
        } catch (IOException ioe) {
        	close();
        }
    }

    
    private void read() throws IOException {
        // This method runs in the IOThread.  Note that we disable read interest here, and
        // not in the worker thread, so that we don't get another ready notification
        // before the worker has had a chance to disable the read interest.
        
        if (LiquidLog.ozserver.isDebugEnabled()) { 
            LiquidLog.ozserver.debug(OzUtil.byteBufferToString("cid=" + mId + " read buffer at start", mReadBuffer, true));
        }
        
        int bytesRead = mChannel.read(mReadBuffer);
        
        if (LiquidLog.ozserver.isDebugEnabled()) { 
            LiquidLog.ozserver.debug(OzUtil.byteBufferToString("cid=" + mId + " read buffer after read", mReadBuffer, true));
        }
        
        assert(bytesRead == mReadBuffer.position());
        
        if (mServer.getSnooper().snoopReads()) {
            mServer.getSnooper().read(this, bytesRead, mReadBuffer);
        }
        
        if (bytesRead == -1) {
            LiquidLog.ozserver.info("cid=" + mId + " client closed channel " + mChannel);
            mProtocolHandler.handleEOF(this);
            close();
            return;
        }
        
        if (bytesRead == 0) {
            LiquidLog.ozserver.warn("cid=" + mId + " got 0 bytes on supposedly read ready channel " + mChannel);
            return;
        }

        int handledBytes = 0;
        
        // Create a copy which we can trash... (this does not copy underlying buffer)
        ByteBuffer matchBuffer = mReadBuffer.duplicate();
        matchBuffer.flip();
        
        // We may have read more than one PDU 
        while (matchBuffer.position() < matchBuffer.limit()) {
            int matchStart = matchBuffer.position();
            
            if (LiquidLog.ozserver.isDebugEnabled()) {
                LiquidLog.ozserver.debug(OzUtil.byteBufferToString("cid=" + mId + " matching", matchBuffer, false));
            }
            
            int matchEnd = mMatcher.match(matchBuffer);
            
            if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("cid=" + mId + " match returned " + matchEnd);
            
            if (matchEnd == -1) {
                break;
            }
            
            ByteBuffer pdu = mReadBuffer.duplicate();
            pdu.position(matchStart);
            pdu.limit(matchBuffer.position());
            mMatcher.trim(pdu);
            if (mServer.getSnooper().snoopInputs()) {
                mServer.getSnooper().input(this, pdu, true);
            }
            mProtocolHandler.handleInput(this, pdu, true);
            handledBytes = matchEnd;
        }
        
        if (handledBytes == 0) {
            if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("no bytes handled and no match");
            if (mReadBuffer.hasRemaining()) {
                if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("no bytes handled, no match, but there is room in buffer");
            } else {
                if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("no bytes handled, no match, and buffer is full, overflowing");
                assert(mReadBuffer.limit() == mReadBuffer.position());
                assert(mReadBuffer.limit() == mReadBuffer.capacity());
                mReadBuffer.flip();
                if (mServer.getSnooper().snoopInputs()) {
                    mServer.getSnooper().input(this, mReadBuffer, false);
                }
                mProtocolHandler.handleInput(this, mReadBuffer, false);
                mReadBuffer.clear();
            }
        } else {
            if (mReadBuffer.position() == handledBytes) {
                if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("handled all bytes in input, clearing buffer (no compacting)");
                mReadBuffer.clear();
            } else {
                if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("not all handled, compacting "  + (mReadBuffer.position() - handledBytes) + " bytes");
                mReadBuffer.position(handledBytes);
                mReadBuffer.compact();
            }
        }
        if (mChannel.isOpen()) {
            enableReadInterest();
        }
        return;
    }

    void handleRead() throws IOException {
        disableReadInterest();
        mServer.execute(this);
    }

    public void writeAscii(String data, boolean addCRLF) throws IOException {
        byte[] bdata = new byte[data.length() + (addCRLF ? 2 : 0)];
        int n = data.length();
        for (int i = 0; i < n; i++) {
            int ch = data.charAt(i);
            if (ch > 127) {
                throw new IllegalArgumentException("expecting ASCII got " + ch + " at " + i + " in '" + data + "'");
            }
            bdata[i] = (byte)ch;
        }
        if (addCRLF) {
            bdata[n] = OzByteArrayMatcher.CR;
            bdata[n+1] = OzByteArrayMatcher.LF;
        }
        write(ByteBuffer.wrap(bdata));
    }
    
    public void write(ByteBuffer data) throws IOException {
        synchronized (mWriteBuffers) {
            mWriteBuffers.add(data);
            enableWriteInterest();
        }
    }

    List mWriteBuffers = new LinkedList();

    // TODO two possible DoS problems - too many write buffers
    // and underlying tcp buffer being too big
    
    void handleWrite() throws IOException {
        // It is possible that more than one buffer can be written
        // so we call channel write until we fail to write fully
        while (true) {
            synchronized (mWriteBuffers) {
                if (mWriteBuffers.isEmpty()) {
                    // nothing more to write
                    disableWriteInterest();
                    return;
                }

                ByteBuffer data = (ByteBuffer)mWriteBuffers.remove(0);

                assert(data != null);
                assert(data.hasRemaining());
                
                if (mServer.getSnooper().snoopWrites()) {
                    mServer.getSnooper().write(this, data);
                }
                int wrote = mChannel.write(data);
                if  (mServer.getSnooper().snoopWrites()) {
                    mServer.getSnooper().wrote(this, wrote);
                }
                
                if (data.hasRemaining()) {
                    // If not all data was written, stop - we will write again
                    // when we get called at a later time when available for
                    // write. Put the buffer back in the list so whatever is
                    // remaining can be written later.  Note that we do not
                    // clear write interest here.
                    if (LiquidLog.ozserver.isDebugEnabled()) LiquidLog.ozserver.debug("incomplete write, adding buffer back");
                    mWriteBuffers.add(0, data); 
                    break;
                }
            }
        }
    }

    public int getId() {
        return mId;
    }
}