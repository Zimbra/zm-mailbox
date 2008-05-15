/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.store.VolumeServiceException;

public class CreateVolume extends RedoableOp {

    private short mId = Volume.ID_NONE;
    private short mType;
    private String mName;
    private String mRootPath;

    private short mMboxGroupBits;
    private short mMboxBits;
    private short mFileGroupBits;
    private short mFileBits;
    
    private boolean mCompressBlobs;
    private long mCompressionThreshold;

    public CreateVolume() {
    }

    public CreateVolume(short type, String name, String rootPath,
                        short mboxGroupBits, short mboxBits,
                        short fileGroupBits, short fileBits,
                        boolean compressBlobs, long compressionThreshold) {
        mType = type;
        mName = name;
        mRootPath = rootPath;
        
        mMboxGroupBits = mboxGroupBits;
        mMboxBits = mboxBits;
        mFileGroupBits = fileGroupBits;
        mFileBits = fileBits;
        mCompressBlobs = compressBlobs;
        mCompressionThreshold = compressionThreshold;
    }

    public void setId(short id) {
        mId = id;
    }

    public int getOpCode() {
        return OP_CREATE_VOLUME;
    }

    protected String getPrintableData() {
        Volume v = new Volume(mId, mType, mName, mRootPath,
                              mMboxGroupBits, mMboxBits,
                              mFileGroupBits, mFileBits,
                              mCompressBlobs, mCompressionThreshold);
        return v.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeShort(mId);
        out.writeShort(mType);
        out.writeUTF(mName);
        out.writeUTF(mRootPath);
        out.writeShort(mMboxGroupBits);
        out.writeShort(mMboxBits);
        out.writeShort(mFileGroupBits);
        out.writeShort(mFileBits);
        out.writeBoolean(mCompressBlobs);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readShort();
        mType = in.readShort();
        mName = in.readUTF();
        mRootPath = in.readUTF();
        mMboxGroupBits = in.readShort();
        mMboxBits = in.readShort();
        mFileGroupBits = in.readShort();
        mFileBits = in.readShort();
        mCompressBlobs = in.readBoolean();
    }

    public void redo() throws Exception {
        try {
            Volume vol = Volume.getById(mId);
            if (vol != null) {
                mLog.info("Volume " + mId + " already exists");
                return;
            }
        } catch (VolumeServiceException e) {
            if (e.getCode() != VolumeServiceException.NO_SUCH_VOLUME)
                throw e;
        }
        try {
            Volume.create(mId, mType, mName, mRootPath,
                          mMboxGroupBits, mMboxBits,
                          mFileGroupBits, mFileBits,
                          mCompressBlobs, mCompressionThreshold,
                          getUnloggedReplay());
        } catch (VolumeServiceException e) {
            if (e.getCode() == VolumeServiceException.ALREADY_EXISTS)
                mLog.info("Volume " + mId + " already exists");
            else
                throw e;
        }
    }
}
