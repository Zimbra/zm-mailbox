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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 12. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class CreateContact extends RedoableOp {

    private int mId;
    private int mFolderId;
    private Map<String, String> mAttrs;
    private String mTags;
    private short mVolumeId = -1;

    public CreateContact() {
        mId = UNKNOWN_ID;
        mFolderId = UNKNOWN_ID;
    }

    public CreateContact(int mailboxId, int folderId, Map<String, String> attrs, String tags) {
        setMailboxId(mailboxId);
        mId = UNKNOWN_ID;
        mFolderId = folderId;
        mAttrs = attrs;
        mTags = tags != null ? tags : "";
    }

    public void setContactId(int id) {
    	mId = id;
    }

    public int getContactId() {
    	return mId;
    }

    public void setVolumeId(short volId) {
    	mVolumeId = volId;
    }

    public short getVolumeId() {
    	return mVolumeId;
    }

    /* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getOperationCode()
	 */
	public int getOpCode() {
		return OP_CREATE_CONTACT;
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#getPrintableData()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("folder=").append(mFolderId);
        sb.append(", vol=").append(mVolumeId);
        sb.append(", tags=\"").append(mTags).append("\"");
        if (mAttrs != null && mAttrs.size() > 0) {
            sb.append(", attrs={");
            for (Iterator it = mAttrs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                sb.append("\n    ").append(key).append(": ").append(value);
            }
            sb.append("\n}");
        }
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#serializeData(java.io.RedoLogOutput)
	 */
	protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mFolderId);
        out.writeShort(mVolumeId);
        out.writeUTF(mTags);
        int numAttrs = mAttrs != null ? mAttrs.size() : 0;
        out.writeShort((short) numAttrs);
        for (Iterator it = mAttrs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            out.writeUTF((String) entry.getKey());
            String value = (String) entry.getValue();
            out.writeUTF(value != null ? value : "");
        }
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.op.RedoableOp#deserializeData(java.io.RedoLogInput)
	 */
	protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mFolderId = in.readInt();
        mVolumeId = in.readShort();
        mTags = in.readUTF();
        int numAttrs = in.readShort();
        if (numAttrs > 0) {
            mAttrs = new HashMap<String, String>(numAttrs);
            for (int i = 0; i < numAttrs; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                mAttrs.put(key, value);
            }
        }
	}

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        try {
            mailbox.createContact(getOperationContext(), mAttrs, mFolderId, mTags);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Contact " + mId + " already exists in mailbox " + mboxId);
            } else
                throw e;
        }
    }
}
