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

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.TransactionId;
import com.zimbra.cs.redolog.Version;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 */
public abstract class RedoableOp {

	protected static Log mLog = LogFactory.getLog(RedoableOp.class);
    private static String sPackageName = RedoableOp.class.getPackage().getName();

	// magic marker for each redo item in redo log
	private static final String REDO_MAGIC = "ZMREDO";

	// versioning of serialization
	// currently v0.1
	private static final Version VERSION = new Version((short) 0, (short) 1);

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
	public static final int OP_LAST_CONTROL_OP			= 5;

    public static final int OP_ROLLOVER                 = 6;

    public static final int OP_CREATE_MAILBOX           = 7;
    public static final int OP_DELETE_MAILBOX           = 8;
    public static final int OP_BACKUP_MAILBOX           = 9;
    public static final int OP_REINDEX_MAILBOX          = 10;
    public static final int OP_PURGE_OLD_MESSAGES       = 11;

    public static final int OP_CREATE_SAVED_SEARCH      = 12;
    public static final int OP_MODIFY_SAVED_SEARCH      = 13;
    public static final int OP_CREATE_TAG               = 14;
    public static final int OP_RENAME_TAG               = 15;
    public static final int OP_COLOR_TAG                = 16;

    public static final int OP_INDEX_ITEM               = 17;
    public static final int OP_ALTER_ITEM_TAG           = 18;
    public static final int OP_SET_ITEM_TAGS            = 19;
    public static final int OP_MOVE_ITEM                = 20;
    public static final int OP_DELETE_ITEM              = 21;
    public static final int OP_COPY_ITEM                = 22;

    public static final int OP_CREATE_FOLDER            = 23;
    public static final int OP_RENAME_FOLDER            = 24;
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
    public static final int OP_COLOR_NOTE               = 35;
    
    public static final int OP_MODIFY_INVITE_FLAG       = 36;
    public static final int OP_MODIFY_INVITE_PARTSTAT   = 37;

    public static final int OP_CREATE_VOLUME            = 38;
    public static final int OP_MODIFY_VOLUME            = 39;
    public static final int OP_DELETE_VOLUME            = 40;
    public static final int OP_SET_CURRENT_VOLUME       = 41;

	public static final int OP_LAST						= 42;


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
        "ColorTag",
        "IndexItem",
        "AlterItemTag",
        "SetItemTags",
        "MoveItem",                     // 20
        "DeleteItem",
        "CopyItem",
        "CreateFolder",
        "RenameFolder",
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
        "ColorNote",
        "ModifyInviteFlag",
        "ModifyInvitePartStat",
        "CreateVolume",
        "ModifyVolume",
        "DeleteVolume",                 // 40
        "SetCurrentVolume"
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
	private int mMailboxId;
	private RedoLogManager mRedoLogMgr;

	public RedoableOp() {
		mRedoLogMgr = RedoLogProvider.getInstance().getRedoLogManager();
		mVersion = VERSION;
		mTxnId = null;
		mActive = false;
		mMailboxId = UNKNOWN_ID;
	}

