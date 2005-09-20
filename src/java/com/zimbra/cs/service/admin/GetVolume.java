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

public class GetVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        short id = (short) request.getAttributeLong(AdminService.A_ID);
        Volume vol = Volume.getById(id);

        Element response = lc.createElement(AdminService.GET_VOLUME_RESPONSE);
        addVolumeElement(response, vol);
        return response;
    }

    static void addVolumeElement(Element parent, Volume vol) {
        Element eVol = parent.addElement(AdminService.E_VOLUME);
        eVol.addAttribute(AdminService.A_ID, vol.getId());
        eVol.addAttribute(AdminService.A_VOLUME_TYPE, vol.getType());
        eVol.addAttribute(AdminService.A_VOLUME_NAME, vol.getName());
        eVol.addAttribute(AdminService.A_VOLUME_ROOTPATH, vol.getRootPath());
        eVol.addAttribute(AdminService.A_VOLUME_MGBITS, vol.getMboxGroupBits());
        eVol.addAttribute(AdminService.A_VOLUME_MBITS,  vol.getMboxBits());
        eVol.addAttribute(AdminService.A_VOLUME_FGBITS, vol.getFileGroupBits());
        eVol.addAttribute(AdminService.A_VOLUME_FBITS,  vol.getFileBits());
        eVol.addAttribute(AdminService.A_VOLUME_COMPRESS_BLOBS, vol.getCompressBlobs());
        eVol.addAttribute(AdminService.A_VOLUME_COMPRESSION_THRESHOLD,
                          vol.getCompressionThreshold());
    }
}
