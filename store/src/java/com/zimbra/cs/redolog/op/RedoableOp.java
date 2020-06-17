/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.redolog.RedoCommitCallback;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.Version;

public abstract class RedoableOp {

    protected static Log mLog = LogFactory.getLog(RedoableOp.class);
    private static String sPackageName = RedoableOp.class.getPackage().getName();

    // magic marker for each redo item in redo log
    public static final String REDO_MAGIC = "ZMREDO";

    // for initializing various ID member values
    public static final int UNKNOWN_ID = 0;

    // special ID value meaning operation is not for any specific mailbox
    // but is needed by all mailboxes
    public static final int MAILBOX_ID_ALL = -1;

    protected MailboxOperation mOperation;
    private Version mVersion;
    private TransactionId mTxnId;
    private boolean mActive;
    private long mTimestamp;		// timestamp of the operation
    private int mChangeId = -1;
    private int mChangeConstraint;
    private int mMailboxId;
    protected RedoLogManager mRedoLogMgr;
    private boolean mUnloggedReplay;  // true if redo of this op is not redo-logged
    protected RedoCommitCallback mCommitCallback;

    protected RedoableOp(MailboxOperation op, RedoLogManager mgr) {
        mOperation = op;
        mRedoLogMgr = mgr;
        mVersion = new Version();
        mTxnId = null;
        mActive = false;
        mMailboxId = UNKNOWN_ID;
        mUnloggedReplay = false;
    }
    protected RedoableOp(MailboxOperation op) {
        this(op, RedoLogProvider.getInstance().getRedoLogManager());
    }

    protected Version getVersion()      { return mVersion; }
    private void setVersion(Version v)  { mVersion = v; }

    public boolean getUnloggedReplay()        { return mUnloggedReplay; }
    public void setUnloggedReplay(boolean b)  { mUnloggedReplay = b; }

    /**
     * Set up a callback that will be called when the transaction commits and
     * the commit record is safely on disk.  This method must be called before
     * commit() is called.
     * @param callback
     */
    public void setCommitCallback(RedoCommitCallback callback) {
        mCommitCallback = callback;
    }

    protected RedoCommitCallback getCommitCallback() {
        return mCommitCallback;
    }

    public void start(long timestamp) {
        // Assign timestamp and txn ID to the operation.
        // Doing the assignment in this method means we timestamp and sequence
        // the operation start time, not when the operation get committed or
        // logged.
        //
        // Ideally, operations should start, commit, and log in precisely the
        // same order, but we can guarantee that only by eliminating concurrency.

        mTimestamp = timestamp;
        if (isStartMarker())
            setTransactionId(mRedoLogMgr.getNewTxnId());
    }

    public void log() {
        log(true);
    }

    public void log(boolean synchronous) {
        mRedoLogMgr.log(this, synchronous);
        if (isStartMarker())
            mActive = true;
    }

    public void setChangeConstraint(boolean checkCreated, int changeId) {
        mChangeConstraint = changeId;
        if (checkCreated == OperationContext.CHECK_CREATED)
            mChangeConstraint *= -1;
    }

    public OperationContext getOperationContext() {
        OperationContext octxt = new OperationContext(this);
        if (mChangeConstraint != 0) {
            boolean checkCreated = (mChangeConstraint < 0 ? OperationContext.CHECK_CREATED : OperationContext.CHECK_MODIFIED);
            octxt.setChangeConstraint(checkCreated, Math.abs(mChangeConstraint));
        }
        return octxt;
    }

    public synchronized void commit() throws ServiceException {
        if (mActive) {
            mActive = false;
            mRedoLogMgr.commit(this);
            if (mChainedOps != null) {
                for (RedoableOp rop : mChainedOps)
                    rop.commit();
                mChainedOps = null;
            }
        }
        // We don't need to hang onto the byte arrays after commit/abort.
        synchronized (mSBAVGuard) {
            mSerializedByteArrayVector = null;
        }
    }

