/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

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


    // List of supported operations
    // The integer values keep changing during development in order to
    // keep related operations next to each other.  Once we ship, there
    // should not be any more rearranging.  New operations should only
    // get added at the end, and deprecated operations should result in
    // holes.

    public static final int OP_UNKNOWN                  = 0;
    public static final int OP_CHECKPOINT               = 1;
    public static final int OP_START_TXN                = 2;
    public static final int OP_COMMIT_TXN               = 3;
    public static final int OP_ABORT_TXN                = 4;
    public static final int OP_LAST_CONTROL_OP          = 5;

    public static final int OP_ROLLOVER                 = 6;

    public static final int OP_CREATE_MAILBOX           = 7;
    public static final int OP_DELETE_MAILBOX           = 8;
    public static final int OP_DEPRECATED_BACKUP_MAILBOX = 9;
    public static final int OP_REINDEX_MAILBOX          = 10;
    public static final int OP_PURGE_OLD_MESSAGES       = 11;

    public static final int OP_CREATE_SAVED_SEARCH      = 12;
    public static final int OP_MODIFY_SAVED_SEARCH      = 13;
    public static final int OP_CREATE_TAG               = 14;
    public static final int OP_RENAME_TAG               = 15;

    public static final int OP_COLOR_ITEM               = 16;
    public static final int OP_INDEX_ITEM               = 17;
    public static final int OP_ALTER_ITEM_TAG           = 18;
    public static final int OP_SET_ITEM_TAGS            = 19;
    public static final int OP_MOVE_ITEM                = 20;
    public static final int OP_DELETE_ITEM              = 21;
    public static final int OP_COPY_ITEM                = 22;

    public static final int OP_CREATE_FOLDER_PATH       = 23;
    public static final int OP_RENAME_FOLDER_PATH       = 24;
    public static final int OP_EMPTY_FOLDER             = 25;
    public static final int OP_STORE_INCOMING_BLOB      = 26;
    public static final int OP_CREATE_MESSAGE           = 27;
    public static final int OP_SAVE_DRAFT               = 28;
    public static final int OP_SET_IMAP_UID             = 29;
    public static final int OP_CREATE_CONTACT           = 30;
    public static final int OP_MODIFY_CONTACT           = 31;
    public static final int OP_CREATE_NOTE              = 32;
    public static final int OP_EDIT_NOTE                = 33;
    public static final int OP_REPOSITION_NOTE          = 34;
    public static final int OP_CREATE_LINK              = 35;

    public static final int OP_MODIFY_INVITE_FLAG       = 36;
    public static final int OP_MODIFY_INVITE_PARTSTAT   = 37;

    public static final int OP_CREATE_VOLUME            = 38;
    public static final int OP_MODIFY_VOLUME            = 39;
    public static final int OP_DELETE_VOLUME            = 40;
    public static final int OP_SET_CURRENT_VOLUME       = 41;
    public static final int OP_MOVE_BLOBS               = 42;

    public static final int OP_CREATE_INVITE            = 43;
    public static final int OP_SET_CALENDAR_ITEM        = 44;    
    public static final int OP_TRACK_SYNC               = 45;    
    public static final int OP_SET_CONFIG               = 46;    
    public static final int OP_GRANT_ACCESS             = 47;    
    public static final int OP_REVOKE_ACCESS            = 48;    
    public static final int OP_SET_URL                  = 49;    
    public static final int OP_SET_SUBSCRIPTION_DATA    = 50;    
    public static final int OP_SET_PERMISSIONS          = 51;    

    public static final int OP_SAVE_WIKI                = 52;
    public static final int OP_SAVE_DOCUMENT            = 53;
    public static final int OP_ADD_DOCUMENT_REVISION    = 54;

    public static final int OP_TRACK_IMAP               = 55;
    public static final int OP_IMAP_COPY_ITEM           = 56;

    public static final int OP_ICAL_REPLY               = 57;

    public static final int OP_CREATE_FOLDER            = 58;
    public static final int OP_RENAME_FOLDER            = 59;

    public static final int OP_FIX_CALENDAR_ITEM_TIME_ZONE = 60;

    public static final int OP_RENAME_ITEM              = 61;
    public static final int OP_RENAME_ITEM_PATH         = 62;

    public static final int OP_CREATE_CHAT              = 63;
    public static final int OP_SAVE_CHAT                = 64;

    public static final int OP_PURGE_IMAP_DELETED       = 65;

    public static final int OP_DISMISS_CALENDAR_ITEM_ALARM = 66;

    public static final int OP_FIX_CALENDAR_ITEM_END_TIME = 67;

    public static final int OP_INDEX_DEFERRED_ITEMS     = 68;

    public static final int OP_RENAME_MAILBOX           = 69;

    public static final int OP_FIX_CALENDAR_ITEM_TZ     = 70;

    public static final int OP_DATE_ITEM                = 71;

    public static final int OP_SET_DEFAULT_VIEW         = 72;

    public static final int OP_SET_CUSTOM_DATA          = 73;

    public static final int OP_LOCK_ITEM                = 74;
    public static final int OP_UNLOCK_ITEM              = 75;

    public static final int OP_PURGE_REVISION           = 76;

    public static final int OP_LAST                     = 77;


    // Element index is same as Redoable.OP_* constants.
    // The strings must match the class names.
    public static final String[] sOpClassNameArray = {
        "UNKNOWN",                      // 0
        "Checkpoint",
        "UNKNOWN",
        "CommitTxn",
        "AbortTxn",
        "UNKNOWN",                      // separator between control ops and data ops
        "Rollover",
        "CreateMailbox",
        "DeleteMailbox",
        "BackupMailbox",
        "ReindexMailbox",               // 10
        "PurgeOldMessages",
        "CreateSavedSearch",
        "ModifySavedSearch",
        "CreateTag",
        "RenameTag",
        "ColorItem",
        "IndexItem",
        "AlterItemTag",
        "SetItemTags",
        "MoveItem",                     // 20
        "DeleteItem",
        "CopyItem",
        "CreateFolderPath",
        "RenameFolderPath",
        "EmptyFolder",
        "StoreIncomingBlob",
        "CreateMessage",
        "SaveDraft",
        "SetImapUid",
        "CreateContact",                // 30
        "ModifyContact",
        "CreateNote",
        "EditNote",
        "RepositionNote",
        "CreateMountpoint",
        "ModifyInviteFlag",
        "ModifyInvitePartStat",
        "CreateVolume",
        "ModifyVolume",
        "DeleteVolume",                 // 40
        "SetCurrentVolume",
        "MoveBlobs",
        "CreateInvite",
        "SetCalendarItem",
        "TrackSync",
        "SetConfig",
        "GrantAccess",
        "RevokeAccess",
        "SetFolderUrl",
        "SetSubscriptionData",          // 50
        "SetPermissions",
        "SaveWiki",
        "SaveDocument",
        "AddDocumentRevision",
        "TrackImap",
        "ImapCopyItem",
        "ICalReply",
        "CreateFolder",
        "RenameFolder",
        "FixCalendarItemTimeZone",      // 60
        "RenameItem",
        "RenameItemPath",
        "CreateChat",
        "SaveChat",
        "PurgeImapDeleted",
        "DismissCalendarItemAlarm",
        "FixCalendarItemEndTime",
        "IndexDeferredItems",
        "RenameMailbox",
        "FixCalendarItemTZ",            // 70
        "DateItem",
        "SetFolderDefaultView",
        "SetCustomData",
        "LockItem",
        "UnlockItem",
        "PurgeRevision"
    };

    public static String getOpClassName(int opcode) {
        if (opcode > OP_UNKNOWN && opcode < OP_LAST)
            return sOpClassNameArray[opcode];
        else
            return null;
    }


    private Version mVersion;
    private TransactionId mTxnId;
    private boolean mActive;
    private long mTimestamp;		// timestamp of the operation
    private int mChangeId = -1;
    private int mChangeConstraint;
    private long mMailboxId;
    private RedoLogManager mRedoLogMgr;
    private boolean mUnloggedReplay;  // true if redo of this op is not redo-logged
    RedoCommitCallback mCommitCallback;

    public RedoableOp() {
        mRedoLogMgr = RedoLogProvider.getInstance().getRedoLogManager();
        mVersion = new Version();
        mTxnId = null;
        mActive = false;
        mMailboxId = UNKNOWN_ID;
        mUnloggedReplay = false;
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

    public synchronized void commit() {
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
        return getOpCode() > OP_LAST_CONTROL_OP;
    }

    // whether this is a recod that marks the end of a transaction
    // (true for CommitTxn and AbortTxn, false for all others)
    public boolean isEndMarker() {
        int opCode = getOpCode();
        return opCode == OP_COMMIT_TXN || opCode == OP_ABORT_TXN;
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

    public long getMailboxId() {
        return mMailboxId;
    }

    public void setMailboxId(long mboxId) {
        mMailboxId = mboxId;
    }

    protected void serializeHeader(RedoLogOutput out) throws IOException {
        out.write(REDO_MAGIC.getBytes());
        mVersion.serialize(out);
        out.writeInt(getOpCode());

        out.writeLong(mTimestamp);
        out.writeInt(mChangeId);
        out.writeInt(mChangeConstraint);
        mTxnId.serialize(out);
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
        if (getVersion().atLeast(1, 26))
            mMailboxId = in.readLong();
        else
            mMailboxId = in.readInt();

        // Deserialize the subclass.
        deserializeData(in);
    }

    @Override public String toString() {
        StringBuffer sb = new StringBuffer("txn ");
        sb.append(mTxnId).append(" [").append(getOpClassName(getOpCode()));
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
     * same value as getEntryOpCode(), except CommitTxn and AbortTxn
     * return the getEntryOpCode() value from their corresponding
     * change entry.
     * @return
     */
    public int getTxnOpCode() {
        return getOpCode();
    }

    /**
     * Returns the operation code of the log entry.
     * @return
     */
    // Should return one of the OP_xyz constants defined above.
    public abstract int getOpCode();

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
        return deserializeOp(in, false);
    }

    public static RedoableOp deserializeOp(RedoLogInput in, boolean skipDetail)
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

        int opcode = in.readInt();
        if (!skipDetail) {
            String className = RedoableOp.getOpClassName(opcode);
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
        } else {
            op = new HeaderOnlyOp(opcode);
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


    private static boolean checkSubclasses() {
        boolean allGood = true;
        for (int opcode = OP_UNKNOWN + 1; opcode < OP_LAST; opcode++) {
            String className = RedoableOp.getOpClassName(opcode);
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

    public static void main(String[] args) {
        if (!checkSubclasses()) {
            System.err.println(
                    "Some RedoableOp subclasses are incomplete.  " +
                "Hint: Make sure the subclass defines a default constructor.");
            System.exit(1);
        }
    }

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
