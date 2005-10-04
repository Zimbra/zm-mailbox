/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import com.zimbra.cs.util.ZimbraLog;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

public class OzConnectionHandler {

    private OzProtocolHandler mProtocolHandler;

    private OzServer mServer;
    
    private SocketChannel mChannel;
    
    private OzMatcher mMatcher;

    private SelectionKey mSelectionKey; 

    OzProtocolHandler getProtocolHandler() {
        return mProtocolHandler;
    }

    private void logKey(String where) {
        OzUtil.logSelectionKey(mSelectionKey, mId, where);
    }
    
    private void enableReadInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops | SelectionKey.OP_READ);
            if (ZimbraLog.ozserver.isDebugEnabled()) logKey("enabled read interest");
        }
        mSelectionKey.selector().wakeup();
    }
    
    private void disableReadInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops & (~SelectionKey.OP_READ));
            if (ZimbraLog.ozserver.isDebugEnabled()) logKey("disabled read interest");
        }
    }   
    
    private void enableWriteInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops | SelectionKey.OP_WRITE);
            if (ZimbraLog.ozserver.isDebugEnabled()) logKey("enabled write interest"); 
        }
        mSelectionKey.selector().wakeup();
    }
    
    private void disableWriteInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops & (~SelectionKey.OP_WRITE));
            if (ZimbraLog.ozserver.isDebugEnabled()) logKey("disabled write interest"); 
        }
    }
    
    private static SynchronizedInt mIdCounter = new SynchronizedInt(0);
    
    private int mId;

    private ByteBuffer mReadBuffer;

    private final Object mReadBufferLock = new Object();

    OzConnectionHandler(OzServer server, SocketChannel channel) throws IOException {
        mId = mIdCounter.increment();
        mChannel = channel;
        mServer = server;
        mReadBuffer = server.getBufferPool().get();
        mSelectionKey = channel.register(server.getSelector(), 0, this); 
        mProtocolHandler = server.newProtocolHandler();
        mProtocolHandler.handleConnect(this);
        ZimbraLog.ozserver.info("connected cid=" + mId + " buf=" + mReadBuffer.hashCode() + " " + mChannel);
        enableReadInterest();
    }

    public void closeNow() {
        Runnable closeTask = new Runnable() {
            public void run() {
                ZimbraLog.ozserver.info("closing cid=" + mId);
                try {
                    mChannel.close();
                } catch (IOException ioe) {
                    ZimbraLog.ozserver.warn("error closing cid=" + mId, ioe);
                }
                mSelectionKey.cancel();
                synchronized (mReadBufferLock) {
                    if (mReadBuffer != null) {
                        mServer.getBufferPool().recycle(mReadBuffer);
                    }
                    mReadBuffer = null;
                }
            }
        };
        mServer.runTaskInServerThread(closeTask);
    }
    
    public void setMatcher(OzMatcher matcher) {
        mMatcher = matcher;
    }

    private HandleDisconnectTask mHandleDisconnectTask = new HandleDisconnectTask();
    
    private class HandleDisconnectTask implements Runnable {
    	public void run() {
    		mProtocolHandler.handleDisconnect(OzConnectionHandler.this, true);
    		closeNow();
    	}
    }
    
    private HandleReadTask mHandleReadTask = new HandleReadTask();
    
    private class HandleReadTask implements Runnable {
        public void run() {
            Exception e = null;
            try {
                synchronized (mReadBufferLock) {
                    if (mReadBuffer == null) {
                        ZimbraLog.ozserver.info("connection cid=" + mId + " ready to read, but already closed");
                    } else {
                        processReadBufferLocked();
                    }
                }
            } catch (IOException ioe) {
                e = ioe;
            } catch (CancelledKeyException cke) {
                e = cke;
            }
            if (e != null) {
                if (mChannel.isOpen()) {
                    ZimbraLog.ozserver.warn("exception during read cid=" + mId, e);
                    closeNow();
                } else {
                    // someone else may have closed the channel - a shutdown or client went away, etc.
                    if (ZimbraLog.ozserver.isDebugEnabled()) {
                        ZimbraLog.ozserver.debug("ignorable " + e + " when reading, connection already closed cid=" + mId, e); 
                    }
                }
            }
        }
    }

    private void processReadBufferLocked() throws IOException {
    	boolean trace = ZimbraLog.ozserver.isTraceEnabled(); 
        
        if (trace) ZimbraLog.ozserver.trace(OzUtil.byteBufferToString("cid=" + mId + " read buffer at start", mReadBuffer, true));
        
        int bytesRead = mChannel.read(mReadBuffer);
        
        if (trace) ZimbraLog.ozserver.trace(OzUtil.byteBufferToString("cid=" + mId + " read buffer after read", mReadBuffer, true));
        
        assert(bytesRead == mReadBuffer.position());
        
        if (mServer.getSnooper().snoopReads()) {
            mServer.getSnooper().read(this, bytesRead, mReadBuffer);
        }
        
        if (bytesRead == -1) {
            ZimbraLog.ozserver.info("cid=" + mId + " client closed channel " + mChannel);
            mServer.execute(mHandleDisconnectTask);
            return;
        }
        
        if (bytesRead == 0) {
            ZimbraLog.ozserver.warn("cid=" + mId + " got 0 bytes on supposedly read ready channel " + mChannel);
            return;
        }

        int handledBytes = 0;
        
        // Create a copy which we can trash.  (duplicate() does not
        // copy underlying buffer)
        ByteBuffer matchBuffer = mReadBuffer.duplicate();
        matchBuffer.flip();
        
        // We may have read more than one PDU 
        while (matchBuffer.position() < matchBuffer.limit()) {
            int matchStart = matchBuffer.position();
            
            if (trace) {
                ZimbraLog.ozserver.trace(OzUtil.byteBufferToString("cid=" + mId + " matching", matchBuffer, false));
            }
            
            int matchEnd = mMatcher.match(matchBuffer);
            
            if (ZimbraLog.ozserver.isDebugEnabled()) {
            	ZimbraLog.ozserver.debug("cid=" + mId + " match returned " + matchEnd);
            }
            
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
            if (ZimbraLog.ozserver.isDebugEnabled()) ZimbraLog.ozserver.debug("no bytes handled and no match");
            if (mReadBuffer.hasRemaining()) {
                if (ZimbraLog.ozserver.isDebugEnabled()) ZimbraLog.ozserver.debug("no bytes handled, no match, but there is room in buffer");
            } else {
                if (ZimbraLog.ozserver.isDebugEnabled()) ZimbraLog.ozserver.debug("no bytes handled, no match, and buffer is full, overflowing");
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
                if (ZimbraLog.ozserver.isDebugEnabled()) ZimbraLog.ozserver.debug("handled all bytes in input, clearing buffer (no compacting)");
                mReadBuffer.clear();
            } else {
                if (ZimbraLog.ozserver.isDebugEnabled()) ZimbraLog.ozserver.debug("not all handled, compacting "  + (mReadBuffer.position() - handledBytes) + " bytes");
                mReadBuffer.position(handledBytes);
                mReadBuffer.compact();
            }
        }
        enableReadInterest();
        return;
    }

    void doRead() throws IOException {
        // This method runs in the server thread.  Note that we disable
        // read interest here, and not in the worker thread, so that
        // we don't get another ready notification before the worker
        // will get a chance to run and disable read interest.
        disableReadInterest();
        mServer.execute(mHandleReadTask);
    }

    public int getId() {
        return mId;
    }

    /**
     * Buffers that need to written out, when write side of the connection is ready.
     */
    List mWriteBuffers = new LinkedList();

    /**
     * Always looked at with mWriteBuffers locked.
     */
    private boolean mCloseAfterWrite = false;

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

    /**
     * This method runs in the server thread.
     * 
     * TODO two possible problems - too many write buffers
     * and underlying tcp buffer being too big
     */
    void doWrite() throws IOException {
        // It is possible that more than one buffer can be written
        // so we call channel write until we fail to write fully
        while (true) {
            synchronized (mWriteBuffers) {
                if (mWriteBuffers.isEmpty()) { 
                    // nothing more to write
                	disableWriteInterest();
                	if (mCloseAfterWrite) {
                		closeNow();
                	}
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
                    if (ZimbraLog.ozserver.isDebugEnabled()) ZimbraLog.ozserver.debug("incomplete write, adding buffer back");
                    mWriteBuffers.add(0, data); 
                    break;
                }
            }
        }
    }

    /**
     * Close after all pending writes.
     */
    public void close() {
        disableReadInterest();
        synchronized (mWriteBuffers) {
            mCloseAfterWrite = true;
            if (mWriteBuffers.isEmpty()) {
                disableWriteInterest();
                closeNow();
            }
        }
    }
}