	private void setVersion(Version v) {
		mVersion = v;
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

	/**
	 * Does commit or rollback of current database transaction and writes
	 * COMMIT/ABORT record into the redo log appropriately.
	 * @param conn
	 * @param success true if commit is requested, false if rollback is requested
	 * @throws ServiceException
	 */
	public void dbCommit(Connection conn, boolean success) throws ServiceException {
    	if (success) {
    		try {
    			conn.commit();
		    	commit();
    		} catch (ServiceException e) {
    			abort();
        		DbPool.quietRollback(conn);
    			throw e;
    		}
    	} else {
    		abort();
    		DbPool.quietRollback(conn);
    	}
	}

	public synchronized void commit() {
		if (mActive) {
			mActive = false;
			mRedoLogMgr.commit(this);
            if (mChainedOp != null) {
                mChainedOp.commit();
                mChainedOp = null;
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
            if (mChainedOp != null) {
                mChainedOp.abort();
                mChainedOp = null;
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

	public int getMailboxId() {
		return mMailboxId;
	}

	public void setMailboxId(int mboxId) {
		mMailboxId = mboxId;
	}

	protected void serializeHeader(DataOutput out) throws IOException {
		out.write(REDO_MAGIC.getBytes());
		mVersion.serialize(out);
		out.writeInt(getOpCode());

        out.writeLong(mTimestamp);
        out.writeInt(mChangeId);
        out.writeInt(mChangeConstraint);
		mTxnId.serialize(out);
		out.writeInt(mMailboxId);
	}

	private void deserialize(DataInput in) throws IOException {
		// We don't deserialize magic marker, version, and operation code
		// because they were already deserialized by deserializeOp.

        mTimestamp = in.readLong();
        mChangeId = in.readInt();
        mChangeConstraint = in.readInt();
		mTxnId = new TransactionId();
		mTxnId.deserialize(in);
		mMailboxId = in.readInt();

		// Deserialize the subclass.
		deserializeData(in);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("txn ");
		sb.append(mTxnId).append(" [").append(getOpClassName(getOpCode()));
        sb.append("] tstamp=").append(mTimestamp);
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
	protected abstract void serializeData(DataOutput out) throws IOException;
	protected abstract void deserializeData(DataInput in) throws IOException;

	/**
	 * Returns the next operation in the redo log stream.
	 * @return a RedoableOp object
	 * @throws EOFException - if end of stream is reached while reading;
	 *						  EOF may happen in the middle of a log entry,
	 *						  indicating the server may have died unexpectedly
	 * @throws IOException - if any other error occurs
	 */
	public static RedoableOp deserializeOp(DataInput is)
	throws EOFException, IOException {
		return deserializeOp(is, false);
	}

	public static RedoableOp deserializeOp(DataInput is, boolean skipDetail)
	throws EOFException, IOException {
		RedoableOp op = null;
		byte[] magicBuf = new byte[REDO_MAGIC.length()];
		is.readFully(magicBuf, 0, magicBuf.length);
		String magic = new String(magicBuf);
		if (magic.compareTo(REDO_MAGIC) != 0)
			throw new IOException("Missing redo item magic marker");

		Version ver = new Version();
		ver.deserialize(is);
		if (ver.compareTo(RedoableOp.VERSION) > 0)
			throw new IOException("Redo log version " + ver + " is higher than " + RedoableOp.VERSION);

		int opcode = is.readInt();
		if (!skipDetail) {
			String className = RedoableOp.getOpClassName(opcode);
			if (className != null) {
				Class cl = null;
				try {
					cl = Class.forName(sPackageName + "." + className);
				} catch (ClassNotFoundException e) {
					throw new IOException("ClassNotFoundException for redo operation " + className);
				}
				try {
					op = (RedoableOp) cl.newInstance();
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
		op.deserialize(is);
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

    protected byte[] serializeToByteArray() throws IOException {
        byte[] buf;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        DataOutputStream dos = new DataOutputStream(baos);
        serializeHeader(dos);
        serializeData(dos);
        buf = baos.toByteArray();
        dos.close();
        return buf;
    }

    public byte[][] getSerializedByteArrayVector() throws IOException {
        synchronized (mSBAVGuard) {
            if (mSerializedByteArrayVector == null)
                setSerializedByteArray(serializeToByteArray());
    		return mSerializedByteArrayVector;
        }
	}


    // indexing sub-operation that is chained to this operation

    private RedoableOp mChainedOp;

    public synchronized void setChainedOp(RedoableOp subOp) {
    	mChainedOp = subOp;
    }


    // Custom read/writeUTF8 methods to replace DataOutputStream.writeUTF()
    // which has 64KB limit

//    private static final int MAX_STRING_LEN = 10 * 1024 * 1024;     // 10MB

    protected static void writeUTF8(DataOutput out, String str) throws IOException {
        ByteUtil.writeUTF8(out, str);
    }

    protected static String readUTF8(DataInput in) throws IOException {
        return ByteUtil.readUTF8(in);
    }

    static {
    	boolean allSubclassesGood = checkSubclasses();
        if (!allSubclassesGood) {
        	Zimbra.halt("Some RedoableOp subclasses are incomplete");
        }
    }

    private static boolean checkSubclasses() {
        boolean allGood = true;
        for (int opcode = OP_UNKNOWN + 1; opcode < OP_LAST; opcode++) {
            String className = RedoableOp.getOpClassName(opcode);
            if (className == null) {
            	mLog.error("Invalid redo operation code: " + opcode);
                allGood = false;
            } else if (className.compareTo("UNKNOWN") != 0) {
                Class cl = null;
                try {
                    cl = Class.forName(sPackageName + "." + className);
                    RedoableOp op = (RedoableOp) cl.newInstance();
                } catch (ClassNotFoundException e) {
                    mLog.error("ClassNotFoundException for redo operation " + className, e);
                    allGood = false;
                } catch (InstantiationException e) {
                    String msg = "Unable to instantiate " + className +
                                 "; Check default constructor is defined.";
                    mLog.error(msg, e);
                    allGood = false;
                } catch (IllegalAccessException e) {
                    String msg = "IllegalAccessException while instantiating " + className;
                    mLog.error(msg, e);
                    allGood = false;
                }
            }
        }
    	return allGood;
    }
}
