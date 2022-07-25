/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

package com.zimbra.cs.service.util;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.store.helper.ClassHelper;
import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.cs.volume.VolumeServiceException;
import com.zimbra.soap.admin.message.CreateVolumeRequest;
import com.zimbra.soap.admin.message.DeleteVolumeRequest;
import com.zimbra.soap.admin.message.ModifyVolumeRequest;
import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;
import com.zimbra.soap.admin.message.GetVolumeRequest;
import com.zimbra.soap.admin.message.GetVolumeResponse;
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.util.ExternalVolumeInfoHandler;

public class VolumeConfigUtil {

    /**
     * Validate the create volume request parameters
     *
     * @param request
     * @return
     * @throws ServiceException
     */
    public static void validateCreateVolumeRequest(CreateVolumeRequest request, VolumeInfo volInfoRequest,
                                                   Volume.StoreType enumStoreType) throws ServiceException {
        ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());

        // validate store type
        Short storeType = volInfoRequest.getStoreType();
        if (Volume.StoreType.INTERNAL.getStoreType() != storeType.intValue() && Volume.StoreType.EXTERNAL.getStoreType() != storeType.intValue()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Store Type shall be either 1 for internal volume or 2 for external volume", VolumeServiceException.BAD_VOLUME_STORE_TYPE);
        }

        // validate/set root path if store type is external
        if (Volume.StoreType.EXTERNAL.getStoreType() == storeType.intValue()) {
            String extRootPath = "/" + volInfoRequest.getVolumeExternalInfo().getStorageType() + "-" + volInfoRequest.getName() + "-" + volInfoRequest.getVolumeExternalInfo().getGlobalBucketConfigurationId();
            volInfoRequest.setRootPath(extRootPath);
        }

