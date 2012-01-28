/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

public class ImapCopyItem extends RedoableOp {

    private Map<Integer, Integer> mDestIds = new HashMap<Integer, Integer>();
    private Map<Integer, String> mDestUuids = new HashMap<Integer, String>();
    private MailItem.Type type;
    private int mDestFolderId;

    public ImapCopyItem() {
        super(MailboxOperation.ImapCopyItem);
        type = MailItem.Type.UNKNOWN;
        mDestFolderId = 0;
    }

    public ImapCopyItem(int mailboxId, MailItem.Type type, int folderId) {
        this();
        setMailboxId(mailboxId);
        this.type = type;
        mDestFolderId = folderId;
    }

    /**
     * Sets the ID of the copied item.
     * @param destId
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
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeByte(type.toByte());
        out.writeInt(mDestFolderId);
        out.writeShort((short) -1);
        out.writeInt(mDestIds.size());
        for (Map.Entry<Integer, Integer> entry : mDestIds.entrySet()) {
            Integer srcId = entry.getKey();
            out.writeInt(srcId);
            out.writeInt(entry.getValue());
            out.writeInt(-1);                    // now unused; don't break the old format...
            if (getVersion().atLeast(1, 37)) {
                out.writeUTF(mDestUuids.get(srcId));
            }
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        type = MailItem.Type.of(in.readByte());
        mDestFolderId = in.readInt();
        in.readShort();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            Integer srcId = in.readInt();
            mDestIds.put(srcId, in.readInt());
            in.readInt();                        // now unused; don't break the old format...
            if (getVersion().atLeast(1, 37)) {
                mDestUuids.put(srcId,  in.readUTF());
            }
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
            mbox.imapCopy(getOperationContext(), itemIds, type, mDestFolderId);
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Item is already in mailbox " + mboxId);
                return;
            } else
                throw e;
        }
    }
}
