/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ModifyVolumeRequest;
import com.zimbra.soap.admin.message.ModifyVolumeResponse;
import com.zimbra.soap.admin.type.VolumeInfo;

public final class ModifyVolume extends AdminDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((ModifyVolumeRequest) JaxbUtil.elementToJaxb(req), ctx));
    }

    private ModifyVolumeResponse handle(ModifyVolumeRequest req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        checkRight(zsc, ctx, Provisioning.getInstance().getLocalServer(), Admin.R_manageVolume);

        VolumeManager mgr = VolumeManager.getInstance();
        Volume.Builder builder = Volume.builder(mgr.getVolume(req.getId()));
        VolumeInfo vol = req.getVolume();
        if (vol.getType() > 0) {
            builder.setType(vol.getType());
        }
        if (vol.getName() != null) {
            builder.setName(vol.getName());
        }
        if (vol.getRootPath() != null) {
            builder.setPath(vol.getRootPath(), true);
        }
        if (vol.getCompressBlobs() != null) {
            builder.setCompressBlobs(vol.getCompressBlobs());
        }
        if (vol.getCompressionThreshold() > 0) {
            builder.setCompressionThreshold(vol.getCompressionThreshold());
        }
        mgr.update(builder.build());
        return new ModifyVolumeResponse();

    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageVolume);
    }

}
