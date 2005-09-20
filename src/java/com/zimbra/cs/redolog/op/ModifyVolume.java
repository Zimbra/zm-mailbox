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

package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.store.Volume;

public class ModifyVolume extends RedoableOp {

    private short mId;
    private short mType;
    private String mName;
    private String mRootPath;
    private String mIncomingMsgDir;

    private short mMboxGroupBits;
    private short mMboxBits;
    private short mFileGroupBits;
    private short mFileBits;
    
    private boolean mCompressBlobs;

    public ModifyVolume() {
    }

    public ModifyVolume(short id, short type, String name, String rootPath,
                        short mboxGroupBits, short mboxBits,
                        short fileGroupBits, short fileBits,
                        boolean compressBlobs) {
        mId = id;
        mType = type;
        mName = name;
        mRootPath = rootPath;
        
        mMboxGroupBits = mboxGroupBits;
        mMboxBits = mboxBits;
        mFileGroupBits = fileGroupBits;
        mFileBits = fileBits;
        
        mCompressBlobs = compressBlobs;
    }

    public int getOpCode() {
        return OP_MODIFY_VOLUME;
    }

    protected String getPrintableData() {
        Volume v = new Volume(mId, mType, mName, mRootPath,
                              mMboxGroupBits, mMboxBits,
                              mFileGroupBits, mFileBits,
                              mCompressBlobs);
        return v.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeShort(mId);
        out.writeShort(mType);
        writeUTF8(out, mName);
        writeUTF8(out, mRootPath);
        out.writeShort(mMboxGroupBits);
        out.writeShort(mMboxBits);
        out.writeShort(mFileGroupBits);
        out.writeShort(mFileBits);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readShort();
        mType = in.readShort();
        mName = readUTF8(in);
        mRootPath = readUTF8(in);
        mMboxGroupBits = in.readShort();
        mMboxBits = in.readShort();
        mFileGroupBits = in.readShort();
        mFileBits = in.readShort();
    }

    public void redo() throws Exception {
        Volume vol = Volume.getById(mId);
        Volume.update(mId, mType, mName, mRootPath,
                      mMboxGroupBits, mMboxBits,
                      mFileGroupBits, mFileBits,
                      mCompressBlobs);
    }
}
