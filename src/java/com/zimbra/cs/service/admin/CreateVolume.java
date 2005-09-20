/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.Volume;
import com.zimbra.soap.ZimbraContext;

public class CreateVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);

        Element eVol = request.getElement(AdminService.E_VOLUME);
        String name  = eVol.getAttribute(AdminService.A_VOLUME_NAME);
        String path  = eVol.getAttribute(AdminService.A_VOLUME_ROOTPATH);
        short type   = (short) eVol.getAttributeLong(AdminService.A_VOLUME_TYPE);
        short mgbits = (short) eVol.getAttributeLong(AdminService.A_VOLUME_MGBITS);
        short mbits  = (short) eVol.getAttributeLong(AdminService.A_VOLUME_MBITS);
        short fgbits = (short) eVol.getAttributeLong(AdminService.A_VOLUME_FGBITS);
        short fbits  = (short) eVol.getAttributeLong(AdminService.A_VOLUME_FBITS);
        boolean compressBlobs = eVol.getAttributeBool(AdminService.A_VOLUME_COMPRESS_BLOBS);
        long compressionThreshold = eVol.getAttributeLong(AdminService.A_VOLUME_COMPRESSION_THRESHOLD);
        Volume vol = Volume.create(Volume.ID_AUTO_INCREMENT, type, name, path,
                                   mgbits, mbits, fgbits, fbits, compressBlobs, compressionThreshold);

        Element response = lc.createElement(AdminService.CREATE_VOLUME_RESPONSE);
        GetVolume.addVolumeElement(response, vol);
        return response;
    }
}
