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
import com.zimbra.cs.store.Volume;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class SetCurrentVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        short volType = (short) request.getAttributeLong(AdminConstants.A_VOLUME_TYPE);
        long idLong = request.getAttributeLong(AdminConstants.A_ID, Volume.ID_NONE);
        Volume.validateID(idLong, true);  // avoid Java truncation
        short id = (short) idLong;
        Volume.setCurrentVolume(volType, id);

        Element response = lc.createElement(AdminConstants.SET_CURRENT_VOLUME_RESPONSE);
        return response;
    }
}
