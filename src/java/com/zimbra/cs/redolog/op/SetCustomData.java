/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class SetCustomData extends RedoableOp {

    private int itemId;
    private MailItem.Type type;
    private CustomMetadata custom;

    public SetCustomData() {
        super(MailboxOperation.SetCustomData);
    }

    public SetCustomData(int mailboxId, int id, MailItem.Type type, CustomMetadata custom) {
        this();
        setMailboxId(mailboxId);
        this.itemId = id;
        this.type = type;
        this.custom = custom;
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(itemId).append(", data=").append(custom);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(itemId);
        out.writeByte(type.toByte());
        out.writeUTF(custom.getSectionKey());
        out.writeUTF(custom.getSerializedValue());
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        this.itemId = in.readInt();
        this.type = MailItem.Type.of(in.readByte());
        try {
            String extendedKey = in.readUTF();
            this.custom = new CustomMetadata(extendedKey, in.readUTF());
        } catch (ServiceException e) {
            mLog.warn("could not deserialize custom metadata for folder", e);
        }
    }

    @Override
    public void redo() throws Exception {
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        mailbox.setCustomData(getOperationContext(), itemId, type, custom);
    }
}
