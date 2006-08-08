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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;

import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

public class OzConnection {

    private OzConnectionHandler mConnectionHandler;

    private final Log mLog;

    private OzServer mServer;
    
    private final SocketChannel mChannel;
    
    private OzMatcher mMatcher;

    private final SelectionKey mSelectionKey; 

    private static AtomicInteger mIdCounter = new AtomicInteger(0);
    
    private static AtomicInteger mTaskCounter = new AtomicInteger(0);
    
    private final int mId;

    private final String mIdString;
    
    private final String mRemoteAddress;

    private final Object mLock = new Object();
    
    private final boolean mDebug;
    
    private final boolean mTrace;

    private ConnectTask mConnectTask;
    
    private ReadTask mReadTask;
    
    private WriteTask mWriteTask;

    private ByteBuffer mReadBuffer;

    private final OzPlainFilter mPlainFilter;
    
    OzConnection(OzServer server, SocketChannel channel) throws IOException {
        mId = mIdCounter.incrementAndGet();
        mIdString = Integer.toString(mId);
        
        mRemoteAddress = channel.socket().getInetAddress().getHostAddress();
        mChannel = channel;
        mServer = server;
        mChannelClosed = false;
        mLog = mServer.getLog();
        mDebug = mLog.isDebugEnabled();
        mTrace = mServer.debugLogging();
        mSelectionKey = channel.register(server.getSelector(), 0, this); 
        
        mPlainFilter = new OzPlainFilter(); 
        addFilter(mPlainFilter, true);

        mConnectTask = new ConnectTask();
        mReadTask = new ReadTask();
        mWriteTask = new WriteTask();

        mConnectTask.schedule();
    }
    
    static {
        // Bug in JDK 1.5.0_06 - loading TeardownReason class from an exception
        // handler (only in some cases at that) results in NoClassDefFoundError
        if (System.currentTimeMillis() > 0) {
            Class c = TeardownReason.class;
        }
    };
    

    private List<OzFilter> mFilters = new ArrayList<OzFilter>();
    
    private AtomicInteger mFilterChangeId = new AtomicInteger();
    
    /**
     * Adds an IO filter at the top of the filter stack. IO model is a stack of
     * filters, with the top most filter in the stack handling all inbound
     * network packets and outbound application packets, while the bottom most
     * filter (the plain filter) handles all inbound application packets and
     * outbound network packets. The bottom most filter is builtin to the
     * framework and can not be removed - it is also very closely tied to the
     * connection.
     */
    public void addFilter(OzFilter filter) {
        addFilter(filter, false);
    }

    private void addFilter(OzFilter filter, boolean plainFilter) {
        synchronized (mLock) {
            OzFilter nextFilter = null;
            
            if (!plainFilter) {
                nextFilter = mFilters.get(0);
            }

            ensureReadBufferCapacity();
            
            mFilters.add(0, filter);
            filter.setNextFilter(nextFilter);
            mFilterChangeId.incrementAndGet();
        }
    }

    /**
     * Removes the top most filter.
     */
    public OzFilter removeFilter() {
        synchronized (mLock) {
            assert mFilters.size() != 0; // must always have atleast plain filter present.
            if (mFilters.size() == 1) {
                throw new IllegalStateException("attempt to remove plain filter");
            }
            mFilterChangeId.incrementAndGet();
            OzFilter removedFilter = mFilters.remove(0);
            removedFilter.setNextFilter(null);
            return removedFilter;
        }
    }
    
