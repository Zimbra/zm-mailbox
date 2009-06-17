/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateTag extends RedoableOp {

    private int mTagId;
    private String mName;
    private byte mColor;

    public CreateTag() {
        mTagId = UNKNOWN_ID;
        mColor = 0;
    }

    public CreateTag(long mailboxId, String name, byte color) {
        setMailboxId(mailboxId);
        mTagId = UNKNOWN_ID;
        mName = name != null ? name : "";
        mColor = color;
    }

    public int getTagId() {
        return mTagId;
    }

    public void setTagId(int tagId) {
        mTagId = tagId;
    }

    @Override public int getOpCode() {
        return OP_CREATE_TAG;
    }

    @Override protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mTagId);
        sb.append(", name=").append(mName).append(", color=").append(mColor);
        return sb.toString();
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mTagId);
        out.writeUTF(mName);
        out.writeByte(mColor);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mTagId = in.readInt();
        mName = in.readUTF();
        mColor = in.readByte();
    }

    @Override public void redo() throws Exception {
        long mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mbox.createTag(getOperationContext(), mName, mColor);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Tag " + mTagId + " already exists in mailbox " + mboxId);
            } else {
                throw e;
            }
        }
    }
}
