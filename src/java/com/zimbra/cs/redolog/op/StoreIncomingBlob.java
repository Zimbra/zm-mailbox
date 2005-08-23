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
 * Created on 2005. 6. 9.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StoreIncomingBlob extends RedoableOp {

    static final int MAX_BLOB_SIZE = 100 * 1024 * 1024;  // 100MB size limit

    private String mDigest;
    private String mPath;           // full path to blob file
    private short mVolumeId = -1;   // volume on which the blob is saved
    private int mMsgSize;           // original, uncompressed blob size in bytes
    private byte[] mData;

    public StoreIncomingBlob() {
    }

    public StoreIncomingBlob(String digest, int msgSize) {
        setMailboxId(MAILBOX_ID_ALL);
        mDigest = digest != null ? digest : "";
        mMsgSize = msgSize;
    }

    public int getOpCode() {
        return OP_STORE_INCOMING_BLOB;
    }

    public void setBlobBodyInfo(byte[] data, String path, short volumeId) {
        mData = data;
        mPath = path;
        mVolumeId = volumeId;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("blobDigest=\"");
        sb.append(mDigest).append("\", size=").append(mMsgSize);
        sb.append(", vol=").append(mVolumeId);
        sb.append(", path=").append(mPath);
        return sb.toString();
    }

    public byte[][] getSerializedByteArrayVector() throws IOException {
        synchronized (mSBAVGuard) {
            if (mSerializedByteArrayVector != null)
                return mSerializedByteArrayVector;
    
            mSerializedByteArrayVector = new byte[2][];
            mSerializedByteArrayVector[0] = serializeToByteArray();
            mSerializedByteArrayVector[1] = mData;
            return mSerializedByteArrayVector;
        }
    }

    protected void serializeData(DataOutput out) throws IOException {
        writeUTF8(out, mDigest);
        writeUTF8(out, mPath);
        out.writeShort(mVolumeId);
        out.writeInt(mMsgSize);
        out.writeInt(mData.length);
        // During serialize, do not serialize the blob data buffer.
        // Blob buffer is handled by getSerializedByteArrayVector()
        // implementation in this class as the last vector element.
        // Consequently, in the serialized stream blob data comes last.
        // deserializeData() should take this into account.
        //out.write(mData);  // Don't do this here!
    }

    protected void deserializeData(DataInput in) throws IOException {
        mDigest = readUTF8(in);
        mPath = readUTF8(in);
        mVolumeId = in.readShort();
        mMsgSize = in.readInt();
        int dataLen = in.readInt();
        if (dataLen > MAX_BLOB_SIZE) {
            throw new IOException("Deserialized blob size too large (" + dataLen + " bytes)");
        }
        mData = new byte[dataLen];
        in.readFully(mData, 0, dataLen);
        // mData must be the last thing deserialized.  See comments in
        // serializeData().
    }

    public void redo() throws Exception {
        // Execution of redo is logged to current redo logger.  For most other
        // ops this is handled by Mailbox class, but StoreIncomingBlob is an
        // exception because of the way it is used in Mailbox.

        StoreIncomingBlob redoRecorder = new StoreIncomingBlob(mDigest, mMsgSize);
        redoRecorder.start(getTimestamp());
        redoRecorder.setBlobBodyInfo(mData, mPath, mVolumeId);
        redoRecorder.log();

        boolean success = false;
        try {
            Blob blob = StoreManager.getInstance().storeIncoming(mData, mDigest, mPath, mVolumeId);
            success = true;
        } finally {
        	if (success)
                redoRecorder.commit();
            else
                redoRecorder.abort();
        }
    }
}
