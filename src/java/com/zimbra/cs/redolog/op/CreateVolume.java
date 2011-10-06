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
import com.zimbra.cs.volume.VolumeServiceException;

public final class CreateVolume extends RedoableOp {

    private short id = Volume.ID_NONE;
    private short type;
    private String name;
    private String rootPath;

    private short mboxGroupBits;
    private short mboxBits;
    private short fileGroupBits;
    private short fileBits;

    private boolean compressBlobs;
    private long compressionThreshold;

    public CreateVolume() {
        super(MailboxOperation.CreateVolume);
    }

    public CreateVolume(Volume volume) {
        this();
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

    public void setId(short id) {
        this.id = id;
    }

    @Override
    protected String getPrintableData() {
        return Objects.toStringHelper(this)
                .add("id", id).add("type", type).add("name", name).add("path", rootPath)
                .add("mboxGroupBits", mboxGroupBits).add("mboxBit", mboxBits)
                .add("fileGroupBits", fileGroupBits).add("fileBits", fileBits)
                .add("compressBlobs", compressBlobs).add("compressionThreshold", compressionThreshold)
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
        out.writeBoolean(compressBlobs);
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
        compressBlobs = in.readBoolean();
    }

    @Override
    public void redo() throws Exception {
        VolumeManager mgr = VolumeManager.getInstance();
        try {
            Volume vol = mgr.getVolume(id);
            if (vol != null) {
                mLog.info("Volume already exists id=%d", id);
                return;
            }
        } catch (VolumeServiceException e) {
            if (e.getCode() != VolumeServiceException.NO_SUCH_VOLUME) {
                throw e;
            }
        }
        try {
            Volume volume = Volume.builder().setId(id).setType(type).setName(name).setPath(rootPath, false)
                    .setMboxGroupBits(mboxGroupBits).setMboxBit(mboxBits)
                    .setFileGroupBits(fileGroupBits).setFileBits(fileBits)
                    .setCompressBlobs(compressBlobs).setCompressionThreshold(compressionThreshold).build();
            mgr.create(volume, getUnloggedReplay());
        } catch (VolumeServiceException e) {
            if (e.getCode() == VolumeServiceException.ALREADY_EXISTS) {
                mLog.info("Volume already exists id=%d", id);
            } else {
                throw e;
            }
        }
    }

}
