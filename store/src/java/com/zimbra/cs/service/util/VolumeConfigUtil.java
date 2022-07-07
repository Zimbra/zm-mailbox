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

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeServiceException;
import com.zimbra.soap.admin.message.CreateVolumeRequest;
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
        if (1 != storeType.intValue() && 2 != storeType.intValue()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Store Type shall be either 1 for internal volume or 2 for external volume", VolumeServiceException.BAD_VOLUME_STORE_TYPE);
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
        if (false != volInfoRequest.getCompressBlobs() &&
            true != volInfoRequest.getCompressBlobs() &&
            null != volInfoRequest.getCompressBlobs()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Compress Blobs can be TRUE, FALSE or OPTIONAL(don't provide)", VolumeServiceException.BAD_VOLUME_COMPRESS_BLOBS);
        }

        // validate compression threshold
        if (0 > volInfoRequest.getCompressionThreshold()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Compression Threshold can't be negative number", VolumeServiceException.BAD_VOLUME_COMPRESSION_THRESHOLD);
        }

        // validate current
        if (false != volInfoRequest.getIsCurrent() &&
            true != volInfoRequest.getIsCurrent()) {
            throw VolumeServiceException.INVALID_REQUEST("Volume Current can be TRUE, FALSE or OPTIONAL(don't provide)", VolumeServiceException.BAD_VOLUME_CURRENT);
        }

        if (enumStoreType.equals(Volume.StoreType.EXTERNAL)) {
            // validate use in frequent access
            if (false != volInfoRequest.getVolumeExternalInfo().getUseInFrequentAccess() &&
                true != volInfoRequest.getVolumeExternalInfo().getUseInFrequentAccess()) {
                throw VolumeServiceException.INVALID_REQUEST("Volume UseInFrequentAccess can be TRUE, FALSE or OPTIONAL(don't provide)", VolumeServiceException.BAD_VOLUME_USE_IN_FREQUENT_ACCESS);
            }

            // validate use intelligent tiering
            if (false != volInfoRequest.getVolumeExternalInfo().getUseIntelligentTiering() &&
                true != volInfoRequest.getVolumeExternalInfo().getUseIntelligentTiering()) {
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
}
