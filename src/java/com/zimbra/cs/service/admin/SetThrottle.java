/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.operation.Scheduler;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class SetThrottle extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
                throws ServiceException, SoapFaultException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        // allow just system admin for now
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        
        String concurStr = request.getAttribute(AdminConstants.A_CONCURRENCY, null);
        
        if (concurStr != null) {
            int[] params = Scheduler.readOpsFromString(concurStr);
            if (params == null) 
                throw ServiceException.INVALID_REQUEST("Could not parse concurrency string: "+concurStr, null);
            Scheduler.setConcurrency(params);    
        }
        
        Element response = zsc.createElement(AdminConstants.SET_THROTTLE_RESPOSNE);
        
        Scheduler s = Scheduler.get(null);
        concurStr = "";
        int[] concur = s.getMaxOps();
        for (int i = 0; i < concur.length; i++) {
            if (i > 0)
                concurStr+=",";
            concurStr += concur[i];
        }
        response.addAttribute(AdminConstants.A_CONCURRENCY, concurStr);
        
        return response;
    }
    
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
    
}
