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

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetCurrentVolumesRequest;
import com.zimbra.soap.admin.message.GetCurrentVolumesResponse;

public final class GetCurrentVolumes extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((GetCurrentVolumesRequest) zsc.elementToJaxb(req), ctx));
    }

    private GetCurrentVolumesResponse handle(@SuppressWarnings("unused") GetCurrentVolumesRequest req,
            Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        checkRight(zsc, ctx, Provisioning.getInstance().getLocalServer(), Admin.R_manageVolume);

        GetCurrentVolumesResponse resp = new GetCurrentVolumesResponse();
        VolumeManager mgr = VolumeManager.getInstance();
        Volume msgVol = mgr.getCurrentMessageVolume();
        if (msgVol != null) {
            resp.addVolume(new GetCurrentVolumesResponse.CurrentVolumeInfo(msgVol.getId(), msgVol.getType()));
        }
        Volume secondaryMsgVol = mgr.getCurrentSecondaryMessageVolume();
        if (secondaryMsgVol != null) {
            resp.addVolume(new GetCurrentVolumesResponse.CurrentVolumeInfo(
                    secondaryMsgVol.getId(), secondaryMsgVol.getType()));
        }
        Volume indexVol = mgr.getCurrentIndexVolume();
        if (indexVol != null) {
            resp.addVolume(new GetCurrentVolumesResponse.CurrentVolumeInfo(indexVol.getId(), indexVol.getType()));
        }
        return resp;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }

}
