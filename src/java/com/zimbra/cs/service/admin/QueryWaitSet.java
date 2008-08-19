/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.session.IWaitSet;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * This API is used to dump the internal state of a wait set.  This API is intended
 * for debugging use only.
 */
public class QueryWaitSet extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(AdminConstants.QUERY_WAIT_SET_RESPONSE);
        
        String waitSetId = request.getAttribute(MailConstants.A_WAITSET_ID);
        
        IWaitSet ws = WaitSetMgr.lookup(waitSetId);
        if (ws == null) {
            throw AdminServiceException.NO_SUCH_WAITSET(waitSetId);
        }
        ws.handleQuery(response);
        return response;
    }
}
