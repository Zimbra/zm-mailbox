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

public class GetCurrentVolumes extends AdminDocumentHandler {

    public Element handle(Element request, Map context)
            throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);

        Element response = lc.createElement(AdminService.GET_CURRENT_VOLUMES_RESPONSE);

        Volume msgVol = Volume.getCurrentMessageVolume();
        Element eMsgVol = response.addElement(AdminService.E_CURRENT_VOLUME);
        eMsgVol.addAttribute(AdminService.A_VOLUME_TYPE, Volume.TYPE_MESSAGE_STR);
        eMsgVol.addAttribute(AdminService.A_ID, msgVol.getId());

        Volume indexVol = Volume.getCurrentIndexVolume();
        Element eIndexVol = response.addElement(AdminService.E_CURRENT_VOLUME);
        eIndexVol.addAttribute(AdminService.A_VOLUME_TYPE, Volume.TYPE_INDEX_STR);
        eIndexVol.addAttribute(AdminService.A_ID, indexVol.getId());

        return response;
    }
}
