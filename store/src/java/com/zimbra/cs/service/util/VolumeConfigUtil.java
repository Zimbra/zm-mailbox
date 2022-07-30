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

import com.zimbra.client.ZMailbox;
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
import com.zimbra.soap.admin.type.VolumeExternalOpenIOInfo;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.util.ExternalVolumeInfoHandler;

public class VolumeConfigUtil {

    private static final String  ROOT_PATH_ELE_SEPARATOR = "-";

    /**
     * This is default store manager
     */
    private static final String DEFAULT_STORE_MANAGER = "com.zimbra.cs.store.file.FileBlobStore";
    /**
     * This is default external store manager, not part of FOSS edition
     */
    private static final String DEFAULT_EXTERNAL_STORE_MANAGER = "com.zimbra.storemanagers.store.GenericStoreManager";

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
        if (volInfoRequest != null && volInfoRequest.getVolumeExternalInfo() != null
                && Volume.StoreType.EXTERNAL.getStoreType() == storeType.intValue()) {
            String extRootPath = "/" + volInfoRequest.getVolumeExternalInfo().getStorageType() + "-" + volInfoRequest.getName() + "-" + volInfoRequest.getVolumeExternalInfo().getGlobalBucketConfigurationId();
            volInfoRequest.setRootPath(extRootPath);
        }

