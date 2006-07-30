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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public class ImapCopyItem extends RedoableOp {

    private Map<Integer, Integer> mDestIds = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> mSrcImapIds = new HashMap<Integer, Integer>();
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

    /**
     * Sets the IMAP UID for the moved item.
     * @param volId
     */
    public void setSrcImapId(int srcId, int srcImapId) {
        mSrcImapIds.put(srcId, srcImapId);
    }

    public int getSrcImapId(int srcId) {
        Integer srcImapId = mSrcImapIds.get(srcId);
        return srcImapId == null ? -1 : srcImapId;
    }

    public int getOpCode() {
        return OP_IMAP_COPY_ITEM;
    }

    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("type=").append(mType);
        sb.append(", destFolder=").append(mDestFolderId);
        sb.append(", destVolumeId=").append(mDestVolumeId);
        sb.append(", [srcId, destId, srcImap]=");
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet()) {
            Integer srcId = entry.getKey();
            sb.append('[').append(srcId).append(',').append(entry.getValue()).append(',').append(mSrcImapIds.get(srcId)).append(']');
        }
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeByte(mType);
        out.writeInt(mDestFolderId);
        out.writeShort(mDestVolumeId);
        out.writeInt(mDestIds.size());
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet()) {
            Integer srcId = entry.getKey();
            out.writeInt(srcId);
            out.writeInt(entry.getValue());
            out.writeInt(getSrcImapId(srcId));
        }
    }

    protected void deserializeData(DataInput in) throws IOException {
        mType = in.readByte();
        mDestFolderId = in.readInt();
        mDestVolumeId = in.readShort();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            Integer srcId = in.readInt();
            mDestIds.put(srcId, in.readInt());
            mSrcImapIds.put(srcId, in.readInt());
        }
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);

        int i = 0, itemIds[] = new int[mDestIds.size()];
        for (int id : mDestIds.keySet())
            itemIds[i] = id;

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
