/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
 * Created on 2005. 6. 9.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.redolog.op;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogBlobStore;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.redolog.RedoOpBlobStore;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

public class StoreIncomingBlob extends RedoableOp {

    static final int MAX_BLOB_SIZE = 100 * 1024 * 1024;  // 100MB size limit
    static final int MAX_MAILBOX_LIST_LENGTH = 1000000;

    private String mDigest;
    private String mPath;           // full path to blob file
    private int mMsgSize;           // original, uncompressed blob size in bytes
    private RedoableOpData mData;
    private List<Integer> mMailboxIdList;

    public StoreIncomingBlob()  {
        super(MailboxOperation.StoreIncomingBlob);
    }

    public StoreIncomingBlob(String digest, int msgSize, List<Integer> mboxIdList) {
        this();
        setMailboxId(MAILBOX_ID_ALL);
        mDigest = digest != null ? digest : "";
        mMsgSize = msgSize;
        mMailboxIdList = mboxIdList;
    }

    public List<Integer> getMailboxIdList() {
        return mMailboxIdList;
    }

    public void setMailboxIdList(List<Integer> list) {
        mMailboxIdList = list;
    }

    public void setBlobBodyInfo(File file) {
        mData = new RedoableOpData(file);
        mPath = file.getPath();
    }

    public void setBlobBodyInfo(DataSource ds, int dataLength, String path) {
        mData = new RedoableOpData(ds, dataLength);
        mPath = path;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("blobDigest=\"");
        sb.append(mDigest).append("\", size=").append(mMsgSize);
        sb.append(", dataLen=").append(mData.getLength());
        sb.append(", path=").append(mPath);
        sb.append(", mbox=").append(mMailboxIdList == null ? "[]" : mMailboxIdList);
        return sb.toString();
    }

    @Override public InputStream getAdditionalDataStream() throws IOException {
        return mData.getInputStream();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        if (getVersion().atLeast(1, 0)) {
            if (mMailboxIdList != null) {
                out.writeInt(mMailboxIdList.size());
                for (Integer mboxId : mMailboxIdList) {
                    // still writing and reading long mailbox IDs for backwards compatibility, even though they're ints again
                    if (getVersion().atLeast(1, 26)) {
                        out.writeLong(mboxId.longValue());
                    } else {
                        out.writeInt(mboxId.intValue());
                    }
                }
            } else {
                out.writeInt(0);
            }
        }
        out.writeUTF(mDigest);
        out.writeUTF(mPath);
        out.writeShort((short) -1);
        out.writeInt(mMsgSize);
        out.writeInt(mData.getLength());

        // During serialize, do not serialize the blob data buffer.
        // Blob buffer is handled by getSerializedByteArrayVector()
        // implementation in this class as the last vector element.
        // Consequently, in the serialized stream blob data comes last.
        // deserializeData() should take this into account.
        //out.write(mData);  // Don't do this here!
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        if (getVersion().atLeast(1, 0)) {
            int listLen = in.readInt();
            if (listLen > MAX_MAILBOX_LIST_LENGTH) {
                throw new IOException("Deserialized mailbox list too large (" + listLen + ")");
            }
            if (listLen >= 1) {
                List<Integer> list = new ArrayList<Integer>(listLen);
                for (int i = 0; i < listLen; i++) {
                    // still writing and reading long mailbox IDs for backwards compatibility, even though they're ints again
                    if (getVersion().atLeast(1, 26)) {
                        list.add(Integer.valueOf((int) in.readLong()));
                    } else {
                        list.add(Integer.valueOf(in.readInt()));
                    }
                }
                mMailboxIdList = list;
            }
        }
        mDigest = in.readUTF();
        mPath = in.readUTF();
        in.readShort();
        mMsgSize = in.readInt();
        int dataLen = in.readInt();

        // mData must be the last thing deserialized.  See comments in
        // serializeData().
        long pos = in.getFilePointer();
        mData = new RedoableOpData(new File(in.getPath()), pos, dataLen);

        // Now that we have a stream to the data, skip to the next op.
        int numSkipped = in.skipBytes(dataLen);
        if (numSkipped != dataLen) {
            String msg = String.format("Attempted to skip %d bytes at position %d in %s, but actually skipped %d.",
                    dataLen, pos, in.getPath(), numSkipped);
            throw new IOException(msg);
        }
    }

    @Override public void redo() throws Exception {
        // Execution of redo is logged to current redo logger.  For most other
        // ops this is handled by Mailbox class, but StoreIncomingBlob is an
        // exception because of the way it is used in Mailbox.

        StoreIncomingBlob redoRecorder = null;
        if (!getUnloggedReplay()) {
            redoRecorder = new StoreIncomingBlob(mDigest, mMsgSize, mMailboxIdList);
            redoRecorder.start(getTimestamp());
            redoRecorder.setBlobBodyInfo(new RedoableOpDataSource(mData), mData.getLength(), mPath);
            redoRecorder.log();
        }

        boolean success = false;
        try {
            boolean compressed = mData.getLength() != mMsgSize;
            Blob blob = StoreManager.getInstance().storeIncoming(mData.getInputStream(), compressed);
            if (compressed) {
                blob.setDigest(mDigest).setRawSize(mMsgSize).setCompressed(compressed);
            }
            RedoLogBlobStore blobStore = mRedoLogMgr.getBlobStore();
            if (blobStore instanceof RedoOpBlobStore) {
                ((RedoOpBlobStore) blobStore).registerBlob(mPath, blob);
            } else {
                ZimbraLog.redolog.warn("StoreIncomingBlob operations must be used with RedoOpBlobStore (found %s)", blobStore.getClass().getSimpleName());
            }
            success = true;
        } finally {
            if (redoRecorder != null) {
                if (success) {
                    redoRecorder.commit();
                } else {
                    redoRecorder.abort();
                }
            }
        }
    }
}