    public void close() {
        try {
            mFilters.get(0).close();
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError) {
                String msg = null;
                try {
                    msg = "OOME closing connection (" + mServer.mServerName + ")";
                } finally {
                    Zimbra.halt(msg);
                }
            }
            teardown(TeardownReason.CLOSE_EXCEPTION);
        }
    }

    enum TeardownReason { WRITE_FAILED, NORMAL, ABRUPT, ALARM_EXCEPTION, CLOSE_EXCEPTION; }

    private boolean mChannelClosed;

    private boolean isChannelClosed() {
        synchronized (mTeardownLock) {
            return mChannelClosed;
        }        
    }
    
    /**
     * A teardown is the final step of a close where we cleanup the socket and
     * have the protocol handler cleanup also.
     */ 
    void teardown(TeardownReason reason, boolean closeInServerThread) {
        synchronized (mTeardownLock) {
            try {
                mLog.info("teardown - " + reason);
                if (mChannelClosed) {
                    if (mDebug) mLog.debug("duplicate close (occurs on write exceptions)");
                    return;
                }
                if (mDebug) mLog.debug("scheduling close in server thread");
                if (closeInServerThread) {
                    mServer.schedule(mServerCloseChannelTask);
                } else {
                    try { 
                        mChannel.close();
                    } catch (IOException ioe) {
                        if (mDebug) mLog.debug("teardown exception in closing channel", ioe);
                    }
                }
                mChannelClosed = true;
                cancelAlarm();
            } catch (Throwable t) {
                if (t instanceof OutOfMemoryError) {
                    String msg = null;
                    try {
                        msg = "OOME in teardown connection (" + mServer.mServerName + ")";
                    } finally {
                        Zimbra.halt(msg);
                    }
                }
                mLog.warn("exception closing channel", t);
                mConnectionHandler.handleDisconnect();
            }
        }
    }

    private void teardown(TeardownReason reason) {
        teardown(reason, true);
    }
    
    /**
     * Close the underlying IO channel. 
     */
    private Object mTeardownLock = new Object();
    
    abstract class ServerTask {
        String mName;
        
        ServerTask(String name) {
            mName = name;
        }
        
        String getName() {
            return mName;
        }
        
        protected abstract void doTask() throws IOException;
        
        void run() {
            try {
                addToNDC();
                if (mDebug) mLog.debug("server task " + mName);
                doTask();
            } catch (Throwable t) {
                if (t instanceof OutOfMemoryError) {
                    String msg = null;
                    try {
                        msg = "OOME in server task " + mName + " (" + mServer.mServerName + ")";
                    } finally {
                        Zimbra.halt(msg);
                    }
                }
                mLog.warn("exception performing server task " + mName);
            } finally {
                clearFromNDC();
            }
        }
    }
    
    private ServerCloseChannel mServerCloseChannelTask = new ServerCloseChannel();
    private ServerRegisterRead mServerRegisterReadTask = new ServerRegisterRead();
    private ServerRegisterWrite mServerRegisterWriteTask = new ServerRegisterWrite();
    
    private class ServerCloseChannel extends ServerTask {
        ServerCloseChannel() { super("close"); }
        
        protected void doTask() throws IOException {
            mChannel.close();
        }
    }
    
    private class ServerRegisterRead extends ServerTask {
        ServerRegisterRead() { super("eread"); }

        protected void doTask() throws IOException {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops | SelectionKey.OP_READ);
            OzUtil.logKey(mLog, mSelectionKey, "registered read interest");
        }
    }   
    
    private class ServerRegisterWrite extends ServerTask {
        ServerRegisterWrite() { super("ewrite"); }

        protected void doTask() throws IOException {
            int iops = mSelectionKey.interestOps();
            mSelectionKey.interestOps(iops | SelectionKey.OP_WRITE);
            OzUtil.logKey(mLog, mSelectionKey, "registered write interest");
        }
    }

    void addToNDC() {
        //ZimbraLog.addToContext("t", Thread.currentThread().getName());
        ZimbraLog.addIpToContext(getRemoteAddress());
        ZimbraLog.addConnectionIdToContext(getIdString());
    }

    void clearFromNDC() {
        ZimbraLog.clearContext();
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

    private boolean mReadRequested;
    
    public void enableReadInterest() {
        synchronized (mLock) {
            mReadRequested = true;
        }
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
            synchronized (mLock) {
                if (mDebug) {
                    ZimbraLog.addToContext("op", mName);
                    ZimbraLog.addToContext("opid", new Integer(mTaskCounter.incrementAndGet()).toString());
                }
                addToNDC();
                if (mDebug) mLog.debug("starting " + mName);
                try {
                    if (isChannelClosed()) {
                        if (mDebug) mLog.debug("connection already closed, aborting " + mName);
                        return;
                    }
                    mReadRequested = false;
                    doTask();
                    if (mReadRequested) {
                        if (mDebug) mLog.debug("scheduling read register in server thread");
                        mServer.schedule(mServerRegisterReadTask);
                    }
                    if (mPlainFilter.isWritePending()) {
                        if (mDebug) mLog.debug("scheduling write register in server thread");
                        mServer.schedule(mServerRegisterWriteTask);
                    }
                } catch (Throwable t) {
                    if (t instanceof OutOfMemoryError) {
                        String msg = null;
                        try {
                            msg = "OOME in task " + mName + " ("+ mServer.mServerName + ")";
                        } finally {
                            Zimbra.halt(msg);
                        }
                    }
                    if (t instanceof IOException && isChannelClosed()) {
                        mLog.info("ignoring IOException on a closed channel with message: " + t.getMessage());
                    } else {
                        mLog.info("exception occurred during " + mName + " task", t);
                        teardown(TeardownReason.ABRUPT);
                    }
                } finally {
                    if (mDebug) mLog.debug("finished " + mName);
                    clearFromNDC();
                }
            }
        }
        
        protected abstract void doTask() throws IOException;
    }
    
    private class ConnectTask extends Task {
        ConnectTask() { super("connect"); }
        
        protected void doTask() throws IOException {
            mLog.info("connected");
            mConnectionHandler = mServer.newConnectionHandler(OzConnection.this);
            mConnectionHandler.handleConnect();
        }
    }
    
    private void unregisterReadInterest() {
        int iops = mSelectionKey.interestOps();
        mSelectionKey.interestOps(iops & (~SelectionKey.OP_READ));
        OzUtil.logKey(mLog, mSelectionKey, "unregistered read interest");
    }

    private void unregisterWriteInterest() {
        int iops = mSelectionKey.interestOps();
        mSelectionKey.interestOps(iops & (~SelectionKey.OP_WRITE));
        OzUtil.logKey(mLog, mSelectionKey, "unregistered write interest");
    }

    public static final int MINIMUM_READ_BUFFER_SIZE = 4096;
    
    private void ensureReadBufferCapacity() {
    	if (mReadBuffer == null) {
    		if (mDebug) mLog.debug("create new read buffer capacity=" + MINIMUM_READ_BUFFER_SIZE);
    		mReadBuffer = ByteBuffer.allocate(MINIMUM_READ_BUFFER_SIZE);
    		return;
    	}
    	
    	if (mReadBuffer.remaining() < MINIMUM_READ_BUFFER_SIZE) {
    		int oldCapacity = mReadBuffer.capacity();
    		int newCapacity = oldCapacity * 2;

    		ByteBuffer bb = ByteBuffer.allocate(newCapacity);
    		mReadBuffer.flip();
    		bb.put(mReadBuffer);
    		mReadBuffer = bb;
    		if (mDebug) mLog.debug("resized read buffer from " + oldCapacity + " to " + newCapacity);
    	}
    }
    
    private class ReadTask extends Task {
        ReadTask() { super("read"); }

        public void doTask() throws IOException {
            int bytesRead = -1;
            
            ensureReadBufferCapacity();
            
            String before = null;
            if (mDebug) before = OzUtil.toString(mReadBuffer);
            bytesRead = mChannel.read(mReadBuffer);
            if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("channel read buffer", mReadBuffer, true));
            if (mDebug) mLog.debug("channel read=" + bytesRead + " buffer: " + before + "->" + OzUtil.toString(mReadBuffer));

            if (bytesRead == -1) {
                throw new EOFException("socket channel read");
            }

            if (bytesRead == 0) {
                mLog.warn("no bytes on supposedly read ready channel re-enabling read interest");
                enableReadInterest();
                return;
            }
            
            mFilters.get(0).read(mReadBuffer);
        }
    }

    void doReadReady() throws IOException {
        // This method runs in the server thread.  Note that we disable
        // read interest here, and not in the worker thread, so that
        // we don't get another ready notification before the worker
        // will get a chance to run and disable read interest.
        unregisterReadInterest();
        mReadTask.schedule();
    }

    void doWriteReady() throws IOException {
        unregisterWriteInterest();
        mWriteTask.schedule();
    }

    private class WriteTask extends Task {
        WriteTask() { super("write"); }
        
        protected void doTask() throws IOException {
            mPlainFilter.flush();
        }
    }

    private static final int WRITE_BUFFER_COMPACTION_PERCENT = LC.nio_write_buffer_compaction_percent.intValue();
        
    private final class WriteQueue {
        private LinkedList<ByteBuffer> mWriteBuffers = new LinkedList<ByteBuffer>();
        int mCurrentCapacity = 0;
        int mCapacityMax = Integer.MAX_VALUE;

        boolean isEmpty() {
            return mWriteBuffers.isEmpty();
        }
        
        void clear() {
            mWriteBuffers.clear();
            mCurrentCapacity = 0;
        }
        
        void add(ByteBuffer bb) throws IOException {
            mWriteBuffers.add(bb);
            mCurrentCapacity += bb.capacity();
            // don't check for capacity on append - we try and
            // flush right away and at the time of the flush if
            // something could not be written then we will check
            // capacity then because addFirst or addLast will be called
        }
        
        void addFirst(ByteBuffer bb) throws IOException {
            mWriteBuffers.addFirst(bb);
            mCurrentCapacity += bb.capacity();
            if (mCurrentCapacity > mCapacityMax) {
                int cc = mCurrentCapacity;
                clear();
                throw new IOException("write queue size (first) " + cc + " too large (" + mCapacityMax + " allowed)");
            }
        }

        void addLast(ByteBuffer bb) throws IOException {
            mWriteBuffers.addLast(bb);
            mCurrentCapacity += bb.capacity();
            if (mCurrentCapacity > mCapacityMax) {
                int cc = mCurrentCapacity;
                clear();
                throw new IOException("write queue size (last) " + cc + " too large (" + mCapacityMax + " allowed)");
            }
        }

        
        ByteBuffer removeLast() {
            ByteBuffer bb = mWriteBuffers.removeLast();
            mCurrentCapacity -= bb.capacity();
            return bb;
        }
        
        ByteBuffer removeFirst() {
            ByteBuffer bb = mWriteBuffers.removeFirst();
            mCurrentCapacity -= bb.capacity();
            return bb;
        }

        void setMaxCapacity(int capacity) {
            mCapacityMax = capacity;
        }
    }
    
    private Object mWriteLock = new Object();

    private boolean mCloseAfterWrite = false;

    private final WriteQueue mWriteQueue = new WriteQueue();

    public void setWriteQueueMaxCapacity(int capacity) {
        synchronized (mWriteLock) {
            mWriteQueue.setMaxCapacity(capacity);
        }
    }
    
    private class OzPlainFilter extends OzFilter {

        public void read(ByteBuffer rbb) throws IOException {
            rbb.flip();
            int filterChangeId = mFilterChangeId.get();

            // We may have read more than one PDU 
            while (rbb.hasRemaining()) {
                if (mMatcher == null)
                	break;
            	
                ByteBuffer pdu = rbb.duplicate();
                int initialPosition = rbb.position();
                
                boolean matched = false;
                
                matched = mMatcher.match(rbb);
                
                if (mDebug) mLog.debug("plain filter: match returned " + matched);
                
                if (!matched) {
                    rbb.position(initialPosition);
                    break;
                }
                    
                pdu.position(initialPosition);
                pdu.limit(rbb.position());
                //if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("plain filter: input matched", pdu, false));
                mConnectionHandler.handleInput(pdu, true);

                // The earlier call might have resulted in a filter being added
                // to the stack. Check for that condition and bubble back up and
                // refilter whatever else remains in the buffer throug the new
                // filter.
                if (mFilterChangeId.get() != filterChangeId) {
                    mLog.info("plain filter: filter stack has changed");
                    return;
                }
            }
            
            // Just spill all the unmatched stuff to the handler
            if (rbb.hasRemaining()) {
                //if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("plain filter: input unmatched", rbb, false));
                mConnectionHandler.handleInput(rbb, false);
                // automatically enable read interest in the case
                // where match was not found.
                enableReadInterest();
            }
            
            rbb.clear();
        }

        public void close() {
            synchronized (mWriteLock) {
                mCloseAfterWrite = true;
                if (!isWritePending()) {
                    teardown(TeardownReason.NORMAL);
                }
            }
        }

        boolean isWritePending() {
            synchronized (mWriteLock) {
                return !mWriteQueue.isEmpty();
            }
        }

        public void write(ByteBuffer buffer) throws IOException {
            synchronized (mWriteLock) {
                // We always try to flush so we fill up network buffers.
                // Since this a non-blocking channel write() will return
                // immediately if the network buffers are full.
                writeLocked(buffer);
            }
        }

        public boolean flush() throws IOException {
            synchronized (mWriteLock) {
                return writeLocked(null);
            }
        }
        
        private ByteBuffer compactIfNecessary(ByteBuffer orig) {
            int capacity = orig.capacity();
            int remaining = orig.remaining();
            int fullness = 100 * remaining / capacity;
            if (fullness <= WRITE_BUFFER_COMPACTION_PERCENT) {
                ByteBuffer smaller = ByteBuffer.allocate(remaining);
                smaller.put(orig);
                smaller.flip();
                if (mDebug) mLog.debug("compacted write buffer " + orig.capacity() + "->" + smaller.capacity());
                return smaller;
            } else {
                if (mDebug) mLog.debug("uncompacted write buffer " + orig.capacity());
                return orig;
            }    
        }
        
        private boolean writeLocked(ByteBuffer newbb) throws IOException {
            // NB: this check will take teardown lock and we already hold write lock.
            if (isChannelClosed()) {
                mWriteQueue.clear();
                throw new IOException("write requested on closed channel");
            }
     
            if (newbb != null) {
                mWriteQueue.add(newbb);
            }
            
            int totalWritten = 0;
            boolean allWritten = true;
            while (!mWriteQueue.isEmpty()) {
                ByteBuffer data = mWriteQueue.removeFirst();
                assert(data != null);
                assert(data.hasRemaining());

                String before = null;
                int req = 0;
                if (mDebug) before = OzUtil.toString(data); 
                if (mDebug) req = data.remaining();
                if (mTrace) mLog.trace(OzUtil.byteBufferDebugDump("channel write buffer", data, false));

                int wrote = 0;
                try {
                    wrote = mChannel.write(data);
                } catch (IOException ioe) {
                    teardown(TeardownReason.WRITE_FAILED);
                    /*
                     * We definitely want to close the connection right away on
                     * write errors, so that the selector won't spin if someone
                     * forgets to call close.  We re-throw just to propogate the
                     * error state. 
                     */ 
                    throw ioe;
                }

                totalWritten += wrote;
                if (mTrace) mLog.trace("channel wrote=" + wrote + " req=" + req + " partial=" + data.hasRemaining() + " total=" + totalWritten + " buffer: " + before + "->" + OzUtil.toString(data));

                if (data.hasRemaining()) {
                    // Not all data was written. Note that write interest is
                    // enabled *elsewhere* when write is pending.
                    allWritten = false;

                    // Try and compact the new buffer
                    if (data == newbb) { 
                        data = compactIfNecessary(data);
                    } else if (newbb != null) {
                        if (mDebug) mLog.debug("compacting at end of write queue");
                        ByteBuffer last = mWriteQueue.removeLast();
                        if (last != newbb) {
                            throw new IOException("internal error in write queue: last buffer is not the new buffer");
                        }
                        last = compactIfNecessary(last);
                        mWriteQueue.addLast(last);
                    }
                    
                    mWriteQueue.addFirst(data);
                    break;
                }
            }

            if (allWritten) {
                if (mCloseAfterWrite) {
                    teardown(TeardownReason.NORMAL);
                }
                return true;
            } else {
                return false;              
            }
        }
    }

    private Object mAlarmLock = new Object();
    
    private ScheduledFuture<?> mAlarmHandle;
    
    private static final ScheduledThreadPoolExecutor mAlarmExecutor = new ScheduledThreadPoolExecutor(1, new AlarmThreadFactory());

    private static class AlarmThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable, "OzAlarmThread");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    /**
     * Call handleAlarm method with the connection locked after said time has
     * elapsed.
     */
    public void setAlarm(long millis) {
        Runnable alarm = new Runnable() {
            public void run() {
                try {
                    addToNDC();
                    if (mDebug) mLog.debug("firing alarm");
                    synchronized (mLock) {
                        try {
                            mConnectionHandler.handleAlarm();
                        } catch (Throwable t) {
                            if (t instanceof OutOfMemoryError) {
                                String msg = null;
                                try {
                                    msg = "OOME handling alarm (" + mServer.mServerName + ")";
                                } finally {
                                    Zimbra.halt(msg);
                                }
                            }
                            teardown(TeardownReason.ALARM_EXCEPTION);
                        }
                    }
                } finally {
                    clearFromNDC();
                }
            }
        };
        synchronized (mAlarmLock) {
            if (mAlarmHandle != null) {
                cancelAlarm();
            }
            if (mDebug) mLog.debug("creating alarm to fire in " + millis + "ms");
            mAlarmHandle = mAlarmExecutor.schedule(alarm, millis, TimeUnit.MILLISECONDS);
        }
    }
    
    public void cancelAlarm() {
        synchronized (mAlarmLock) {
            if (mAlarmHandle != null) {
                if (mDebug) mLog.debug("cancelling alarm");
                mAlarmHandle.cancel(false);
                boolean removed = mAlarmExecutor.remove((Runnable)mAlarmHandle);
                if (mDebug) mLog.debug("alarm removed (" + removed + ")");
                mAlarmHandle = null;
            }
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
    
    public void writeAsciiWithCRLF(String data) throws IOException {
        byte[] bdata = new byte[data.length() + 2];
        int n = data.length();
        for (int i = 0; i < n; i++) {
            int ch = data.charAt(i);
            if (ch > 127) {
                throw new IllegalArgumentException("expecting ASCII got " + ch + " at " + i + " in '" + data + "'");
            }
            bdata[i] = (byte)ch;
        }
        bdata[n] = OzByteArrayMatcher.CR;
        bdata[n+1] = OzByteArrayMatcher.LF;
        write(ByteBuffer.wrap(bdata));
    }

    public void write(ByteBuffer wbb) throws IOException {
        mFilters.get(0).write(wbb);
    }

    public void flush() throws IOException {
        mFilters.get(0).flush();
    }

    private Map<String, String> mProperties = new HashMap<String, String>();
    
    public String getProperty(String key, String defaultValue) {
        synchronized (mProperties) {
            String result = mProperties.get(key);
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
