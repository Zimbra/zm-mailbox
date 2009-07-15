/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.store.file.VolumeServiceException;

public class DeleteVolume extends RedoableOp {

    private short mId;

    public DeleteVolume() {
    }

    public DeleteVolume(short id) {
        mId = id;
    }

    public int getOpCode() {
        return OP_DELETE_VOLUME;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeShort(mId);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readShort();
    }

    public void redo() throws Exception {
        try {
            Volume.getById(mId);  // make sure it exists
            Volume.delete(mId, getUnloggedReplay());
        } catch (VolumeServiceException e) {
            if (e.getCode() != VolumeServiceException.NO_SUCH_VOLUME)
                throw e;
        }
    }
}
