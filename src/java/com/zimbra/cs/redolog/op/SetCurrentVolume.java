/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
