/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class ImapCopyItem extends RedoableOp {

    private Map<Integer, Integer> mDestIds = new HashMap<Integer, Integer>();
    private byte mType;
    private int mDestFolderId;
    private short mDestVolumeId;

    public ImapCopyItem() {
        mType = MailItem.TYPE_UNKNOWN;
        mDestFolderId = 0;
        mDestVolumeId = -1;
    }

    public ImapCopyItem(int mailboxId, byte type, int folderId, short volumeId) {
        setMailboxId(mailboxId);
        mType = type;
        mDestFolderId = folderId;
        mDestVolumeId = volumeId;
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

    /**
     * Sets the volume ID for the copied blob.
     * @param volId
     */
    public void setDestVolumeId(short volId) {
        mDestVolumeId = volId;
    }

    public short getDestVolumeId() {
        return mDestVolumeId;
    }

    public int getOpCode() {
        return OP_IMAP_COPY_ITEM;
    }

    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("type=").append(mType);
        sb.append(", destFolder=").append(mDestFolderId);
        sb.append(", destVolumeId=").append(mDestVolumeId);
        sb.append(", [srcId, destId, srcImap]=");
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet())
            sb.append('[').append(entry.getKey()).append(',').append(entry.getValue()).append(']');
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeByte(mType);
        out.writeInt(mDestFolderId);
        out.writeShort(mDestVolumeId);
        out.writeInt(mDestIds.size());
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet()) {
            Integer srcId = entry.getKey();
            out.writeInt(srcId);
            out.writeInt(entry.getValue());
            out.writeInt(-1);                    // now unused; don't break the old format...
        }
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mType = in.readByte();
        mDestFolderId = in.readInt();
        mDestVolumeId = in.readShort();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            Integer srcId = in.readInt();
            mDestIds.put(srcId, in.readInt());
            in.readInt();                        // now unused; don't break the old format...
        }
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        int i = 0, itemIds[] = new int[mDestIds.size()];
        for (int id : mDestIds.keySet())
            itemIds[i++] = id;

        try {
            mbox.imapCopy(getOperationContext(), itemIds, mType, mDestFolderId);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Item is already in mailbox " + mboxId);
                return;
            } else
                throw e;
        }
    }
}
