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

/*
 * Created on 2004. 12. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class CreateFolder extends RedoableOp {

    private String mName;
    private byte mAttrs;
    private byte mDefaultView;
    private int mFlags;
    private byte mColor;
    private String mUrl;
    private int mFolderIds[];

    public CreateFolder() { }

    public CreateFolder(int mailboxId, String name, byte attrs, byte view, int flags, byte color, String url) {
        setMailboxId(mailboxId);
        mName = name == null ? "" : name;
        mAttrs = attrs;
        mDefaultView = view;
        mFlags = flags;
        mColor = color;
        mUrl = url == null ? "" : url;
    }

    public int[] getFolderIds() {
        return mFolderIds;
    }

    public void setFolderIds(int parentIds[]) {
        mFolderIds = parentIds;
    }

    public int getOpCode() {
        return OP_CREATE_FOLDER;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("name=").append(mName);
        sb.append(", attrs=").append(mAttrs).append(", view=").append(mDefaultView);
        sb.append(", flags=").append(mFlags).append(", color=").append(mColor);
        sb.append(", url=").append(mUrl);
        if (mFolderIds != null) {
            sb.append(", folderIds=[");
            for (int i = 0; i < mFolderIds.length; i++) {
                sb.append(mFolderIds[i]);
                if (i < mFolderIds.length - 1)
                    sb.append(", ");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mName);
        out.writeByte(mAttrs);
        out.writeByte(mDefaultView);
        out.writeInt(mFlags);
        out.writeByte(mColor);
        out.writeUTF(mUrl);
        if (mFolderIds != null) {
            out.writeInt(mFolderIds.length);
            for (int i = 0; i < mFolderIds.length; i++)
                out.writeInt(mFolderIds[i]);
        } else
            out.writeInt(0);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mName = in.readUTF();
        mAttrs = in.readByte();
        mDefaultView = in.readByte();
        mFlags = in.readInt();
        mColor = in.readByte();
        mUrl = in.readUTF();
        int numParentIds = in.readInt();
        if (numParentIds > 0) {
            mFolderIds = new int[numParentIds];
            for (int i = 0; i < numParentIds; i++)
                mFolderIds[i] = in.readInt();
        }
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        try {
            mailbox.createFolder(getOperationContext(), mName, mAttrs, mDefaultView, mFlags, mColor, mUrl);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Folder " + mName + " already exists in mailbox " + mboxId);
            } else
                throw e;
        }
    }
}
