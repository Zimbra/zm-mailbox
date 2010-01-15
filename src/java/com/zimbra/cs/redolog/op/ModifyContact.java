/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on 2004. 12. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class ModifyContact extends RedoableOp {

    private int mId;
    private Map<String, String> mFields;
    private byte[] mBlob;
    private short mVolumeId = -1;

    public ModifyContact() {
        mId = UNKNOWN_ID;
    }

    public ModifyContact(int mailboxId, int id, ParsedContact pc) {
        setMailboxId(mailboxId);
        mId = id;
        mFields = pc.getFields();
        mBlob = pc.getBlob();
    }

    public void setVolumeId(short volumeId) {
        mVolumeId = volumeId;
    }

    public short getVolumeId() {
        return mVolumeId;
    }

    @Override
    public int getOpCode() {
        return OP_MODIFY_CONTACT;
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId);
        if (mFields != null && mFields.size() > 0) {
            sb.append(", attrs={");
            for (Map.Entry<String, String> entry : mFields.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                sb.append("\n    ").append(key).append(": ").append(value);
            }
            sb.append("\n}");
        }
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
//        out.writeBoolean(mReplace);
        int numAttrs = mFields != null ? mFields.size() : 0;
        out.writeShort((short) numAttrs);
        if (numAttrs > 0) {
            for (Map.Entry<String, String> entry : mFields.entrySet()) {
                out.writeUTF(entry.getKey());
                String value = entry.getValue();
                out.writeUTF(value != null ? value : "");
            }
        }
        if (getVersion().atLeast(1, 14)) {
            out.writeShort(mVolumeId);
            out.writeInt(mBlob == null ? 0 : mBlob.length);
            if (mBlob != null)
                out.write(mBlob);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        if (!getVersion().atLeast(1, 14))
            in.readBoolean();
        int numAttrs = in.readShort();
        if (numAttrs > 0) {
            mFields = new HashMap<String, String>(numAttrs);
            for (int i = 0; i < numAttrs; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                mFields.put(key, value);
            }
        }
        if (getVersion().atLeast(1, 14)) {
            mVolumeId = in.readShort();
            int length = in.readInt();
            if (length > 0)
                in.readFully(mBlob = new byte[length]);
        }
    }

    @Override
    public void redo() throws ServiceException {
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        ParsedContact pc = new ParsedContact(mFields, mBlob);
        mailbox.modifyContact(getOperationContext(), mId, pc);
    }
}
