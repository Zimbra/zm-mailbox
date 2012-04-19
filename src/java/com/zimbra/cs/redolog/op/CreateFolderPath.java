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

import com.google.common.base.Strings;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
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

    private String path;
    private byte attrs;
    private MailItem.Type defaultView;
    private int flags;
    private long color;
    private String url;
    private Long date;
    private CustomMetadata custom;
    private int folderIds[];
    private String folderUuids[];

    public CreateFolderPath() {
        super(MailboxOperation.CreateFolderPath);
    }

    public CreateFolderPath(int mailboxId, String path, Folder.FolderOptions fopt) {
        this();
        setMailboxId(mailboxId);
        this.path = path == null ? "" : path;
        this.attrs = fopt.getAttributes();
        this.defaultView = fopt.getDefaultView();
        this.flags = fopt.getFlags();
        this.color = fopt.getColor().getValue();
        this.url = Strings.nullToEmpty(fopt.getUrl());
        this.date = fopt.getDate();
        this.custom = fopt.getCustomMetadata();
    }

    public int[] getFolderIds() {
        return folderIds;
    }

    public String[] getFolderUuids() {
        return folderUuids;
    }

    public void setFolderIdsAndUuids(int folderIds[], String folderUuids[]) {
        this.folderIds = folderIds;
        this.folderUuids = folderUuids;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("name=").append(path);
        sb.append(", attrs=").append(attrs).append(", view=").append(defaultView);
        sb.append(", flags=").append(flags).append(", color=").append(color);
        sb.append(", url=").append(url);
        sb.append(", date=").append(date);
        sb.append(", custom=").append(custom);
        if (folderIds != null) {
            sb.append(", folderIdsAndUuids=[");
            for (int i = 0; i < folderIds.length; i++) {
                sb.append(folderIds[i]).append(" (").append(folderUuids[i]).append(")");
                if (i < folderIds.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(path);
        out.writeByte(attrs);
        out.writeByte(defaultView.toByte());
        out.writeInt(flags);
        // color from byte to long in Version 1.27
        out.writeLong(color);
        out.writeUTF(url);
        // date as long in version 1.41
        out.writeBoolean(date != null);
        if (date != null) {
            out.writeLong(date);
        }
        // custom metadata as of version 1.41
        if (custom == null) {
            out.writeUTF(null);
        } else {
            out.writeUTF(custom.getSectionKey());
            out.writeUTF(custom.getSerializedValue());
        }

        if (folderIds != null) {
            out.writeInt(folderIds.length);
            for (int i = 0; i < folderIds.length; i++) {
                out.writeInt(folderIds[i]);
                if (getVersion().atLeast(1, 37)) {
                    out.writeUTF(folderUuids[i]);
                }
            }
        } else {
            out.writeInt(0);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        this.path = in.readUTF();
        this.attrs = in.readByte();
        this.defaultView = MailItem.Type.of(in.readByte());
        this.flags = in.readInt();
        if (getVersion().atLeast(1, 27)) {
            this.color = in.readLong();
        } else {
            this.color = in.readByte();
        }
        this.url = in.readUTF();
        if (getVersion().atLeast(1, 41)) {
            if (in.readBoolean()) {
                this.date = in.readLong();
            }
            String section = in.readUTF();
            if (section != null) {
                try {
                    this.custom = new CustomMetadata(section, in.readUTF());
                } catch (ServiceException e) {
                    mLog.warn("could not deserialize custom metadata for folder", e);
                }
            }
        }

        int numParentIds = in.readInt();
        if (numParentIds > 0) {
            this.folderIds = new int[numParentIds];
            this.folderUuids = new String[numParentIds];
            for (int i = 0; i < numParentIds; i++) {
                this.folderIds[i] = in.readInt();
                if (getVersion().atLeast(1, 37)) {
                    this.folderUuids[i] = in.readUTF();
                }
            }
        }
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);

        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setAttributes(attrs).setColor(Color.fromMetadata(color)).setDate(date);
        fopt.setDefaultView(defaultView).setFlags(flags).setUrl(url).setCustomMetadata(custom);

        try {
            mailbox.createFolder(getOperationContext(), path, fopt);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                mLog.info("Folder %s already exists in mailbox %d", path, mboxId);
            } else {
                throw e;
            }
        }
    }
}
