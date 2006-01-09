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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;

import com.zimbra.cs.util.ZimbraLog;

public class OzConnection {

    private OzConnectionHandler mConnectionHandler;

    private Log mLog;

    private OzServer mServer;
    
    private SocketChannel mChannel;
    
    private OzMatcher mMatcher;

    private SelectionKey mSelectionKey; 

    private static AtomicInteger mIdCounter = new AtomicInteger(0);
    
    private static AtomicInteger mTaskCounter = new AtomicInteger(0);
    
    private final int mId;

    private final String mIdString;
    
    private final String mRemoteAddress;

    private final Object mReadLock = new Object();
    
    private final Object mWriteLock = new Object(); 
    
    private boolean mClosed;
    
    private final boolean mDebug;
    
    private final boolean mTrace;

    private boolean mReadPending;
    
    private boolean mWritePending;
    
    private ConnectTask mConnectTask;
    
    private DisconnectTask mDisconnectTask;
    
    private ReadTask mReadTask;
    
    private WriteTask mWriteTask;
    
    OzConnection(OzServer server, SocketChannel channel) throws IOException {
        mId = mIdCounter.incrementAndGet();
        mIdString = Integer.toString(mId);
        
        mRemoteAddress = channel.socket().getInetAddress().getHostAddress();
        mChannel = channel;
        mServer = server;
        mClosed = false;
        mLog = mServer.getLog();
        mDebug = mLog.isDebugEnabled();
        mTrace = mLog.isTraceEnabled();
        mSelectionKey = channel.register(server.getSelector(), 0, this); 

        mConnectTask = new ConnectTask();
        mReadTask = new ReadTask();
        mWriteTask = new WriteTask();
        mDisconnectTask = new DisconnectTask();
        
        mConnectTask.schedule();
    }

    public Log getLog() {
        return mLog;
    }
    
    private OzFilter mFilter;
    
    synchronized void setFilter(OzFilter filter) {
        mFilter = filter;
    }

    public void closeConnection() {
        try {
            synchronized (mSelectionKey) {
                mLog.info("closing channel");
                mChannel.close();
            }
        } catch (IOException ioe) {
            mLog.warn("exception closing channel, ignoring and continuing", ioe);
        } finally {
            if (mDebug && mClosed) mLog.debug("duplicate close detected");
            mClosed = true;
        }
    }

    void addToNDC() {
        ZimbraLog.addToContext("t", Thread.currentThread().getName());
        ZimbraLog.addIpToContext(getRemoteAddress());
        ZimbraLog.addConnectionIdToContext(getIdString());
    }

    void clearFromNDC() {
        ZimbraLog.clearContext();
    }
    
    static private String opsToString(int ops) {
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
    
    /* Caller must lock selectionkey. */
    static void logKey(Log log, SelectionKey selectionKey, String where) {
        if (selectionKey.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug(where +
                          " interest=" + opsToString(selectionKey.interestOps()) + 
                          " ready=" + opsToString(selectionKey.readyOps()) + 
                          " key=" + Integer.toHexString(selectionKey.hashCode()));
            }
        } else {
            log.warn(where + " invalid key=" + Integer.toHexString(selectionKey.hashCode()));
        }
    }

    public void enableReadInterest() {
        synchronized (mReadLock) {
            if (mReadPending) {
                if (mTrace) mLog.trace("noop enable read interest - read already pending");
                return;
            }
            if (mClosed) {
                if (mTrace) mLog.trace("noop enable read interest - channel already closed"); 
                return;
            }

            synchronized (mSelectionKey) {
                int iops = mSelectionKey.interestOps();
                mSelectionKey.interestOps(iops | SelectionKey.OP_READ);
                logKey(mLog, mSelectionKey, "enabled read interest");
            }
            mSelectionKey.selector().wakeup();
            mReadPending = true;
        }
    }
    
