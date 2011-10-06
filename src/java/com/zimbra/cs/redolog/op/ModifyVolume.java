/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.google.common.base.Objects;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;

public final class ModifyVolume extends RedoableOp {

    private short id;
    private short type;
    private String name;
    private String rootPath;

    private short mboxGroupBits;
    private short mboxBits;
    private short fileGroupBits;
    private short fileBits;

    private boolean compressBlobs;
    private long compressionThreshold;

    public ModifyVolume() {
        super(MailboxOperation.ModifyVolume);
    }

    public ModifyVolume(Volume volume) {
        this();
        id = volume.getId();
        type = volume.getType();
        name = volume.getName();
        rootPath = volume.getRootPath();

        mboxGroupBits = volume.getMboxGroupBits();
        mboxBits = volume.getMboxBits();
        fileGroupBits = volume.getFileGroupBits();
        fileBits = volume.getFileBits();

        compressBlobs = volume.isCompressBlobs();
        compressionThreshold = volume.getCompressionThreshold();
    }

    @Override
    protected String getPrintableData() {
        return Objects.toStringHelper(this).add("id", id).add("type", type).add("name", name).add("path", rootPath)
                .add("mboxGroupBits", mboxGroupBits).add("mboxBits", mboxBits)
                .add("fileGroupBits", fileGroupBits).add("fileBits", fileBits)
                .add("compressBlobs", compressBlobs).add("compressionThrehold", compressionThreshold)
                .toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeShort(id);
        out.writeShort(type);
        out.writeUTF(name);
        out.writeUTF(rootPath);
        out.writeShort(mboxGroupBits);
        out.writeShort(mboxBits);
        out.writeShort(fileGroupBits);
        out.writeShort(fileBits);
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        id = in.readShort();
        type = in.readShort();
        name = in.readUTF();
        rootPath = in.readUTF();
        mboxGroupBits = in.readShort();
        mboxBits = in.readShort();
        fileGroupBits = in.readShort();
        fileBits = in.readShort();
    }

    @Override
    public void redo() throws Exception {
        VolumeManager mgr = VolumeManager.getInstance();
        mgr.getVolume(id);  // make sure it exists
        Volume vol = Volume.builder().setId(id).setType(type).setName(name).setPath(rootPath, false)
                .setMboxGroupBits(mboxGroupBits).setMboxBit(mboxBits)
                .setFileGroupBits(fileGroupBits).setFileBits(fileBits)
                .setCompressBlobs(compressBlobs).setCompressionThreshold(compressionThreshold).build();
        mgr.update(vol, getUnloggedReplay());
    }
}
