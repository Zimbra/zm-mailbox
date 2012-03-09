/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.mailbox.Color;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

public class CreateFolder extends RedoableOp {

    private String mName;
    private int mParentId;
    private byte mAttrs;
    private MailItem.Type defaultView;
    private int mFlags;
    private long mColor;
    private String mUrl;
    private int mFolderId;
    private String mFolderUuid;
    private Integer mDate;

    public CreateFolder() {
        super(MailboxOperation.CreateFolder);
    }

    public CreateFolder(int mailboxId, String name, int parentId, MailItem.Type view, int flags,
            Color color, String url) {
        this(mailboxId, name, parentId, (byte) 0, view, flags, color, url, null);
    }

    public CreateFolder(int mailboxId, String name, int parentId, byte attrs, MailItem.Type view, int flags,
            Color color, String url) {
        this(mailboxId, name, parentId, attrs, view, flags, color, url, null);

    }
    public CreateFolder(int mailboxId, String name, int parentId, byte attrs, MailItem.Type view, int flags,
        Color color, String url, Integer date) {
        this();
        setMailboxId(mailboxId);
        mName = name == null ? "" : name;
        mParentId = parentId;
        mAttrs = attrs;
        defaultView = view;
        mFlags = flags;
        mColor = color.getValue();
        mUrl = url == null ? "" : url;
        mDate = date;
    }

    public int getFolderId() {
        return mFolderId;
    }

    public String getFolderUuid() {
        return mFolderUuid;
    }

    public void setFolderIdAndUuid(int folderId, String uuid) {
        mFolderId = folderId;
        mFolderUuid = uuid;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("name=").append(mName);
        sb.append(", parent=").append(mParentId);
        sb.append(", attrs=").append(mAttrs);
        sb.append(", view=").append(defaultView);
        sb.append(", flags=").append(mFlags).append(", color=").append(mColor);
        sb.append(", url=").append(mUrl).append(", id=").append(mFolderId);
        sb.append(", uuid=").append(mFolderUuid);
        sb.append(", date=").append(mDate);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mName);
        out.writeInt(mParentId);
        if (getVersion().atLeast(1, 19)) out.writeByte(mAttrs);
        out.writeByte(defaultView.toByte());
        out.writeInt(mFlags);
        // mColor from byte to long in Version 1.27
        out.writeLong(mColor);
        out.writeUTF(mUrl);
        out.writeInt(mFolderId);
        if (getVersion().atLeast(1, 37)) {
            out.writeUTF(mFolderUuid);
        }
        if (getVersion().atLeast(1, 39)) {
            out.writeBoolean(mDate != null);
            if (mDate != null) {
                out.writeInt(mDate.intValue());
            }
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mName = in.readUTF();
        mParentId = in.readInt();
        if (getVersion().atLeast(1, 19)) {
            mAttrs = in.readByte();
        }
        defaultView = MailItem.Type.of(in.readByte());
        mFlags = in.readInt();
        if (getVersion().atLeast(1, 27)) {
            mColor = in.readLong();
        } else {
            mColor = in.readByte();
        }
        mUrl = in.readUTF();
        mFolderId = in.readInt();
        if (getVersion().atLeast(1, 37)) {
            mFolderUuid = in.readUTF();
        }
        if (getVersion().atLeast(1, 39)) {
            boolean hasDate = in.readBoolean();
            if (hasDate) {
                mDate = new Integer(in.readInt());
            }
        }
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mailbox.createFolder(getOperationContext(), mName, mParentId, mAttrs, defaultView, mFlags,
                    Color.fromMetadata(mColor), mUrl, mDate);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Folder " + mName + " already exists in mailbox " + mboxId);
            } else {
                throw e;
            }
        }
    }
}
