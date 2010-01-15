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

public class DeleteVolume extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        long idLong = request.getAttributeLong(AdminConstants.A_ID);
        Volume.validateID(idLong);  // avoid Java truncation
        short id = (short) idLong;
        // make sure the volume exists before doing anything heavyweight...
        Volume.getById(id);
        Volume.delete(id);

        Element response = lc.createElement(AdminConstants.DELETE_VOLUME_RESPONSE);
        return response;
    }
}
