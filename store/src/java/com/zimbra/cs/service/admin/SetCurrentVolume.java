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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.store.helper.StoreManagerResetHelper;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.SetCurrentVolumeRequest;
import com.zimbra.soap.admin.message.SetCurrentVolumeResponse;
import com.zimbra.soap.admin.type.StoreManagerRuntimeSwitchResult;

import java.util.List;
import java.util.Map;

public final class SetCurrentVolume extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((SetCurrentVolumeRequest) zsc.elementToJaxb(req), ctx));
    }

    private SetCurrentVolumeResponse handle(SetCurrentVolumeRequest req, Map<String, Object> ctx)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        checkRight(zsc, ctx, Provisioning.getInstance().getLocalServer(), Admin.R_manageVolume);

        short volId = req.getId() > 0 ? req.getId() : Volume.ID_NONE;
        VolumeManager.getInstance().setCurrentVolume(req.getType(), volId);
        SetCurrentVolumeResponse response = new SetCurrentVolumeResponse();
        Volume volume = VolumeManager.getInstance().getVolume(volId);
        // if its primary volume then
        if (Volume.TYPE_MESSAGE == volume.getType()) {
            // set current store manager
            StoreManagerRuntimeSwitchResult runtimeSwitchResult = StoreManagerResetHelper.setNewStoreManager(volume.getStoreManagerClass());
            response.setRuntimeSwitchResult(runtimeSwitchResult);
        }
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }
}
