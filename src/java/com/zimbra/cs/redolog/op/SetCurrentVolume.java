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

public class SetCurrentVolume extends RedoableOp {

    private short mType;
    private short mId;

    public SetCurrentVolume() {
    }

    public SetCurrentVolume(short type, short id) {
        mType = type;
        mId = id;
    }

    public int getOpCode() {
        return OP_SET_CURRENT_VOLUME;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("type=").append(mType);
        sb.append(", id=").append(mId);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeShort(mType);
        out.writeShort(mId);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mType = in.readShort();
        mId = in.readShort();
    }

    public void redo() throws Exception {
        Volume.setCurrentVolume(mType, mId, getUnloggedReplay());
    }
}
