/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.store.Volume;
import com.zimbra.soap.ZimbraSoapContext;

public class CreateVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element eVol = request.getElement(AdminConstants.E_VOLUME);
        String name  = eVol.getAttribute(AdminConstants.A_VOLUME_NAME);
        String path  = eVol.getAttribute(AdminConstants.A_VOLUME_ROOTPATH);
        short type   = (short) eVol.getAttributeLong(AdminConstants.A_VOLUME_TYPE);

        // TODO: These "bits" parameters are ignored inside Volume.create() for now.
//        short mgbits = (short) eVol.getAttributeLong(AdminService.A_VOLUME_MGBITS);
//        short mbits  = (short) eVol.getAttributeLong(AdminService.A_VOLUME_MBITS);
//        short fgbits = (short) eVol.getAttributeLong(AdminService.A_VOLUME_FGBITS);
//        short fbits  = (short) eVol.getAttributeLong(AdminService.A_VOLUME_FBITS);

        boolean compressBlobs = eVol.getAttributeBool(AdminConstants.A_VOLUME_COMPRESS_BLOBS);
        long compressionThreshold = eVol.getAttributeLong(AdminConstants.A_VOLUME_COMPRESSION_THRESHOLD);
//        Volume vol = Volume.create(Volume.ID_AUTO_INCREMENT, type, name, path,
//                                   mgbits, mbits, fgbits, fbits, compressBlobs, compressionThreshold);
        Volume vol = Volume.create(Volume.ID_AUTO_INCREMENT, type, name, path,
                                   (short) 0, (short) 0, (short) 0, (short) 0,
                                   compressBlobs, compressionThreshold);

        Element response = lc.createElement(AdminConstants.CREATE_VOLUME_RESPONSE);
        GetVolume.addVolumeElement(response, vol);
        return response;
    }
}