        // validate root path
        if (StringUtil.isNullOrEmpty(volInfoRequest.getRootPath())) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Root Path can't be missing, empty or null", VolumeServiceException.BAD_VOLUME_PATH);
        }

        // validate name
        if (StringUtil.isNullOrEmpty(volInfoRequest.getName())) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Name can't be missing, empty or null", VolumeServiceException.BAD_VOLUME_NAME);
        }

        // validate type
        Short type = volInfoRequest.getType();
        if (1 != type.intValue() && 2 != type.intValue() && 10 != type.intValue()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Type can be 1 for PRIMARY volume, 2 for SECONDARY volume or 10 for INDEX volume", VolumeServiceException.BAD_VOLUME_TYPE);
        }

        // validate compress blobs
        if (false != volInfoRequest.isCompressBlobs() &&
            true != volInfoRequest.isCompressBlobs()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Compress Blobs can be TRUE, FALSE or OPTIONAL(don't provide)", VolumeServiceException.BAD_VOLUME_COMPRESS_BLOBS);
        }

        // validate compression threshold
        if (0 > volInfoRequest.getCompressionThreshold()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Compression Threshold can't be negative number", VolumeServiceException.BAD_VOLUME_COMPRESSION_THRESHOLD);
        }


        if (Volume.TYPE_MESSAGE == volInfoRequest.getType() || Volume.TYPE_MESSAGE_SECONDARY == volInfoRequest.getType()) {
            if (!StringUtil.isNullOrEmpty(volInfoRequest.getStoreManagerClass())) {
                if (!ClassHelper.isClassExist(volInfoRequest.getStoreManagerClass())) {
                    throw VolumeServiceException.INVALID_REQUEST("Invalid StoreManager class, can not loaded", VolumeServiceException.BAD_VOLUME_STORE_MANAGER_CLASS);
                }
            } else {
                // set to default store manager if not passed.
                volInfoRequest.setStoreManagerClass(LC.zimbra_class_store.value());
            }
        }

        // validate current
        if (false != volInfoRequest.isCurrent() &&
            true != volInfoRequest.isCurrent()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Current can be TRUE, FALSE or OPTIONAL(don't provide)", VolumeServiceException.BAD_VOLUME_CURRENT);
        }

        if (enumStoreType.equals(Volume.StoreType.EXTERNAL)) {
            // validate use in frequent access
            if (false != volInfoRequest.getVolumeExternalInfo().isUseInFrequentAccess() &&
                true != volInfoRequest.getVolumeExternalInfo().isUseInFrequentAccess()) {
                throw VolumeServiceException.INVALID_REQUEST("Volume UseInFrequentAccess can be TRUE, FALSE or OPTIONAL(don't provide)", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS);
            }

            // validate use intelligent tiering
            if (false != volInfoRequest.getVolumeExternalInfo().isUseIntelligentTiering() &&
                true != volInfoRequest.getVolumeExternalInfo().isUseIntelligentTiering()) {
                throw VolumeServiceException.INVALID_REQUEST("Volume UseIntelligentTiering can be TRUE, FALSE or OPTIONAL(don't provide)", VolumeServiceException.BAD_VOLUME_USE_INTELLIGENT_TIERING);
            }

            // validate storage type
            String storageType = volInfoRequest.getVolumeExternalInfo().getStorageType();
            if (false == storageType.equals("S3")) {
                throw VolumeServiceException.INVALID_REQUEST("Volume Storage Type can be only S3", VolumeServiceException.BAD_VOLUME_STORAGE_TYPE);

            }

            // validate use in frequent access threshold
            int useInFrequentAccessThreshold = volInfoRequest.getVolumeExternalInfo().getUseInFrequentAccessThreshold();
            if (0 > useInFrequentAccessThreshold) {
                throw VolumeServiceException.INVALID_REQUEST("Volume UseInFrequentAccessThreshold can't be negative number", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD);
            }

            // validate global bucket id
            String globalS3BucketId = volInfoRequest.getVolumeExternalInfo().getGlobalBucketConfigurationId();
            if (false == extVolInfoHandler.validateGlobalBucketID(globalS3BucketId)) {
                throw VolumeServiceException.INVALID_REQUEST("Volume GlobalBucketID provided is incorrect, missing or empty", VolumeServiceException.BAD_VOLUME_GLOBAL_BUCKET_ID);
            }

            // no validation for volume prefix as of now
        }
    }

    /**
     * Perform required actions after creating volume
     *
     * @param request, volInfoResponse
     * @return
     * @throws ServiceException
     */
    public static void postCreateVolumeActions(CreateVolumeRequest request, Volume volRequest,
                                               VolumeInfo volInfoRequest, VolumeInfo volInfoResponse,
                                               Volume.StoreType enumStoreType) throws ServiceException {
        ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
        if (enumStoreType.equals(Volume.StoreType.EXTERNAL)) {
            try {
                volInfoRequest.setId(volRequest.getId());
                volInfoResponse.setVolumeExternalInfo(volInfoRequest.getVolumeExternalInfo());
                extVolInfoHandler.addServerProperties(volInfoRequest);
            } catch (JSONException e) {
                throw ServiceException.FAILURE("Error while processing postCreateVolumeActions", e);
            }
        }
    }

    /**
     * Perform required actions for processing GetAllVolumesRequest
     *
     * @param GetAllVolumesRequest, GetAllVolumesResponse
     * @return
     * @throws ServiceException
     */
    public static void parseGetAllVolumesRequest(GetAllVolumesRequest req, GetAllVolumesResponse resp) throws ServiceException {
        for (Volume vol : VolumeManager.getInstance().getAllVolumes()) {
            VolumeInfo volInfo = vol.toJAXB();

            if (vol.getStoreType().equals(Volume.StoreType.EXTERNAL)) {
                VolumeExternalInfo volExtInfo = new VolumeExternalInfo();
                ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());

                try {
                    JSONObject properties = extVolInfoHandler.readServerProperties(volInfo.getId());
                    String volumePrefix = properties.getString(AdminConstants.A_VOLUME_VOLUME_PREFIX);
                    String globalBucketConfigId = properties.getString(AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID);
                    String storageType = properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE);
                    Boolean useInFrequentAccess = Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS));
                    Boolean useIntelligentTiering = Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING));
                    int useInFrequentAccessThreshold = Integer.parseInt(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD));

                    volExtInfo.setVolumePrefix(volumePrefix);
                    volExtInfo.setGlobalBucketConfigurationId(globalBucketConfigId);
                    volExtInfo.setStorageType(storageType);
                    volExtInfo.setUseInFrequentAccess(useInFrequentAccess);
                    volExtInfo.setUseIntelligentTiering(useIntelligentTiering);
                    volExtInfo.setUseInFrequentAccessThreshold(useInFrequentAccessThreshold);
                    volInfo.setVolumeExternalInfo(volExtInfo);
                } catch (JSONException e) {
                    throw ServiceException.FAILURE("Error while processing GetAllVolumesRequest", e);
                }
            }
            resp.addVolume(volInfo);
        }
    }

    /**
     * Perform required actions for processing parseGetVolumeRequest
     *
     * @param GetVolumeResponse, GetVolumeResponse, Volume, VolumeInfo
     * @return
     * @throws ServiceException
     */
    public static void parseGetVolumeRequest(GetVolumeRequest req, GetVolumeResponse resp,
                                             Volume vol, VolumeInfo volInfo) throws ServiceException {
        if (vol.getStoreType().equals(Volume.StoreType.EXTERNAL)) {
            VolumeExternalInfo volExtInfo = new VolumeExternalInfo();
            ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
            try {
                JSONObject properties = extVolInfoHandler.readServerProperties(volInfo.getId());
                String volumePrefix = properties.getString(AdminConstants.A_VOLUME_VOLUME_PREFIX);
                String globalBucketConfigId = properties.getString(AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID);
                String storageType = properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE);
                Boolean useInFrequentAccess = Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS));
                Boolean useIntelligentTiering = Boolean.valueOf(properties.getString(AdminConstants.A_VOLUME_USE_INTELLIGENT_TIERING));
                int useInFrequentAccessThreshold = Integer.parseInt(properties.getString(AdminConstants.A_VOLUME_USE_IN_FREQ_ACCESS_THRESHOLD));

                volExtInfo.setVolumePrefix(volumePrefix);
                volExtInfo.setGlobalBucketConfigurationId(globalBucketConfigId);
                volExtInfo.setStorageType(storageType);
                volExtInfo.setUseInFrequentAccess(useInFrequentAccess);
                volExtInfo.setUseIntelligentTiering(useIntelligentTiering);
                volExtInfo.setUseInFrequentAccessThreshold(useInFrequentAccessThreshold);
                volInfo.setVolumeExternalInfo(volExtInfo);
            } catch (JSONException e) {
                throw ServiceException.FAILURE("Error while processing GetVolumesRequest", e);
            }
        }
    }

    /**
     * Perform required actions for processing parseDeleteVolumeRequest
     *
     * @param DeleteVolumeRequest
     * @return
     * @throws ServiceException
     */
    public static void parseDeleteVolumeRequest(DeleteVolumeRequest req) throws ServiceException {
        VolumeManager mgr = VolumeManager.getInstance();
        Volume vol = mgr.getVolume(req.getId()); // make sure the volume exists before doing anything heavyweight...
        StoreManager storeManager = StoreManager.getInstance();
        if (storeManager.supports(StoreManager.StoreFeature.CUSTOM_STORE_API, String.valueOf(req.getId()))) {
            throw VolumeServiceException.INVALID_REQUEST("Operation unsupported, use zxsuite to edit this volume", VolumeServiceException.INVALID_REQUEST);
        }
        mgr.delete(req.getId());
        try {
            if (vol.getStoreType().equals(Volume.StoreType.EXTERNAL)) {
                ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
                if (extVolInfoHandler.isVolumePresentInJson(req.getId())) {
                    extVolInfoHandler.deleteServerProperties(req.getId());
                } else {
                    String errMsg = "External volume entry in JSON not found, Volume ID " + req.getId();
                    throw ServiceException.FAILURE(errMsg, null);
                }
            }
        } catch (JSONException e) {
            throw ServiceException.FAILURE("Error while processing DeleteVolumeRequest", e);
        }
    }

    /**
     * Perform required actions for processing parseModifyVolumeRequest
     *
     * @param ModifyVolumeRequest
     * @return
     * @throws ServiceException
     */
    public static void parseModifyVolumeRequest(ModifyVolumeRequest req) throws ServiceException {
        VolumeManager mgr = VolumeManager.getInstance();
        VolumeInfo volInfo = req.getVolumeInfo();
        Volume vol = mgr.getVolume(volInfo.getId());
        Volume.Builder builder = Volume.builder(vol);

        if (volInfo == null) {
            throw VolumeServiceException.INVALID_REQUEST("Must specify a volume Element", VolumeServiceException.NO_SUCH_VOLUME);
        }

        StoreManager storeManager = StoreManager.getInstance();
        if (storeManager.supports(StoreManager.StoreFeature.CUSTOM_STORE_API, String.valueOf(volInfo.getId()))) {
            throw VolumeServiceException.INVALID_REQUEST("Operation unsupported, use zxsuite to edit this volume", VolumeServiceException.INVALID_REQUEST);
        }

        // store type == 1, allow modification of all parameters
        if (vol.getStoreType().equals(Volume.StoreType.INTERNAL)) {
            if (volInfo.getType() > 0) {
                builder.setType(volInfo.getType());
            }
            if (!StringUtil.isNullOrEmpty(volInfo.getName())) {
                builder.setName(volInfo.getName());
            }
            if (!StringUtil.isNullOrEmpty(volInfo.getRootPath())) {
                builder.setPath(volInfo.getRootPath(), true);
            }
            if (volInfo.getCompressionThreshold() > 0) {
                builder.setCompressionThreshold(volInfo.getCompressionThreshold());
            }
            builder.setCompressBlobs(volInfo.isCompressBlobs());
        }
        // store type == 2, allow modification of only volume name
        else if (vol.getStoreType().equals(Volume.StoreType.EXTERNAL)) {
            if (volInfo.getName() != null) {
                builder.setName(volInfo.getName());
            }
        }
        mgr.update(builder.build());
    }
}
