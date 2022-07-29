/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.google.common.base.MoreObjects;
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

    // new params
    private short storeType;

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

        storeType = (short)(volume.getStoreType().getStoreType());
    }

    @Override
    protected String getPrintableData() {
        return MoreObjects.toStringHelper(this).add("id", id).add("type", type).add("name", name).add("path", rootPath)
                .add("mboxGroupBits", mboxGroupBits).add("mboxBits", mboxBits)
                .add("fileGroupBits", fileGroupBits).add("fileBits", fileBits)
                .add("compressBlobs", compressBlobs).add("compressionThrehold", compressionThreshold)
                .add("storeType", storeType)
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
        out.writeShort(storeType);
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
        storeType = in.readShort();
    }

    @Override
    public void redo() throws Exception {
        VolumeManager mgr = VolumeManager.getInstance();
        mgr.getVolume(id);  // make sure it exists
        Volume vol = Volume.builder().setId(id).setType(type).setName(name).setPath(rootPath, false)
                .setMboxGroupBits(mboxGroupBits).setMboxBit(mboxBits)
                .setFileGroupBits(fileGroupBits).setFileBits(fileBits)
                .setCompressBlobs(compressBlobs).setCompressionThreshold(compressionThreshold)
                .setStoreType(Volume.StoreType.getStoreTypeBy(storeType)).build();
        // volume.toJAXB().setVolumeExternalInfo(buildVolumeExternalInfo());
        // ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
        // extVolInfoHandler.addServerProperties(buildVolumeInfo(volume));
        mgr.update(vol, getUnloggedReplay());
    }
}
