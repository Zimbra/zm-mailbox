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
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @since 2005. 5. 31.
 */
public class CopyItem extends RedoableOp {

    private Map<Integer, Integer> mDestIds = new HashMap<Integer, Integer>();
    private Map<Integer, String> mDestUuids = new HashMap<Integer, String>();
    private MailItem.Type type;
    private int mDestFolderId;
    private boolean mFromDumpster;  // false in this class, true in subclass RecoverItem

    public CopyItem() {
        super(MailboxOperation.CopyItem);
        type = MailItem.Type.UNKNOWN;
        mDestFolderId = 0;
    }

    public CopyItem(int mailboxId, MailItem.Type type, int folderId) {
        this();
        setMailboxId(mailboxId);
        this.type = type;
        mDestFolderId = folderId;
    }

    /**
     * Sets the ID and UUID of the copied item.
     */
    public void setDest(int srcId, int destId, String destUuid) {
        mDestIds.put(srcId, destId);
        mDestUuids.put(srcId, destUuid);
    }

    public int getDestId(int srcId) {
        Integer destId = mDestIds.get(srcId);
        return destId == null ? -1 : destId;
    }

    public String getDestUuid(int srcId) {
        return mDestUuids.get(srcId);
    }

    protected void setFromDumpster(boolean fromDumpster) {
        mFromDumpster = fromDumpster;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("type=").append(type);
        sb.append(", destFolder=").append(mDestFolderId);
        sb.append(", [srcId, destId, destUuid]=");
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet()) {
            int srcId = entry.getKey();
            sb.append('[').append(srcId).append(',').append(entry.getValue());
            sb.append(',').append(mDestUuids.get(srcId)).append(']');
        }
        if (mFromDumpster)
            sb.append(", fromDumpster=").append(mFromDumpster);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(-1);
        out.writeInt(-1);
        out.writeByte(type.toByte());
        out.writeInt(mDestFolderId);
        out.writeShort((short) -1);
        out.writeInt(mDestIds.size());
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet()) {
            int srcId = entry.getKey();
            out.writeInt(srcId);
            out.writeInt(entry.getValue());
            if (getVersion().atLeast(1, 37)) {
                out.writeUTF(mDestUuids.get(srcId));
            }
        }
        if (getVersion().atLeast(1, 30)) {
            out.writeBoolean(mFromDumpster);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        // deal with old-style redologs
        int srcId = in.readInt();
        int destId = in.readInt();
        if (srcId > 0 && destId > 0) {
            mDestIds.put(srcId, destId);
        }
        type = MailItem.Type.of(in.readByte());
        mDestFolderId = in.readInt();
        in.readShort();
        if (mDestIds.isEmpty()) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                srcId = in.readInt();
                mDestIds.put(srcId, in.readInt());
                if (getVersion().atLeast(1, 37)) {
                    mDestUuids.put(srcId, in.readUTF());
                }
            }
        }
        if (getVersion().atLeast(1, 30)) {
            mFromDumpster = in.readBoolean();
        } else {
            mFromDumpster = false;
        }
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        int i = 0, itemIds[] = new int[mDestIds.size()];
        for (int id : mDestIds.keySet()) {
            itemIds[i++] = id;
        }
        try {
            if (!mFromDumpster)
                mbox.copy(getOperationContext(), itemIds, type, mDestFolderId);
            else
                mbox.recover(getOperationContext(), itemIds, type, mDestFolderId);
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
