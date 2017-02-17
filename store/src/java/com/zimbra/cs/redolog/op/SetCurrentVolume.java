/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.volume.VolumeManager;

public class SetCurrentVolume extends RedoableOp {

    private short mType;
    private short mId;

    public SetCurrentVolume() {
        super(MailboxOperation.SetCurrentVolume);
    }

    public SetCurrentVolume(short type, short id) {
        this();
        mType = type;
        mId = id;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("type=").append(mType);
        sb.append(", id=").append(mId);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeShort(mType);
        out.writeShort(mId);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mType = in.readShort();
        mId = in.readShort();
    }

    @Override
    public void redo() throws Exception {
        VolumeManager.getInstance().setCurrentVolume(mType, mId, getUnloggedReplay());
    }
}
