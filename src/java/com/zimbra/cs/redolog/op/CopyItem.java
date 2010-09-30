/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on 2005. 5. 31.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CopyItem extends RedoableOp {

    private Map<Integer, Integer> mDestIds = new HashMap<Integer, Integer>();
    private byte mType;
    private int mDestFolderId;
    private boolean mFromDumpster;

    public CopyItem() {
        mType = MailItem.TYPE_UNKNOWN;
        mDestFolderId = 0;
    }

    public CopyItem(long mailboxId, byte type, int folderId, boolean fromDumpster) {
        setMailboxId(mailboxId);
        mType = type;
        mDestFolderId = folderId;
        mFromDumpster = fromDumpster;
    }

    /**
     * Sets the ID of the copied item.
     * @param destId
     */
    public void setDestId(int srcId, int destId) {
        mDestIds.put(srcId, destId);
    }

    public int getDestId(int srcId) {
        Integer destId = mDestIds.get(srcId);
        return destId == null ? -1 : destId;
    }

    @Override public int getOpCode() {
        return OP_COPY_ITEM;
    }

    @Override protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("type=").append(mType);
        sb.append(", destFolder=").append(mDestFolderId);
        sb.append(", [srcId, destId, srcImap]=");
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet())
            sb.append('[').append(entry.getKey()).append(',').append(entry.getValue()).append(']');
        if (mFromDumpster)
            sb.append(", fromDumpster=").append(mFromDumpster);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(-1);
        out.writeInt(-1);
        out.writeByte(mType);
        out.writeInt(mDestFolderId);
        out.writeShort((short) -1);
        out.writeInt(mDestIds.size());
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet()) {
            out.writeInt(entry.getKey());
            out.writeInt(entry.getValue());
        }
        if (getVersion().atLeast(1, 30))
            out.writeBoolean(mFromDumpster);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        // deal with old-style redologs
        int srcId = in.readInt();
        int destId = in.readInt();
        if (srcId > 0 && destId > 0)
            mDestIds.put(srcId, destId);

        mType = in.readByte();
        mDestFolderId = in.readInt();
        in.readShort();
        if (mDestIds.isEmpty()) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                srcId = in.readInt();
                mDestIds.put(srcId, in.readInt());
            }
        }
        if (getVersion().atLeast(1, 30))
            mFromDumpster = in.readBoolean();
        else
            mFromDumpster = false;
    }

    @Override public void redo() throws Exception {
        long mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        int i = 0, itemIds[] = new int[mDestIds.size()];
        for (int id : mDestIds.keySet())
            itemIds[i++] = id;

        try {
            mbox.copy(getOperationContext(), itemIds, mType, mDestFolderId, mFromDumpster);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Item is already in mailbox " + mboxId);
                return;
            } else {
                throw e;
            }
        }
    }
}
