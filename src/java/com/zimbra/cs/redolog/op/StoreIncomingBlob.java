/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.StoreManager;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StoreIncomingBlob extends RedoableOp {

    static final int MAX_BLOB_SIZE = 100 * 1024 * 1024;  // 100MB size limit
    static final int MAX_MAILBOX_LIST_LENGTH = 1000000;

    private String mDigest;
    private String mPath;           // full path to blob file
    private short mVolumeId = -1;   // volume on which the blob is saved
    private int mMsgSize;           // original, uncompressed blob size in bytes
    private RedoableOpData mData;
    private List<Integer> mMailboxIdList;

    public StoreIncomingBlob() {
    }

    public StoreIncomingBlob(String digest, int msgSize,
    						 List<Integer> mboxIdList) {
        setMailboxId(MAILBOX_ID_ALL);
        mDigest = digest != null ? digest : "";
        mMsgSize = msgSize;
        mMailboxIdList = mboxIdList;
    }

    public int getOpCode() {
        return OP_STORE_INCOMING_BLOB;
    }

    public List<Integer> getMailboxIdList() {
    	return mMailboxIdList;
    }

    public void setMailboxIdList(List<Integer> list) {
        mMailboxIdList = list;
    }

    public void setBlobBodyInfo(byte[] data, String path, short volumeId) {
        mData = new RedoableOpData(data);
        mPath = path;
        mVolumeId = volumeId;
    }
    
    public void setBlobBodyInfo(File file, short volumeId) {
        mData = new RedoableOpData(file);
        mPath = file.getPath();
        mVolumeId = volumeId;
    }
    
    private void setBlobBodyInfo(InputStream dataStream, int dataLength, String path, short volumeId) {
        mData = new RedoableOpData(dataStream, dataLength);
        mPath = path;
        mVolumeId = volumeId;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("blobDigest=\"");
        sb.append(mDigest).append("\", size=").append(mMsgSize);
        sb.append(", vol=").append(mVolumeId);
        sb.append(", path=").append(mPath);
        sb.append(", mbox=[");
        if (mMailboxIdList != null) {
	        int i = 0;
	        for (Integer mboxId : mMailboxIdList) {
	        	if (i > 0)
	        		sb.append(", ");
	        	sb.append(mboxId.toString());
	        	i++;
	        }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public InputStream getAdditionalDataStream()
    throws IOException {
        return mData.getInputStream();
    }
    
    protected void serializeData(RedoLogOutput out) throws IOException {
    	if (getVersion().atLeast(1, 0)) {
    		if (mMailboxIdList != null) {
				out.writeInt(mMailboxIdList.size());
				for (Integer mboxId : mMailboxIdList)
					out.writeInt(mboxId.intValue());
    		} else
    			out.writeInt(0);
    	}
        out.writeUTF(mDigest);
        out.writeUTF(mPath);
        out.writeShort(mVolumeId);
        out.writeInt(mMsgSize);
        // Eventually we may differentiate between the message size and compressed
        // size but currently they're the same.
        out.writeInt(mMsgSize);
        
        // During serialize, do not serialize the blob data buffer.
        // Blob buffer is handled by getSerializedByteArrayVector()
        // implementation in this class as the last vector element.
        // Consequently, in the serialized stream blob data comes last.
        // deserializeData() should take this into account.
        //out.write(mData);  // Don't do this here!
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
    	if (getVersion().atLeast(1, 0)) {
    		int listLen = in.readInt();
    		if (listLen > MAX_MAILBOX_LIST_LENGTH)
    			throw new IOException("Deserialized mailbox list too large (" +
    								  listLen + ")");
    		if (listLen >= 1) {
	    		List<Integer> list = new ArrayList<Integer>(listLen);
	    		for (int i = 0; i < listLen; i++)
	    			list.add(new Integer(in.readInt()));
	    		mMailboxIdList = list;
    		}
    	}
        mDigest = in.readUTF();
        mPath = in.readUTF();
        mVolumeId = in.readShort();
        mMsgSize = in.readInt();
        mMsgSize = in.readInt();  // Serialization code wrote the size twice

        // mData must be the last thing deserialized.  See comments in
        // serializeData().
        long pos = in.getFilePointer();
        mData = new RedoableOpData(new File(in.getPath()), pos, mMsgSize);
        
        // Now that we have a stream to the data, skip to the next op.
        int numSkipped = in.skipBytes(mMsgSize);
        if (numSkipped != mMsgSize) {
            String msg = String.format("Attempted to skip %d bytes at position %d in %s, but actually skipped %d.",
                mMsgSize, pos, in.getPath(), numSkipped);
            throw new IOException(msg);
        }
    }

    public void redo() throws Exception {
        // Execution of redo is logged to current redo logger.  For most other
        // ops this is handled by Mailbox class, but StoreIncomingBlob is an
        // exception because of the way it is used in Mailbox.

        StoreIncomingBlob redoRecorder = null;
        if (!getUnloggedReplay()) {
            redoRecorder =
            	new StoreIncomingBlob(mDigest, mMsgSize, mMailboxIdList);
            redoRecorder.start(getTimestamp());
            redoRecorder.setBlobBodyInfo(mData.getInputStream(), mData.getLength(), mPath, mVolumeId);
            redoRecorder.log();
        }

        boolean success = false;
        try {
            StoreManager.getInstance().storeIncoming(mData.getInputStream(), mMsgSize, mPath, mVolumeId);
            success = true;
        } finally {
            if (redoRecorder != null) {
            	if (success)
                    redoRecorder.commit();
                else
                    redoRecorder.abort();
            }
        }
    }
}
