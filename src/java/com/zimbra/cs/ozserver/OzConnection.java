/*
 * ***** Begin LICENSE BLOCK *****
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.zimbra.cs.util.ZimbraLog;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

public class OzConnection {

    private OzConnectionHandler mConnectionHandler;

    private Log mLog;

    private OzServer mServer;
    
    private SocketChannel mChannel;
    
    private OzMatcher mMatcher;

    private SelectionKey mSelectionKey; 

    private static SynchronizedInt mIdCounter = new SynchronizedInt(0);
    
    private final int mId;

    private final String mIdString;
    
    private final String mRemoteAddress;

    /* guards read operations and mClosed */
    private final Object mReadLock = new Object();
    
    private boolean mClosed;
    
    private final boolean mDebug;
    
    private final boolean mTrace;

    OzConnection(OzServer server, SocketChannel channel) throws IOException {
        mId = mIdCounter.increment();
        mIdString = Integer.toString(mId);
        
        mRemoteAddress = channel.socket().getInetAddress().getHostAddress();
        mChannel = channel;
        mServer = server;
        mClosed = false;
        try {
            addToNDC();
            mLog = mServer.getLog();
            mDebug = mLog.isDebugEnabled();
            mTrace = mLog.isTraceEnabled();
            mLog.info("connected");
            mSelectionKey = channel.register(server.getSelector(), 0, this); 
            mConnectionHandler = server.newConnectionHandler(this);
            
            try {
                /* TODO - this should be a task */
                mConnectionHandler.handleConnect();
            } catch (Throwable t) {
                mLog.warn("exception occurrent handling connect; will close connection", t);
                closeNow();
                return;
            }
            enableReadInterest();
        } finally {
            clearFromNDC();
        }
    }

    public Log getLog() {
        return mLog;
    }
    
    /* always access it through the get method which synchronizes - TODO revisit this synch and remove if possible */
    private OzFilter mFilter;
    
    synchronized void setFilter(OzFilter filter) {
        mFilter = filter;
    }
    
    private synchronized OzFilter getFilter() {
        return mFilter;
    }
    
    public void closeNow() {
        Runnable closeTask = new Runnable() {
            public void run() {
                try {
                    addToNDC();
                    run0();
                } finally {
                    clearFromNDC();
                }
            }
            
            private void run0() {
                mLog.info("closing");
                try {
                    if (mChannel.isOpen()) {
                        mChannel.close();
                    }
                    mSelectionKey.cancel();
                } catch (IOException ioe) {
                    mLog.warn("exception closing channel, ignoring and continuing", ioe);
                } finally {
                    synchronized (mReadLock) { 
                        if (mClosed && mDebug) mLog.debug("duplicate close detected");
                        mClosed = true;
                    }
                }
            }
        };
        
        mServer.executeInServerThread(closeTask);
    }

    void addToNDC() {
        ZimbraLog.addIpToContext(getRemoteAddress());
        ZimbraLog.addConnectionIdToContext(getIdString());
    }

    void clearFromNDC() {
        ZimbraLog.clearContext();
    }
    
    /* Caller must lock selectionkey! */
    static void logKey(Log log, SelectionKey selectionKey, String where) {
        if (selectionKey.isValid()) {
            log.debug(where +
                    " iops=" + selectionKey.interestOps() + 
                    " rops=" + selectionKey.readyOps() + 
                    " key=" + Integer.toHexString(selectionKey.hashCode()));
        } else {
            log.warn(where + " invalid key=" + Integer.toHexString(selectionKey.hashCode()));
        }
    }

    void enableReadInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops | SelectionKey.OP_READ);
            logKey(mLog, mSelectionKey, "enabled read interest");
        }
        mSelectionKey.selector().wakeup();
    }
    
    private void disableReadInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops & (~SelectionKey.OP_READ));
            logKey(mLog, mSelectionKey, "disabled read interest");
        }
    }   
    
    private void enableWriteInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops | SelectionKey.OP_WRITE);
            logKey(mLog, mSelectionKey, "enabled write interest"); 
        }
        mSelectionKey.selector().wakeup();
    }
    
    private void disableWriteInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops & (~SelectionKey.OP_WRITE));
            logKey(mLog, mSelectionKey, "disabled write interest"); 
        }
    }
    
    OzConnectionHandler getConnectionHandler() {
        return mConnectionHandler;
    }
    
    public void setMatcher(OzMatcher matcher) {
        mMatcher = matcher;
    }

    private HandleDisconnectTask mHandleDisconnectTask = new HandleDisconnectTask();
    
    private class HandleDisconnectTask implements Runnable {
    	public void run() {
    	    boolean closed = false;
            try {
                try {
                    addToNDC();
                    mConnectionHandler.handleDisconnect();
                    closeNow();
                    if (mFilter != null) {
                        mFilter.close();
                    }
                    closed = true;
                } finally {
                    clearFromNDC();
                }
            } catch (Throwable t) {
                mLog.warn("exception occurred while handling disconnect", t);
                if (!closed) {
                    /*
                     * Not catching any Throwables from closeNow()
                     * intentionally. closeNow() runs in server thread, so any
                     * exception it raises are ignored in the main server loop.
                     * If closeNow() failure is not being able to create the
                     * task object or to add that task object to the queue, that
                     * means the server is fubar - so don't bother to be
                     * graceful in a worker thread.
                     */
                    closeNow();
                }
            }
        }
    }
    
    private ByteBuffer getReadByteBuffer() {
        if (mFilter != null) {
            return mFilter.getReadBuffer();
        } else {
            return ByteBuffer.allocate(mServer.getReadBufferSize());
        }
    }
    
    private HandleReadTask mHandleReadTask = new HandleReadTask();
    
    private class HandleReadTask implements Runnable {
        public void run() {
            try {
                addToNDC();
                ByteBuffer rbb = getReadByteBuffer();
                mLog.info("obtained buffer " + OzUtil.intToHexString(rbb.hashCode(), 0, ' '));
                if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("read buffer at start", rbb, true));
                int bytesRead = -1;
                synchronized (mReadLock) {
                    bytesRead = mChannel.read(rbb);
                }
                if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("read buffer after channel read", rbb, true));
                
                assert(bytesRead == rbb.position());
                
                if (bytesRead == -1) {
                    if (mDebug) mLog.debug("channel read detected that client closed connection");
                    mServer.execute(mHandleDisconnectTask);
                    return;
                }
                
                if (bytesRead == 0) {
                    mLog.warn("got 0 bytes on supposedly read ready channel");
                    return;
                }
                
                if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("read from channel bytes=" + bytesRead, rbb, true));
                
                if (mFilter == null) {
                    if (mDebug) mLog.debug("no filter will process read bytes");
                    processRead(rbb);
                } else {
                    if (mDebug) mLog.debug("handing read bytes to filter");
                    mFilter.read();
                }
            } catch (Throwable t) {
                if (mChannel.isOpen()) {
                    mLog.warn("exception occured handling read; will close connection", t);
                } else {
                    // someone else may have closed the channel, eg, a shutdown or
                    // client went away, etc. Consider this case to be normal.
                    if (mDebug) {
                        mLog.debug("ignorable (" + t.getClass().getName() + ") when reading, connection already closed", t); 
                    }
                }
                closeNow();
            } finally {
                clearFromNDC();
            }
        }
    }

    /* Do not call directly.  For use by filters. */
    public void processRead(ByteBuffer dataBuffer) throws IOException { 
        int handledBytes = 0;
        
        // Create a copy which we can trash.  NB: duplicate() does not
        // copy underlying buffer.
        ByteBuffer matchBuffer = dataBuffer.duplicate();
        matchBuffer.flip();
        
        // We may have read more than one PDU 
        while (matchBuffer.position() < matchBuffer.limit()) {
            int matchStart = matchBuffer.position();
            
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("matching", matchBuffer, false));
            
            int matchEnd = mMatcher.match(matchBuffer);
            
            if (mDebug) mLog.debug("match returned " + matchEnd);
            
            if (matchEnd == -1) {
                break;
            }
            
            ByteBuffer pdu = dataBuffer.duplicate();
            pdu.position(matchStart);
            pdu.limit(matchBuffer.position());
            mMatcher.trim(pdu);
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("input matched=" + true, pdu, false));
            mConnectionHandler.handleInput(pdu, true);
            handledBytes = matchEnd;
        }
            
        if (handledBytes == 0) {
            if (mDebug) mLog.debug("no bytes handled and no match");
            if (dataBuffer.hasRemaining()) {
                if (mDebug) mLog.debug("no bytes handled, no match, but there is room in buffer");
            } else {
                if (mDebug) mLog.debug("no bytes handled, no match, and buffer is full, overflowing");
                assert(dataBuffer.limit() == dataBuffer.position());
                assert(dataBuffer.limit() == dataBuffer.capacity());
                dataBuffer.flip();
                if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("input matched=" + false, dataBuffer, false));
                mConnectionHandler.handleInput(dataBuffer, false);
                dataBuffer.clear();
            }
        } else {
            if (dataBuffer.position() == handledBytes) {
                if (mDebug) mLog.debug("handled all bytes in input, clearing buffer (no compacting)");
                dataBuffer.clear();
            } else {
                if (mDebug) mLog.debug("not all handled, compacting "  + (dataBuffer.position() - handledBytes) + " bytes");
                dataBuffer.position(handledBytes);
                dataBuffer.compact();
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

    public String getIdString() {
        return mIdString;
    }
    
    public String getRemoteAddress() {
        return mRemoteAddress;
    }
    
    /**
     * Buffers that need to written out, when write side of the connection is ready.
     */
    List<ByteBuffer> mWriteBuffers = new LinkedList<ByteBuffer>();

    /**
     * Always looked at with mWriteBuffers locked.
     */
    private boolean mCloseAfterWrite = false;

    public void writeAscii(String data, boolean addCRLF) throws IOException {
        writeAscii(data, addCRLF, true);
    }
    
    public void writeAscii(String data, boolean addCRLF, boolean flush) throws IOException {
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
        write(ByteBuffer.wrap(bdata), flush);
    }

    public void write(ByteBuffer wbb) throws IOException {
        write(wbb, true);
    }

    public void write(ByteBuffer wbb, boolean flush) throws IOException {
        // TODO even if flush has not been requested, we should flush if we
        // have too much data!
        if (mFilter != null) {
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("pre-filter outgoing", wbb, false));
            mFilter.write(wbb, flush);
        } else {
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("pre-filter outgoing", wbb, false));
            channelWrite(wbb, flush);
        }
    }

    /** For use by filters, do not call directly. */
    public void channelWrite(ByteBuffer wbb, boolean flush) {
        synchronized (mWriteBuffers) {
            mWriteBuffers.add(wbb);
            if (flush) {
                enableWriteInterest();
            }
        }
    }
    
    public boolean isWritePending() {
        synchronized (mWriteBuffers) {
            return !mWriteBuffers.isEmpty();
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
        int totalWritten = 0;
        while (true) {
            synchronized (mWriteBuffers) {
                if (mWriteBuffers.isEmpty()) {
                    disableWriteInterest();
                    if (totalWritten == 0) mLog.warn("wrote no bytes to a write ready channel");
                    if (mFilter != null) {
                        mFilter.wrote(totalWritten);
                    }
                    // nothing more to write
                	if (mCloseAfterWrite) {
                		closeNow();
                	}
                	return;
                }

                ByteBuffer data = (ByteBuffer)mWriteBuffers.remove(0);

                assert(data != null);
                assert(data.hasRemaining());
                
                if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("channel write", data, false));
                int wrote = mChannel.write(data);
                totalWritten += wrote;
                if (mDebug) mLog.trace("channel wrote=" + wrote + " totalWritten=" + totalWritten);
                
                if (data.hasRemaining()) {
                    // If not all data was written, stop - we will write again
                    // when we get called at a later time when available for
                    // write. Put the buffer back in the list so whatever is
                    // remaining can be written later.  Note that we do not
                    // clear write interest here.
                    if (mDebug) mLog.debug("incomplete write, adding buffer back");
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

    private Map mProperties = new HashMap();
    
    public String getProperty(String key, String defaultValue) {
        synchronized (mProperties) {
            String result = (String)mProperties.get(key);
            if (result == null) {
                result = mServer.getProperty(key, defaultValue);
            }
            return result;
        }
    }
    
    public void setProperty(String key, String value) {
        synchronized (mProperties) {
            mProperties.put(key, value);
        }
    }
}
