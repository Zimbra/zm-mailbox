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

public class CreateFolder extends RedoableOp {

    private String name;
    private int parentId;
    private byte attrs;
    private MailItem.Type defaultView;
    private int flags;
    private long color;
    private String url;
    private Long date;
    private CustomMetadata custom;
    private int folderId;
    private String folderUuid;

    public CreateFolder() {
        super(MailboxOperation.CreateFolder);
    }

    public CreateFolder(int mailboxId, String name, int parentId, Folder.FolderOptions fopt) {
        this();
        setMailboxId(mailboxId);
        this.name = name == null ? "" : name;
        this.parentId = parentId;
        this.attrs = fopt.getAttributes();
        this.defaultView = fopt.getDefaultView();
        this.flags = fopt.getFlags();
        this.color = fopt.getColor().getValue();
        this.url = Strings.nullToEmpty(fopt.getUrl());
        this.date = fopt.getDate();
        this.custom = fopt.getCustomMetadata();
    }

    public int getFolderId() {
        return folderId;
    }

    public String getFolderUuid() {
        return folderUuid;
    }

    public void setFolderIdAndUuid(int folderId, String uuid) {
        this.folderId = folderId;
        this.folderUuid = uuid;
    }

    @Override
    protected String getPrintableData() {
        StringBuilder sb = new StringBuilder("name=").append(name);
        sb.append(", parent=").append(parentId);
        sb.append(", attrs=").append(attrs);
        sb.append(", view=").append(defaultView);
        sb.append(", flags=").append(flags).append(", color=").append(color);
        sb.append(", url=").append(url).append(", id=").append(folderId);
        sb.append(", uuid=").append(folderUuid);
        sb.append(", date=").append(date);
        sb.append(", custom=").append(custom);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(parentId);
        // attrs as of version 1.19
        out.writeByte(attrs);
        out.writeByte(defaultView.toByte());
        out.writeInt(flags);
        // color from byte to long in Version 1.27
        out.writeLong(color);
        out.writeUTF(url);
        out.writeInt(folderId);
        // folder UUID as of version 1.37
        out.writeUTF(folderUuid);
        // date as long in version 1.40
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
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        this.name = in.readUTF();
        this.parentId = in.readInt();
        if (getVersion().atLeast(1, 19)) {
            this.attrs = in.readByte();
        }
        this.defaultView = MailItem.Type.of(in.readByte());
        this.flags = in.readInt();
        if (getVersion().atLeast(1, 27)) {
            this.color = in.readLong();
        } else {
            this.color = in.readByte();
        }
        this.url = in.readUTF();
        this.folderId = in.readInt();
        if (getVersion().atLeast(1, 37)) {
            this.folderUuid = in.readUTF();
        }
        if (getVersion().atLeast(1, 39)) {
            if (in.readBoolean()) {
                if (getVersion().atLeast(1, 40)) {
                    this.date = in.readLong();
                } else {
                    this.date = ((long) in.readInt()) * 1000;
                }
            }
        }
        if (getVersion().atLeast(1, 41)) {
            String section = in.readUTF();
            if (section != null) {
                try {
                    this.custom = new CustomMetadata(section, in.readUTF());
                } catch (ServiceException e) {
                    mLog.warn("could not deserialize custom metadata for folder", e);
                }
            }
        }
    }

    @Override
    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);

        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setAttributes(attrs).setColor(Color.fromMetadata(color)).setDate(date).setFlags(flags);
        fopt.setUrl(url).setDefaultView(defaultView).setUuid(folderUuid).setCustomMetadata(custom);

        try {
            mailbox.createFolder(getOperationContext(), name, parentId, fopt);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                mLog.info("Folder %s already exists in mailbox %d", name, mboxId);
            } else {
                throw e;
            }
        }
    }
}