    private void disableReadInterest() {
        synchronized (mSelectionKey) {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops & (~SelectionKey.OP_READ));
            logKey(mLog, mSelectionKey, "disabled read interest");
        }
    }   
    
    private void enableWriteInterest() {
        synchronized (mWriteLock) {
            if (mWritePending) {
                if (mTrace) mLog.trace("skipping enable write interest - write already pending");
                return;
            }
            if (!mChannel.isOpen()) {
                if (mTrace) mLog.trace("skipping enable write interest - channel already closed");
                return;
            }
            synchronized (mSelectionKey) {
                int iops = mSelectionKey.interestOps();
                mSelectionKey.interestOps(iops | SelectionKey.OP_WRITE);
                logKey(mLog, mSelectionKey, "enabled write interest"); 
            }
            mSelectionKey.selector().wakeup();
            mWritePending = true;
        }
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

    public OzMatcher getMatcher() {
        return mMatcher;
    }

    private abstract class Task implements Runnable {
        private String mName;
     
        Task(String name) {
            mName = name;
        }

        void schedule() {
            if (mDebug) mLog.debug("scheduling " + mName + " task");
            mServer.execute(this);
        }
        

        public void run() {
            try {
                if (mDebug) {
                    ZimbraLog.addToContext("op", mName);
                    ZimbraLog.addToContext("opid", new Integer(mTaskCounter.incrementAndGet()).toString());
                }
                addToNDC();
                if (mDebug) mLog.debug("starting " + mName);
                doTask();
            } finally {
                if (mDebug) mLog.debug("finished " + mName);
                clearFromNDC();
            }
        }
        
        protected abstract void doTask();
    }
    
    
    private class ConnectTask extends Task {
        ConnectTask() { super("connect"); }
        
        protected void doTask() {
            try {
                mLog.info("connected");
                mConnectionHandler = mServer.newConnectionHandler(OzConnection.this);
                mConnectionHandler.handleConnect();
            } catch (Throwable t) {
                mLog.warn("exception occurred handling connect; will close connection", t);
                closeConnection();
                return;
            }
        }
    }
    
    private class DisconnectTask extends Task {
        DisconnectTask() { super("disconnect"); }
    	
        protected void doTask() {
    	    boolean closed = false;
            try {
                mConnectionHandler.handleDisconnect();
                if (mFilter != null) {
                    mFilter.closeNow();
                } else {
                    closeConnection();
                }
                closed = true;
            } catch (Throwable t) {
                mLog.warn("exception occurred handling disconnect", t);
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
                    closeConnection();
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
    
    private class ReadTask extends Task {
        ReadTask() { super("read"); }

        public void doTask() {
            try {
                int bytesRead = -1;
                synchronized (mReadLock) {
                    if (mClosed) {
                        return;
                    }
                    ByteBuffer rbb = getReadByteBuffer();
                    if (mDebug) mLog.debug("obtained buffer " + OzUtil.intToHexString(rbb.hashCode(), 0, ' '));
                    if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("read buffer before channel read", rbb, true));
                    bytesRead = mChannel.read(rbb);
                    mReadPending = false;
                    if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("read buffer after channel read", rbb, true));
                    
                    assert(bytesRead == rbb.position());
                    
                    if (bytesRead == -1) {
                        if (mDebug) mLog.debug("channel read detected that client closed connection");
                        mDisconnectTask.schedule();
                        return;
                    }
                    
                    if (bytesRead == 0) {
                        mLog.warn("got no bytes on supposedly read ready channel");
                        return;
                    }
                    
                    processRead(rbb, false);

                }
            } catch (Throwable t) {
                if (mChannel.isOpen()) {
                    mLog.warn("exception occurred read; will close connection", t);
                } else {
                    // someone else may have closed the channel, eg, a shutdown or
                    // client went away, etc. Consider this case to be normal.
                    if (mDebug) {
                        mLog.debug("ignorable (" + t.getClass().getName() + ") when reading, connection already closed", t); 
                    }
                }
                closeConnection();
            }
        }
    }

    /* Do not call directly.  For use by filters. */
    public void processRead(ByteBuffer dataBuffer, boolean fromFilter) throws IOException {
        synchronized (mReadLock) {
            processReadLocked(dataBuffer, fromFilter);
        }
    }

    public void processReadLocked(ByteBuffer dataBuffer, boolean fromFilter) throws IOException {
        if (mTrace) mLog.trace("readbb before flip " + dataBuffer);
        dataBuffer.flip();
        if (mTrace) mLog.trace("readbb after flip " + dataBuffer);
        
        // We may have read more than one PDU 
        while (dataBuffer.hasRemaining()) {
            ByteBuffer pdu = dataBuffer.duplicate();
            int initialPosition = dataBuffer.position();

            // An iteration may result in new filter getting set, so check each time.
            if (!fromFilter && mFilter != null) {
                if (mDebug) mLog.debug("filter present, handing input to filter");
                if (mTrace) mLog.trace("readbb before compact " + dataBuffer);
                dataBuffer.compact();
                if (mTrace) mLog.trace("readbb after compact " + dataBuffer);
                mFilter.read();
                return;
            }
            
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("invoking matcher", dataBuffer, false));
            boolean matched = mMatcher.match(dataBuffer);
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("after matcher", dataBuffer, false));
            
            if (mDebug) mLog.debug("match returned " + matched);
            
            if (!matched) {
                dataBuffer.position(initialPosition);
                break;
            }
            
            pdu.position(initialPosition);
            pdu.limit(dataBuffer.position());
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("input matched=" + true, pdu, false));
            mConnectionHandler.handleInput(pdu, true);
        }

        if (dataBuffer.hasRemaining()) {
            if (!fromFilter && mFilter != null) {
                if (mDebug) mLog.debug("filter present (for unmatched portion), handing input to filter");
                if (mTrace) mLog.trace("readbb before compact " + dataBuffer);
                dataBuffer.compact();
                if (mTrace) mLog.trace("readbb after compact " + dataBuffer);
                mFilter.read();
                return;
            }

            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("input matched=" + false, dataBuffer, false));
            mConnectionHandler.handleInput(dataBuffer, false);
        }

        dataBuffer.clear();

        return;
    }

    void doReadReady() throws IOException {
        // This method runs in the server thread.  Note that we disable
        // read interest here, and not in the worker thread, so that
        // we don't get another ready notification before the worker
        // will get a chance to run and disable read interest.
        disableReadInterest();
        mReadTask.schedule();
    }

    void doWriteReady() throws IOException {
        disableWriteInterest();
        mWriteTask.schedule();
    }

    private class WriteTask extends Task {
        WriteTask() { super("write"); }
        
        protected void doTask() {
            try {
                boolean allWritten;
                synchronized (mWriteLock) {
                    allWritten = writeLocked();
                }
                if (mFilter != null && allWritten) {
                    mFilter.writeCompleted();
                }
            } catch (Throwable t) {
                mLog.info("exception occurred handling write", t);
                closeConnection();
            }
        }
        
        private boolean writeLocked() throws IOException {
            int totalWritten = 0;
            boolean allWritten = true;
            for (Iterator<ByteBuffer> iter = mWriteBuffers.iterator(); iter.hasNext(); iter.remove()) {
                ByteBuffer data = iter.next();
                assert(data != null);
                assert(data.hasRemaining());

                if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("channel write", data, false));
                int wrote = mChannel.write(data);
                totalWritten += wrote;
                if (mDebug) mLog.trace("channel wrote=" + wrote + " totalWritten=" + totalWritten);
                
                if (data.hasRemaining()) {
                    mWritePending = false;
                    // If not all data was written, stop - we will write again
                    // when we get called at a later time when available for
                    // write. Put the buffer back in the list so whatever is
                    // remaining can be written later.  Note that we do not
                    // clear write interest here.
                    if (mDebug) mLog.debug("incomplete write, adding buffer back");
                    allWritten = false;
                    enableWriteInterest();
                    break;
                }
            }

            if (totalWritten == 0) mLog.warn("wrote no bytes to a write ready channel");
            
            if (allWritten) {
                mWritePending = false;

                if (mCloseAfterWrite) {
                    closeConnection();
                }
            }            
            
            return allWritten;
        }
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
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("write handed to filter", wbb, false));
            mFilter.write(wbb, flush);
        } else {
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("write handed to channel", wbb, false));
            channelWrite(wbb, flush);
        }
    }

    /** For use by filters, do not call directly. */
    public void channelWrite(ByteBuffer wbb, boolean flush) {
        synchronized (mWriteLock) {
            mWriteBuffers.add(wbb);
            if (flush) {
                enableWriteInterest();
            }
        }
    }

    public boolean isWritePending() {
        synchronized (mWriteLock) {
            return mWritePending;
        }
    }
    
    public void close() {
        if (mFilter != null) {
            try {
                mFilter.close();
            } catch (IOException ioe) {
                mLog.warn("filter exception on close", ioe);
                channelClose();
            }
        } else {
            channelClose();
        }
    }

    /**
     * For use by filters.   Do not call directly.
     */
    public void channelClose() {
        synchronized (mReadLock) {
            if (mClosed) {
                return;
            }
            disableReadInterest();
        }
        synchronized (mWriteLock) {
            mCloseAfterWrite = true;

            if (mWriteBuffers.isEmpty()) {
                closeConnection();
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
