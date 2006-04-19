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

public class SetCurrentVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraContext(context);

        short volType = (short) request.getAttributeLong(AdminService.A_VOLUME_TYPE);
        long idLong = request.getAttributeLong(AdminService.A_ID, Volume.ID_NONE);
        Volume.validateID(idLong, true);  // avoid Java truncation
        short id = (short) idLong;
        Volume.setCurrentVolume(volType, id);

        Element response = lc.createElement(AdminService.SET_CURRENT_VOLUME_RESPONSE);
        return response;
    }
}
