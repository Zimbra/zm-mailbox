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

public class CreateVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map context)
            throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Element eVol = request.getElement(AdminService.E_VOLUME);
        Map attrs = AdminService.getAttrs(eVol, true);
        Volume vol = createFromAttrs(attrs);
        Element response = lc.createElement(AdminService.CREATE_VOLUME_RESPONSE);
        GetVolume.addVolumeElement(response, vol);
        return response;
    }

    private static Volume createFromAttrs( Map attrs)
    throws ServiceException {
        short type = -1;
        String name = null;
        String path = null;
        short mgbits = -1;
        short mbits = -1;
        short fgbits = -1;
        short fbits = -1;

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
            if (key.equals(GetVolume.A_VOLUME_NAME))
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

        if (type == -1)
            throw ServiceException.INVALID_REQUEST("Missing attribute " + GetVolume.A_VOLUME_TYPE, null);
        if (name == null)
            throw ServiceException.INVALID_REQUEST("Missing attribute " + GetVolume.A_VOLUME_NAME, null);
        if (path == null || path.length() < 1)
            throw ServiceException.INVALID_REQUEST("Missing attribute " + GetVolume.A_VOLUME_ROOTPATH, null);
        if (mgbits == -1)
            throw ServiceException.INVALID_REQUEST("Missing attribute " + GetVolume.A_VOLUME_MGBITS, null);
        if (mbits == -1)
            throw ServiceException.INVALID_REQUEST("Missing attribute " + GetVolume.A_VOLUME_MBITS, null);
        if (fgbits == -1)
            throw ServiceException.INVALID_REQUEST("Missing attribute " + GetVolume.A_VOLUME_FGBITS, null);
        if (fbits == -1)
            throw ServiceException.INVALID_REQUEST("Missing attribute " + GetVolume.A_VOLUME_FBITS, null);

        Volume vol = Volume.create(Volume.ID_AUTO_INCREMENT, type,
                                   name, path, mgbits, mbits, fgbits, fbits);
        return vol;
    }
}