        if (volInfoRequest != null && volInfoRequest.getVolumeExternalOpenIOInfo() != null
                && Volume.StoreType.EXTERNAL.getStoreType() == storeType.intValue()) {
            String extRootPath = "/" + volInfoRequest.getVolumeExternalOpenIOInfo().getStorageType() + "-" + volInfoRequest.getName();
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
                setDefaultStoreManager(volInfoRequest, storeType);
            }
        }

        if (Volume.StoreType.EXTERNAL.equals(enumStoreType)) {
            // validate storage type
            String storageTypeS3 = null;
            if (volInfoRequest != null && volInfoRequest.getVolumeExternalInfo() != null) {
                storageTypeS3 = volInfoRequest.getVolumeExternalInfo().getStorageType();
            }
            String storageTypeOpenIO = null;
            if (volInfoRequest != null && volInfoRequest.getVolumeExternalOpenIOInfo() != null) {
                storageTypeOpenIO = volInfoRequest.getVolumeExternalOpenIOInfo().getStorageType();

            }
            if (storageTypeS3 != null && storageTypeOpenIO != null
                    && !storageTypeS3.equalsIgnoreCase(AdminConstants.A_VOLUME_S3)
                    && !storageTypeOpenIO.equalsIgnoreCase(AdminConstants.A_VOLUME_OPEN_IO)) {
                throw VolumeServiceException.INVALID_REQUEST("Volume Storage Type can be only S3 or OpenIO",
                        VolumeServiceException.BAD_VOLUME_STORAGE_TYPE);
            }

            if (AdminConstants.A_VOLUME_S3.equalsIgnoreCase(storageTypeS3)) {
                // validate use in frequent access threshold
                int useInFrequentAccessThreshold = volInfoRequest.getVolumeExternalInfo().getUseInFrequentAccessThreshold();
                if (useInFrequentAccessThreshold < 0) {
                    throw VolumeServiceException.INVALID_REQUEST("Volume UseInFrequentAccessThreshold can't be negative number", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD);
                }

                // validate global bucket id
                String globalS3BucketId = volInfoRequest.getVolumeExternalInfo().getGlobalBucketConfigurationId();
                if (!extVolInfoHandler.validateGlobalBucketID(globalS3BucketId)) {
                    throw VolumeServiceException.INVALID_REQUEST("Volume GlobalBucketID provided is incorrect, missing or empty", VolumeServiceException.BAD_VOLUME_GLOBAL_BUCKET_ID);
                }
                // no validation for volume prefix as of now
            } else if (AdminConstants.A_VOLUME_OPEN_IO.equalsIgnoreCase(storageTypeOpenIO)) {
                // validate proxy port as positive
                if (volInfoRequest.getVolumeExternalOpenIOInfo().getProxyPort() <= 0) {
                    throw VolumeServiceException.INVALID_REQUEST("Proxy port can't be negative number or null", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD);
                }

                // validate account port as positive
                if (volInfoRequest.getVolumeExternalOpenIOInfo().getAccountPort() <= 0) {
                    throw VolumeServiceException.INVALID_REQUEST("Account port can't be negative number or null", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD);
                }

                // validate url is empty or not
                if (StringUtil.isNullOrEmpty(volInfoRequest.getVolumeExternalOpenIOInfo().getUrl())) {
                    throw VolumeServiceException.INVALID_REQUEST("Url can't be null", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD);
                }

                // validate account is empty or not
                if (StringUtil.isNullOrEmpty(volInfoRequest.getVolumeExternalOpenIOInfo().getAccount())) {
                    throw VolumeServiceException.INVALID_REQUEST("Account can't be null", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD);
                }

                // validate namespace is empty or not
                if (StringUtil.isNullOrEmpty(volInfoRequest.getVolumeExternalOpenIOInfo().getNameSpace())) {
                    throw VolumeServiceException.INVALID_REQUEST("Name Space can't be null", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD);
                }
            } else {
                throw VolumeServiceException.INVALID_REQUEST("Volume Storage Type can be only S3 or OpenIO",
                        VolumeServiceException.BAD_VOLUME_STORAGE_TYPE);
            }
        }
    }

    /**
     * Set default StoreManager for volume
     * @param volInfoRequest
     * @param storeType
     */
    private static void setDefaultStoreManager(VolumeInfo volInfoRequest, Short storeType) {
        if (Volume.StoreType.EXTERNAL.getStoreType() == storeType.intValue()) {
            volInfoRequest.setStoreManagerClass(DEFAULT_EXTERNAL_STORE_MANAGER);
        } else {
            volInfoRequest.setStoreManagerClass(DEFAULT_STORE_MANAGER);
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
        if (Volume.StoreType.EXTERNAL.equals(enumStoreType)) {
            try {
                volInfoRequest.setId(volRequest.getId());
                if (volInfoRequest.getVolumeExternalInfo() != null && AdminConstants.A_VOLUME_S3
                        .equalsIgnoreCase(volInfoRequest.getVolumeExternalInfo().getStorageType())) {
                    volInfoResponse.setVolumeExternalInfo(volInfoRequest.getVolumeExternalInfo());
                } else {
                    volInfoResponse.setVolumeExternalOpenIOInfo(volInfoRequest.getVolumeExternalOpenIOInfo());
                }
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

            if (Volume.StoreType.EXTERNAL.equals(vol.getStoreType())) {
                ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());

                try {
                    JSONObject properties = extVolInfoHandler.readServerProperties(volInfo.getId());
                    String storageType = properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE);
                    if (AdminConstants.A_VOLUME_S3.equalsIgnoreCase(storageType)) {
                        volInfo.setVolumeExternalInfo(new VolumeExternalInfo().toExternalInfo(properties));
                    } else {
                        volInfo.setVolumeExternalOpenIOInfo(new VolumeExternalOpenIOInfo().toExternalOpenIoInfo(properties));
                    }
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
        if (Volume.StoreType.EXTERNAL.equals(vol.getStoreType())) {
            ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
            try {
                JSONObject properties = extVolInfoHandler.readServerProperties(volInfo.getId());
                String storageType = properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE);
                if (storageType.equalsIgnoreCase(AdminConstants.A_VOLUME_S3)) {
                    volInfo.setVolumeExternalInfo(new VolumeExternalInfo().toExternalInfo(properties));
                } else {
                    volInfo.setVolumeExternalOpenIOInfo(new VolumeExternalOpenIOInfo().toExternalOpenIoInfo(properties));
                }
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
     * @throws JSONException
     */
    public static void parseModifyVolumeRequest(ModifyVolumeRequest req) throws ServiceException, JSONException {
        VolumeManager mgr = VolumeManager.getInstance();
        VolumeInfo volInfo = req.getVolumeInfo();
        Volume vol = mgr.getVolume(req.getId());
        Volume.Builder builder = Volume.builder(vol);

        if (volInfo == null) {
            throw VolumeServiceException.INVALID_REQUEST("Must specify a volume Element", VolumeServiceException.NO_SUCH_VOLUME);
        }

        StoreManager storeManager = StoreManager.getInstance();
        if (storeManager.supports(StoreManager.StoreFeature.CUSTOM_STORE_API, String.valueOf(volInfo.getId()))) {
            throw VolumeServiceException.INVALID_REQUEST("Operation unsupported, use zxsuite to edit this volume", VolumeServiceException.INVALID_REQUEST);
        }

        // store type == 1, allow modification of all parameters
        if (Volume.StoreType.INTERNAL.equals(vol.getStoreType())) {
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
        else if (Volume.StoreType.EXTERNAL.equals(vol.getStoreType())) {
            if (volInfo.getName() != null) {
                String storageType = "";
                String globalBucketConfigId = "";
                ExternalVolumeInfoHandler extVolInfoHandler = new ExternalVolumeInfoHandler(Provisioning.getInstance());
                JSONObject properties = extVolInfoHandler.readServerProperties(req.getId());
                if (JSONObject.NULL.equals(properties)) {
                    throw VolumeServiceException.INVALID_REQUEST("Unable to read server properties", VolumeServiceException.INVALID_REQUEST);
                }
                storageType = properties.getString(AdminConstants.A_VOLUME_STORAGE_TYPE);
                if (storageType.equalsIgnoreCase(AdminConstants.A_VOLUME_S3)) {
                    try {
                        globalBucketConfigId = properties.getString(AdminConstants.A_VOLUME_GLB_BUCKET_CONFIG_ID);
                    } catch (JSONException e) {
                        throw ServiceException.FAILURE("Error while reading json data for external volume: " + req.getId(), e);
                    }
                    // storageType should not be null
                    if (StringUtil.isNullOrEmpty(storageType)) {
                        throw VolumeServiceException.INVALID_REQUEST("StorageType Empty for external volume " + req.getId(), VolumeServiceException.INVALID_REQUEST);
                    }
                    builder.setName(volInfo.getName());
                    String extRootPath = ZMailbox.PATH_SEPARATOR + storageType + ROOT_PATH_ELE_SEPARATOR + volInfo.getName();
                    // append global bucket id as well in case it is available
                    if (!StringUtil.isNullOrEmpty(globalBucketConfigId)) {
                        extRootPath = extRootPath + ROOT_PATH_ELE_SEPARATOR + globalBucketConfigId;
                    }
                    builder.setPath(extRootPath, false);
                } else {
                    builder.setName(volInfo.getName());
                }
            }
        }
        mgr.update(builder.build());
    }
}