    public synchronized void abort() {
        if (mActive) {
            mActive = false;
            mRedoLogMgr.abort(this);
            if (mChainedOps != null) {
                for (RedoableOp rop : mChainedOps)
                    rop.abort();
                mChainedOps = null;
            }
        }
        // We don't need to hang onto the byte arrays after commit/abort.
        synchronized (mSBAVGuard) {
            mSerializedByteArrayVector = null;
        }
    }

    // whether this is a record that marks the beginning of a transaction
    public boolean isStartMarker() {
        switch (mOperation) {
        case Checkpoint:
        case CommitTxn:
        case AbortTxn:
            return false;
        default:
            return true;
        }
    }

    // whether this is a recod that marks the end of a transaction
    // (true for CommitTxn and AbortTxn, false for all others)
    public boolean isEndMarker() {
        switch (mOperation) {
        case CommitTxn:
        case AbortTxn:
            return true;
        default:
            return false;
        }
    }

    /**
     * Crash recovery redoes all uncommitted operations from previous crash
     * before allowing client connections.  This doesn't work out well for a
     * long-running operation such as fully reindexing a large mailbox.  Those
     * operations should be excluded from initial crash recovery and run after
     * startup procedure finishes.  Note this treatment is possible only for
     * independent operations, i.e. no other operation during crash recovery
     * should depend on having this operation done first.
     * @return
     */
    public boolean deferCrashRecovery() {
        return false;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    protected void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public int getChangeId() {
        return mChangeId;
    }

    public void setChangeId(int changeId) {
        mChangeId = changeId;
    }

    public TransactionId getTransactionId() {
        return mTxnId;
    }

    protected void setTransactionId(TransactionId txnId) {
        mTxnId = txnId;
    }

    public int getMailboxId() {
        return mMailboxId;
    }

    public void setMailboxId(int mboxId) {
        mMailboxId = mboxId;
    }

    protected void serializeHeader(RedoLogOutput out) throws IOException {
        out.write(REDO_MAGIC.getBytes());
        mVersion.serialize(out);
        out.writeInt(getOperation().getCode());

        out.writeLong(mTimestamp);
        out.writeInt(mChangeId);
        out.writeInt(mChangeConstraint);
        getTransactionId().serialize(out);
        // still writing and reading long mailbox IDs for backwards compatibility, even though they're ints again
        out.writeLong(mMailboxId);
    }

    private void deserialize(RedoLogInput in) throws IOException {
        // We don't deserialize magic marker, version, and operation code
        // because they were already deserialized by deserializeOp.

        mTimestamp = in.readLong();
        mChangeId = in.readInt();
        mChangeConstraint = in.readInt();
        mTxnId = new TransactionId();
        mTxnId.deserialize(in);
        // still writing and reading long mailbox IDs for backwards compatibility, even though they're ints again
        if (getVersion().atLeast(1, 26)) {
            mMailboxId = (int) in.readLong();
        } else {
            mMailboxId = in.readInt();
        }

        // Deserialize the subclass.
        deserializeData(in);
    }

    @Override public String toString() {
        StringBuffer sb = new StringBuffer("txn ");
        sb.append(mTxnId).append(" [").append(getOperation().name());
        sb.append("] ver=").append(mVersion);
        sb.append(", tstamp=").append(mTimestamp);
        if (mChangeId != -1)
            sb.append(", change=").append(mChangeId);
        if (mChangeConstraint != 0)
            sb.append(", constraint=").append(mChangeConstraint);
        String data = getPrintableData();
        if (mMailboxId != UNKNOWN_ID) {
            sb.append(", mailbox=").append(mMailboxId);
            if (data != null)
                sb.append(", ").append(data);
        } else if (data != null)
            sb.append(", ").append(data);
        return sb.toString();
    }

    /**
     * Returns the operation code of the transaction.  Returns the
     * same value as getOperation(), except CommitTxn and AbortTxn
     * return the getOperation() value from their corresponding
     * change entry.
     * @return
     */
    public MailboxOperation getTxnOpCode() {
        return getOperation();
    }

    /**
     * Returns the operation being performed
     * @return
     */
    public MailboxOperation getOperation() {
        return mOperation;
    }

    /**
     * Repeat the operation.
     */
    public abstract void redo() throws Exception;

    // Used to toString().
    protected abstract String getPrintableData();

    // Used by serialize() and deserialize().
    protected abstract void serializeData(RedoLogOutput out) throws IOException;
    protected abstract void deserializeData(RedoLogInput in) throws IOException;

    /**
     * Returns any additional data that must be written after the usual header
     * and data.
     */
    public InputStream getAdditionalDataStream() throws IOException {
        return null;
    }

    /**
     * Returns the next operation in the redo log stream.
     * @return a RedoableOp object
     * @throws EOFException - if end of stream is reached while reading;
     *						  EOF may happen in the middle of a log entry,
     *						  indicating the server may have died unexpectedly
     * @throws IOException - if any other error occurs
     */
    public static RedoableOp deserializeOp(RedoLogInput in)
    throws EOFException, IOException {
        RedoableOp op = null;
        byte[] magicBuf = new byte[REDO_MAGIC.length()];
        in.readFully(magicBuf, 0, magicBuf.length);
        String magic = new String(magicBuf);
        if (magic.compareTo(REDO_MAGIC) != 0)
            throw new IOException("Missing redo item magic marker");

        Version ver = new Version();
        ver.deserialize(in);
        if (ver.tooHigh())
            throw new IOException("Redo op version " + ver +
                    " is higher than the highest known version " +
                    Version.latest());

        MailboxOperation opcode = MailboxOperation.fromInt(in.readInt());
        String className = opcode.name();
        if (className != null) {
            Class clz = null;
            try {
                clz = loadOpClass(sPackageName + "." + className);
            } catch (ClassNotFoundException e) {
                throw new IOException("ClassNotFoundException for redo operation " + className);
            }
            try {
                op = (RedoableOp) clz.newInstance();
            } catch (InstantiationException e) {
                String msg = "Unable to instantiate " + className;
                mLog.error(msg, e);
                throw new IOException("Unable to instantiate " + className);
            } catch (IllegalAccessException e) {
                String msg = "IllegalAccessException while instantiating " + className;
                mLog.error(msg, e);
                throw new IOException(msg);
            }
        } else {
            throw new IOException("Invalid redo operation code " + opcode);
        }
        op.setVersion(ver);
        op.deserialize(in);
        return op;
    }

    protected byte[][] mSerializedByteArrayVector;
    // Synchronize access to mSerializedByteArrayVector with mSBAVGuard.
    // Don't synchronize on "this" object because that can cause deadlock
    // between a thread committing this op and another thread doing
    // rollover, which needs to call getSerializedByteArrayVector on this
    // object to log it to new redo log file.
    protected final Object mSBAVGuard = new Object();

    public void setSerializedByteArray(byte[] data) {
        synchronized (mSBAVGuard) {
            if (data != null) {
                mSerializedByteArrayVector = new byte[1][];
                mSerializedByteArrayVector[0] = data;
            } else {
                // clear current vector
                mSerializedByteArrayVector = null;
            }
        }
    }

    /**
     * Returns the core redoable op data as a <tt>byte[]</tt>.  Does not include
     * the result of {@link #getAdditionalDataStream()}.
     */
    private byte[] serializeToByteArray() throws IOException {
        byte[] buf;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        RedoLogOutput out = new RedoLogOutput(baos);
        serializeHeader(out);
        serializeData(out);
        buf = baos.toByteArray();
        baos.close();
        return buf;
    }

    /**
     * Returns the entire redoable op data as an <tt>InputStream</tt>.
     * Includes the result of {@link #getAdditionalDataStream()}.
     */
    public InputStream getInputStream() throws IOException {
        synchronized (mSBAVGuard) {
            if (mSerializedByteArrayVector == null)
                setSerializedByteArray(serializeToByteArray());
            List<InputStream> streams = new ArrayList<InputStream>(mSerializedByteArrayVector.length + 1);
            for (byte[] array : mSerializedByteArrayVector) {
                streams.add(new ByteArrayInputStream(array));
            }
            InputStream additional = getAdditionalDataStream();
            if (additional != null) {
                streams.add(additional);
            }

            return new SequenceInputStream(Collections.enumeration(streams));
        }
    }

    // indexing sub-operation that is chained to this operation

    private List<RedoableOp> mChainedOps;

    public synchronized void addChainedOp(RedoableOp subOp) {
        if (mChainedOps == null)
            mChainedOps = new LinkedList<RedoableOp>();
        mChainedOps.add(subOp);
    }


    protected static boolean checkSubclasses() {
        boolean allGood = true;
        for (MailboxOperation opcode : EnumSet.allOf(MailboxOperation.class)) {
            String className = opcode.name();
            if (className == null) {
                System.err.println("Invalid redo operation code: " + opcode);
                allGood = false;
            } else if (className.compareTo("UNKNOWN") != 0) {
                Class clz = null;
                try {
                    clz = loadOpClass(sPackageName + "." + className);
                    clz.newInstance();
                } catch (ClassNotFoundException e) {
                    // Some classes may not be found depending on which
                    // optional packages are installed.
                    System.out.println("Ignoring ClassNotFoundException for redo operation " + className);
                } catch (InstantiationException e) {
                    String msg = "Unable to instantiate " + className +
                    "; Check default constructor is defined.";
                    System.err.println(msg);
                    e.printStackTrace(System.err);
                    allGood = false;
                } catch (IllegalAccessException e) {
                    String msg = "IllegalAccessException while instantiating " + className;
                    System.err.println(msg);
                    e.printStackTrace(System.err);
                    allGood = false;
                }
            }
        }
        return allGood;
    }

    private static Map<String, Class> sOpClassMap = new HashMap<String, Class>();
    private static List<ClassLoader> sOpClassLoaders = new ArrayList<ClassLoader>();

    /**
     * Register a class loader for instantiating redo op objects.
     * @param ldr
     */
    public synchronized static void registerClassLoader(ClassLoader ldr) {
        mLog.debug("Registering class loader " + ldr);
        for (ClassLoader loader : sOpClassLoaders) {
            if (loader.equals(ldr)) return;
        }
        sOpClassLoaders.add(ldr);
    }

    public synchronized static void deregisterClassLoader(ClassLoader ldr) {
        mLog.debug("Deregistering class loader " + ldr);
        List<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, Class> entry : sOpClassMap.entrySet()) {
            Class clz = entry.getValue();
            if (clz.getClassLoader().equals(ldr))
                toRemove.add(entry.getKey());
        }
        for (String key : toRemove) {
            sOpClassMap.remove(key);
            mLog.debug("Removed " + key + " from redo op class map");
        }
        for (Iterator iter = sOpClassLoaders.iterator(); iter.hasNext(); ) {
            ClassLoader loader = (ClassLoader) iter.next();
            if (loader.equals(ldr))
                iter.remove();
        }
    }

    private synchronized static Class loadOpClass(String className)
    throws ClassNotFoundException {
        Class clz = sOpClassMap.get(className);
        if (clz == null) {
            // Try default class loader first.
            try {
                clz = Class.forName(className);
                sOpClassMap.put(className, clz);
            } catch (ClassNotFoundException e) {
                // Try the registered class loaders.
                for (ClassLoader loader : sOpClassLoaders) {
                    try {
                        clz = loader.loadClass(className);
                        mLog.debug(
                                "Loaded class " + className +
                                " using class loader " + loader);
                        sOpClassMap.put(className, clz);
                        break;
                    } catch (ClassNotFoundException e2) {}
                }
                if (clz == null)
                    throw e;
            }

        }
        return clz;
    }

    public boolean isDeleteOp() {
        return false;
    }
}
