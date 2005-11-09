/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
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

    protected void serializeData(DataOutput out) throws IOException {
        out.writeShort(mType);
        out.writeShort(mId);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mType = in.readShort();
        mId = in.readShort();
    }

    public void redo() throws Exception {
        Volume.setCurrentVolume(mType, mId);
    }
}
