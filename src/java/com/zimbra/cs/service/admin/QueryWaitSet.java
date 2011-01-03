/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
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
        WaitSetMgr.checkRightForAllAccounts(zsc); // must be a global admin
        
        Element response = zsc.createElement(AdminConstants.QUERY_WAIT_SET_RESPONSE);
        
        String waitSetId = request.getAttribute(MailConstants.A_WAITSET_ID, null);
        
        List<IWaitSet> sets;
        
        if (waitSetId != null) {
            sets = new ArrayList<IWaitSet>(1);
            IWaitSet ws = WaitSetMgr.lookup(waitSetId);
            if (ws == null) {
                throw AdminServiceException.NO_SUCH_WAITSET(waitSetId);
            }
            sets.add(ws);
        } else {
            sets = WaitSetMgr.getAll();
        }

        for (IWaitSet set : sets) {
            Element waitSetElt = response.addElement(AdminConstants.E_WAITSET);
            set.handleQuery(waitSetElt);
        }
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
