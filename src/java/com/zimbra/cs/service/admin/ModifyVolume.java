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

import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.Volume;
import com.zimbra.soap.ZimbraContext;

public class ModifyVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map context)
            throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        short id = (short) request.getAttributeLong(AdminService.A_ID);
        Volume vol = Volume.getById(id);
        Element eVol = request.getElement(AdminService.E_VOLUME);
        Map attrs = AdminService.getAttrs(eVol, true);
        applyAttrsToVolume(vol, attrs);
        Element response = lc.createElement(AdminService.MODIFY_VOLUME_RESPONSE);
        return response;
    }

    private static void applyAttrsToVolume(Volume vol, Map attrs)
    throws ServiceException {
        short type = vol.getType();
        String name = vol.getName();
        String path = vol.getRootPath();
        short mgbits = vol.getMboxGroupBits();
        short mbits = vol.getMboxBits();
        short fgbits = vol.getFileGroupBits();
        short fbits = vol.getFileBits();

        for (Iterator iter = attrs.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = null;
            Object val = entry.getValue();
            if (val instanceof String)
                value = (String) val;
            else if (val instanceof String[]) {
                String[] vals = (String[]) val;
                if (vals.length >= 1)
                    value = vals[0];
            }

            if (key.equals(GetVolume.A_VOLUME_TYPE))
                type = Short.parseShort(value);
            else if (key.equals(GetVolume.A_VOLUME_NAME))
                name = value;
            else if (key.equals(GetVolume.A_VOLUME_ROOTPATH))
                path = value;
            else if (key.equals(GetVolume.A_VOLUME_MGBITS))
                mgbits = Short.parseShort(value);
            else if (key.equals(GetVolume.A_VOLUME_MBITS))
                mbits = Short.parseShort(value);
            else if (key.equals(GetVolume.A_VOLUME_FGBITS))
                fgbits = Short.parseShort(value);
            else if (key.equals(GetVolume.A_VOLUME_FBITS))
                fbits = Short.parseShort(value);
        }

        Volume.update(vol.getId(), type, name, path, mgbits, mbits, fgbits, fbits);
    }
}
