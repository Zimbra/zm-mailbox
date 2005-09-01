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

    public static final String A_VOLUME_TYPE = "type";
    public static final String A_VOLUME_NAME = "name";
    public static final String A_VOLUME_ROOTPATH = "rootpath";
    public static final String A_VOLUME_MGBITS = "mgbits";
    public static final String A_VOLUME_MBITS = "mbits";
    public static final String A_VOLUME_FGBITS = "fgbits";
    public static final String A_VOLUME_FBITS = "fbits";

    public Element handle(Element request, Map context)
            throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Element eVol = request.getElement(AdminService.E_VOLUME);
        short id = (short) eVol.getAttributeLong(AdminService.A_ID);
        Volume vol = Volume.getById(id);
        Element response = lc.createElement(AdminService.GET_VOLUME_RESPONSE);
        addVolumeElement(response, vol);
        return response;
    }

    public static void addVolumeElement(Element parent, Volume vol)
    throws ServiceException {
        Element eVol = parent.addElement(AdminService.E_VOLUME);
        eVol.addAttribute(AdminService.A_ID, vol.getId());

        Element type = eVol.addElement(AdminService.E_A);
        type.addAttribute(AdminService.A_N, A_VOLUME_TYPE);
        type.setText(Short.toString(vol.getType()));

        Element name = eVol.addElement(AdminService.E_A);
        name.addAttribute(AdminService.A_N, A_VOLUME_NAME);
        name.setText(vol.getName());

        Element path = eVol.addElement(AdminService.E_A);
        path.addAttribute(AdminService.A_N, A_VOLUME_ROOTPATH);
        path.setText(vol.getRootPath());

        Element mgbits = eVol.addElement(AdminService.E_A);
        mgbits.addAttribute(AdminService.A_N, A_VOLUME_MGBITS);
        mgbits.setText(Short.toString(vol.getMboxGroupBits()));

        Element mbits = eVol.addElement(AdminService.E_A);
        mbits.addAttribute(AdminService.A_N, A_VOLUME_MBITS);
        mbits.setText(Short.toString(vol.getMboxBits()));

        Element fgbits = eVol.addElement(AdminService.E_A);
        fgbits.addAttribute(AdminService.A_N, A_VOLUME_FGBITS);
        fgbits.setText(Short.toString(vol.getFileGroupBits()));

        Element fbits = eVol.addElement(AdminService.E_A);
        fbits.addAttribute(AdminService.A_N, A_VOLUME_FBITS);
        fbits.setText(Short.toString(vol.getFileBits()));
    }
}
