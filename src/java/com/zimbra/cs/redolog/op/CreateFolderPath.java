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

/**
 * @since 2004. 12. 13.
 */
public class CreateFolderPath extends RedoableOp {

    private String mPath;
    private byte mAttrs;
    private MailItem.Type defaultView;
    private int mFlags;
    private long mColor;
    private String mUrl;
    private int mFolderIds[];
    private String mFolderUuids[];

    public CreateFolderPath() {
        super(MailboxOperation.CreateFolderPath);
    }

    public CreateFolderPath(int mailboxId, String name, byte attrs, MailItem.Type view, int flags,
            Color color, String url) {
        this();
        setMailboxId(mailboxId);
        mPath = name == null ? "" : name;
        mAttrs = attrs;
        defaultView = view;
        mFlags = flags;
        mColor = color.getValue();
        mUrl = url == null ? "" : url;
    }

    public int[] getFolderIds() {
        return mFolderIds;
    }

    public String[] getFolderUuids() {
        return mFolderUuids;
    }

    public void setFolderIdsAndUuids(int folderIds[], String folderUuids[]) {
        mFolderIds = folderIds;
        mFolderUuids = folderUuids;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("name=").append(mPath);
        sb.append(", attrs=").append(mAttrs).append(", view=").append(defaultView);
        sb.append(", flags=").append(mFlags).append(", color=").append(mColor);
        sb.append(", url=").append(mUrl);
        if (mFolderIds != null) {
            sb.append(", folderIdsAndUuids=[");
            for (int i = 0; i < mFolderIds.length; i++) {
                sb.append(mFolderIds[i]).append(" (").append(mFolderUuids[i]).append(")");
                if (i < mFolderIds.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(mPath);
        out.writeByte(mAttrs);
        out.writeByte(defaultView.toByte());
        out.writeInt(mFlags);
        // mColor from byte to long in Version 1.27
        out.writeLong(mColor);
        out.writeUTF(mUrl);
        if (mFolderIds != null) {
            out.writeInt(mFolderIds.length);
            for (int i = 0; i < mFolderIds.length; i++) {
                out.writeInt(mFolderIds[i]);
                if (getVersion().atLeast(1, 37)) {
                    out.writeUTF(mFolderUuids[i]);
                }
            }
        } else {
            out.writeInt(0);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mPath = in.readUTF();
        mAttrs = in.readByte();
        defaultView = MailItem.Type.of(in.readByte());
        mFlags = in.readInt();
        if (getVersion().atLeast(1, 27)) {
            mColor = in.readLong();
        } else {
            mColor = in.readByte();
        }
        mUrl = in.readUTF();
        int numParentIds = in.readInt();
        if (numParentIds > 0) {
            mFolderIds = new int[numParentIds];
            mFolderUuids = new String[numParentIds];
            for (int i = 0; i < numParentIds; i++) {
                mFolderIds[i] = in.readInt();
                if (getVersion().atLeast(1, 37)) {
                    mFolderUuids[i] = in.readUTF();
                }
            }
        }
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);

        try {
            mailbox.createFolder(getOperationContext(), mPath, mAttrs, defaultView, mFlags,
                    Color.fromMetadata(mColor), mUrl);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Folder " + mPath + " already exists in mailbox " + mboxId);
            } else {
                throw e;
            }
        }
    }
}
