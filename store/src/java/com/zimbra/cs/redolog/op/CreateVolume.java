/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2022 Synacor, Inc.
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.cs.volume.VolumeServiceException;
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.util.ExternalVolumeInfoHandler;

import org.json.JSONException;
import org.json.JSONObject;

public final class CreateVolume extends RedoableOp {

    private short id = Volume.ID_NONE;
    private short type;
    private String name;
    private String rootPath;

    private short mboxGroupBits;
    private short mboxBits;
    private short fileGroupBits;
    private short fileBits;
    private short storeType;

    private boolean compressBlobs;
    private long compressionThreshold;

    // external params
    private String storageType;
    private String volumePrefix;
    private String globalBucketConfigId;
    private boolean useInFrequentAccess;
    private int useInFrequentAccessThreshold;
    private boolean useIntelligentTiering;

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
        storeType = (short)(volume.getStoreType().getStoreType());

        // set external params
        try {
            setExternalStorageParams(volume);
        } catch (JSONException e) {
            ZimbraLog.misc.error("Failure while redoing CreateVolume Operation : ", e);
        } catch (ServiceException e) {
            ZimbraLog.misc.error("Failure while redoing CreateVolume Operation : ", e);
        }
    }

    public void setId(short id) {
        this.id = id;
    }

    @Override
    protected String getPrintableData() {
        return MoreObjects.toStringHelper(this)
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
            Volume.StoreType enumStoreType =
                (1 == storeType) ? Volume.StoreType.INTERNAL : Volume.StoreType.EXTERNAL;

            Volume volume = Volume.builder().setId(id).setType(type).setName(name).setPath(rootPath, false)
                    .setMboxGroupBits(mboxGroupBits).setMboxBit(mboxBits)
                    .setFileGroupBits(fileGroupBits).setFileBits(fileBits)
                    .setCompressBlobs(compressBlobs).setCompressionThreshold(compressionThreshold)
                    .setStoreType(enumStoreType).build();

            VolumeInfo volInfo = buildVolumeInfo(volume);
            ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
            extVolInfoHandler.addServerProperties(volInfo);
            mgr.create(volume, getUnloggedReplay());
        } catch (VolumeServiceException e) {
            if (e.getCode() == VolumeServiceException.ALREADY_EXISTS) {
                mLog.info("Volume already exists id=%d", id);
            } else {
                throw e;
            }
        }
    }

    private void setExternalStorageParams(Volume volume) throws JSONException, ServiceException {
        // read backup file parameters
        ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
        JSONObject properties = extVolInfoHandler.readBackupServerProperties(volume.getId(), ExternalVolumeInfoHandler.PREV_FILE_PATH);
        volumePrefix = properties.getString(AdminConstants.A_VOLUME_VOLUME_PREFIX);
        globalBucketConfigId = properties.getString(AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID);
        storageType = properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE);
        useInFrequentAccess = Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS));
        useIntelligentTiering = Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING));
        useInFrequentAccessThreshold = Integer.parseInt(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD));
    }

    private VolumeExternalInfo buildVolumeExternalInfo() {
        VolumeExternalInfo volExtInfo = new VolumeExternalInfo();
        volExtInfo.setVolumePrefix(volumePrefix);
        volExtInfo.setGlobalBucketConfigurationId(globalBucketConfigId);
        volExtInfo.setStorageType(storageType);
        volExtInfo.setUseInFrequentAccess(useInFrequentAccess);
        volExtInfo.setUseIntelligentTiering(useIntelligentTiering);
        volExtInfo.setUseInFrequentAccessThreshold(useInFrequentAccessThreshold);
        return volExtInfo;
    }

    private VolumeInfo buildVolumeInfo(Volume volume) {
        VolumeInfo volInfo = volume.toJAXB();
        volInfo.setVolumeExternalInfo(buildVolumeExternalInfo());
        return volInfo;
    }
}
