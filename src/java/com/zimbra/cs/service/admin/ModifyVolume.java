/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.Volume;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraContext(context);

        long idLong = request.getAttributeLong(AdminService.A_ID);
        Volume.validateID(idLong);  // avoid Java truncation
        short id = (short) idLong;
        Volume vol = Volume.getById(id);

        Element eVol = request.getElement(AdminService.E_VOLUME);
        String name  = eVol.getAttribute(AdminService.A_VOLUME_NAME, vol.getName());
        String path  = eVol.getAttribute(AdminService.A_VOLUME_ROOTPATH, vol.getRootPath());
        short type   = (short) eVol.getAttributeLong(AdminService.A_VOLUME_TYPE, vol.getType());

        // TODO: These "bits" parameters are ignored inside Volume.update() for now.
//        short mgbits = (short) eVol.getAttributeLong(AdminService.A_VOLUME_MGBITS, vol.getMboxGroupBits());
//        short mbits  = (short) eVol.getAttributeLong(AdminService.A_VOLUME_MBITS,  vol.getMboxBits());
//        short fgbits = (short) eVol.getAttributeLong(AdminService.A_VOLUME_FGBITS, vol.getFileGroupBits());
//        short fbits  = (short) eVol.getAttributeLong(AdminService.A_VOLUME_FBITS,  vol.getFileBits());

        boolean compressBlobs = eVol.getAttributeBool(AdminService.A_VOLUME_COMPRESS_BLOBS, vol.getCompressBlobs());
        long compressionThreshold = eVol.getAttributeLong(AdminService.A_VOLUME_COMPRESSION_THRESHOLD,
                                                          vol.getCompressionThreshold());
//        Volume.update(vol.getId(), type, name, path, mgbits, mbits, fgbits, fbits,
//                      compressBlobs, compressionThreshold);
        Volume.update(vol.getId(), type, name, path, (short) 0, (short) 0, (short) 0, (short) 0,
                      compressBlobs, compressionThreshold);

        Element response = lc.createElement(AdminService.MODIFY_VOLUME_RESPONSE);
        return response;
    }
}
