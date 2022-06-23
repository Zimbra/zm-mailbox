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

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.DeleteVolumeRequest;
import com.zimbra.soap.admin.message.DeleteVolumeResponse;
import com.zimbra.util.ExternalVolumeReader;

public final class DeleteVolume extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((DeleteVolumeRequest) zsc.elementToJaxb(req), ctx));
    }

    private DeleteVolumeResponse handle(DeleteVolumeRequest req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        checkRight(zsc, ctx, Provisioning.getInstance().getLocalServer(), Admin.R_manageVolume);

        VolumeManager mgr = VolumeManager.getInstance();
        Volume vol = mgr.getVolume(req.getId()); // make sure the volume exists before doing anything heavyweight...
        StoreManager storeManager = StoreManager.getInstance();
        if (storeManager.supports(StoreManager.StoreFeature.CUSTOM_STORE_API, String.valueOf(req.getId()))) {
          throw ServiceException.INVALID_REQUEST("Operation unsupported, use zxsuite to delete this volume", null);
        }
        mgr.delete(req.getId());
        try {
            if (vol.getStoreType().equals(Volume.StoreType.EXTERNAL)) {
                ExternalVolumeReader extVolReader = new ExternalVolumeReader(Provisioning.getInstance());
                extVolReader.deleteServerProperties(req.getId());
            }
        }
        catch (JSONException e) {
            // LOG.error("Error while processing ldap attribute ServerExternalStoreConfig", e);
            // throw e;
        }
        return new DeleteVolumeResponse();
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }

}
