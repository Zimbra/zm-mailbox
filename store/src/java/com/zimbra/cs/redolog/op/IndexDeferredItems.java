/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/**
 * This class is deprecated and obsolete.
 */
public class IndexDeferredItems extends RedoableOp {
    
    private int[] mItemIds = null;
    private byte[] mItemTypes = null;

    public IndexDeferredItems() {
        super(MailboxOperation.IndexDeferredItems);
    }
    
    public void setIds(int[] itemIds, byte[] itemTypes) {
        mItemIds = itemIds;
        mItemTypes = itemTypes;
        if (mItemIds.length != mItemTypes.length)
            throw new IllegalArgumentException("ItemIds and ItemTypes arrays must be same size");
    }
    
    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mItemIds.length);
        for (int i = 0; i < mItemIds.length; i++)
            out.writeInt(mItemIds[i]);
        for (int i = 0; i < mItemIds.length; i++)
            out.writeByte(mItemTypes[i]);
    }
    
    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        int count = in.readInt();
        mItemIds = new int[count];
        mItemTypes = new byte[count];
        for (int i = 0; i < count; i++)
            mItemIds[i] = in.readInt();
        for (int i = 0; i < count; i++)
            mItemTypes[i] = in.readByte();
    }
    
    public boolean deferCrashRecovery() {
        return true;
    }
    
    public int[] getItemIds() { return mItemIds; }
    public byte[] getItemTypes() { return mItemTypes; }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder();
        for (Integer i : mItemIds) {
            sb.append(i).append(',');
        }
        return sb.toString();
    }

    @Override
    public void redo() throws Exception {
        // do nothing.
    }

}
