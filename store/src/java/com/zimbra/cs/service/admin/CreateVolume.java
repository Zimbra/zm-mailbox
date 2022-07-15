/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016, 2022 Synacor, Inc.
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

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.service.util.VolumeConfigUtil;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.soap.admin.message.CreateVolumeRequest;
import com.zimbra.soap.admin.message.CreateVolumeResponse;

public final class CreateVolume extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((CreateVolumeRequest) zsc.elementToJaxb(req), ctx));
    }

    private CreateVolumeResponse handle(CreateVolumeRequest request, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        checkRight(zsc, ctx, Provisioning.getInstance().getLocalServer(), Admin.R_manageVolume);

        VolumeInfo volInfoRequest = request.getVolumeInfo();
        Volume.StoreType enumStoreType = (1 == volInfoRequest.getStoreType()) ? Volume.StoreType.INTERNAL : Volume.StoreType.EXTERNAL;
        VolumeConfigUtil.validateCreateVolumeRequest(request, volInfoRequest, enumStoreType);

        Volume volRequest = VolumeManager.getInstance().create(toVolume(volInfoRequest, enumStoreType));
        VolumeInfo volInfoResponse = volRequest.toJAXB();
        VolumeConfigUtil.postCreateVolumeActions(request, volRequest, volInfoRequest, volInfoResponse, enumStoreType);
        return new CreateVolumeResponse(volInfoResponse);
    }

    private Volume toVolume(VolumeInfo vol, Volume.StoreType enumStoreType) throws ServiceException {
        return Volume.builder().setType(vol.getType()).setName(vol.getName()).setPath(vol.getRootPath(), enumStoreType.equals(Volume.StoreType.INTERNAL))
                .setCompressBlobs(vol.isCompressBlobs()).setCompressionThreshold(vol.getCompressionThreshold())
                .setStoreType(enumStoreType).build();
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }
}
