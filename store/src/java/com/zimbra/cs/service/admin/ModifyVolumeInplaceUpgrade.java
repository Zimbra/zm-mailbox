/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.service.util.VolumeConfigUtil;
import com.zimbra.cs.volume.VolumeServiceException;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ModifyVolumeInplaceUpgradeRequest;
import com.zimbra.soap.admin.message.ModifyVolumeInplaceUpgradeResponse;

public final class ModifyVolumeInplaceUpgrade extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((ModifyVolumeInplaceUpgradeRequest) zsc.elementToJaxb(req), ctx));
    }

    private ModifyVolumeInplaceUpgradeResponse handle(ModifyVolumeInplaceUpgradeRequest req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        Server server = Provisioning.getInstance().getLocalServer();

        checkRight(zsc, ctx, server, Admin.R_manageVolume);
        
        try {
            VolumeConfigUtil.parseModifyVolumeInplaceUpgradeRequest(req, server.getId());
        } catch (JSONException e) {
            throw VolumeServiceException.INVALID_REQUEST("Format is not correct Exception", VolumeServiceException.INVALID_REQUEST);
        }
        return new ModifyVolumeInplaceUpgradeResponse();
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }
}
