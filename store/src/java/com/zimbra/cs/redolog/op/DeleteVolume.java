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
import com.zimbra.cs.volume.VolumeServiceException;

public final class DeleteVolume extends RedoableOp {

    private short mId;

    public DeleteVolume() {
        super(MailboxOperation.DeleteVolume);
    }

    public DeleteVolume(short id) {
        this();
        mId = id;
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mId);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeShort(mId);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readShort();
    }

    @Override
    public void redo() throws Exception {
        VolumeManager mgr = VolumeManager.getInstance();
        try {
            mgr.getVolume(mId);  // make sure it exists
            mgr.delete(mId, getUnloggedReplay());
        } catch (VolumeServiceException e) {
            if (e.getCode() != VolumeServiceException.NO_SUCH_VOLUME) {
                throw e;
            }
        }
    }
}
