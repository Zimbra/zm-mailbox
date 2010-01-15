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
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class CreateContact extends RedoableOp {

    private int mId;
    private int mFolderId;
    private Map<String, String> mFields;
    private byte[] mBlob;
    private short mVolumeId = -1;
    private String mTags;

    public CreateContact() {
        mId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
    }

    public CreateContact(int mailboxId, int folderId, ParsedContact pc, String tags) {
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mFolderId = folderId;
        mFields = pc.getFields();
        mBlob = pc.getBlob();
        mTags = tags != null ? tags : "";
    }

    public void setContactId(int id) {
    	mId = id;
    }

    public int getContactId() {
    	return mId;
    }

    public void setVolumeId(short volumeId) {
    	mVolumeId = volumeId;
    }

    public short getVolumeId() {
    	return mVolumeId;
    }

    @Override
    public int getOpCode() {
		return OP_CREATE_CONTACT;
	}

    @Override
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("folder=").append(mFolderId);
        sb.append(", vol=").append(mVolumeId);
        sb.append(", tags=\"").append(mTags).append("\"");
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
        out.writeInt(mFolderId);
        out.writeShort(mVolumeId);
        out.writeUTF(mTags);
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
            int length = mBlob == null ? 0 : mBlob.length;
            out.writeInt(length);
            if (length > 0)
                out.write(mBlob);
        }
	}

    @Override
	protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mFolderId = in.readInt();
        mVolumeId = in.readShort();
        mTags = in.readUTF();
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
            int length = in.readInt();
            if (length > StoreIncomingBlob.MAX_BLOB_SIZE)
                throw new IOException("deserialized message size too large (" + length + " bytes)");
            mBlob = new byte[length];
            if (length > 0)
                in.readFully(mBlob);
        }
	}

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        try {
            ParsedContact pc = new ParsedContact(mFields, mBlob);
            mailbox.createContact(getOperationContext(), pc, mFolderId, mTags);
        } catch (ServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Contact " + mId + " already exists in mailbox " + mboxId);
            } else {
                throw e;
            }
        }
    }
}
