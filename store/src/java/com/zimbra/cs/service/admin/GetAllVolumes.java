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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.soap.admin.type.VolumeExternalInfo;
import com.zimbra.util.ExternalVolumeInfoHandler;

public final class GetAllVolumes extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((GetAllVolumesRequest) zsc.elementToJaxb(req), ctx));
    }

    private GetAllVolumesResponse handle(@SuppressWarnings("unused") GetAllVolumesRequest req,
            Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        checkRight(zsc, ctx, Provisioning.getInstance().getLocalServer(), Admin.R_manageVolume);

        GetAllVolumesResponse resp = new GetAllVolumesResponse();
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
                } catch (ServiceException e) {
                    ZimbraLog.store.error("Error while processing GetAllVolumesRequest", e);
                    throw e;
                } catch (JSONException e) {
                    throw ServiceException.FAILURE("Error while reading server properties", null);
                }
            }
            resp.addVolume(volInfo);
        }
        return resp;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }
}
