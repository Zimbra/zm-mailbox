/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.account.GetShareInfo.ResultFilter;
import com.zimbra.cs.service.account.GetShareInfo.ResultFilterByTarget;
import com.zimbra.cs.service.account.GetShareInfo.ShareInfoVisitor;
import com.zimbra.soap.ZimbraSoapContext;

public class GetShareInfo extends ShareInfoHandler {
    
    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_OWNER };
    protected String[] getProxiedAccountElementPath()  { return TARGET_ACCOUNT_PATH; }
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Provisioning prov = Provisioning.getInstance();
        
        Element eGrantee = request.getOptionalElement(AccountConstants.E_GRANTEE);
        byte granteeType = com.zimbra.cs.service.account.GetShareInfo.getGranteeType(eGrantee);
        String granteeId = eGrantee == null? null : eGrantee.getAttribute(AccountConstants.A_ID, null);
        String granteeName = eGrantee == null? null : eGrantee.getAttribute(AccountConstants.A_NAME, null);
        
        Element eOwner = request.getElement(AccountConstants.E_OWNER);
        Account ownerAcct = null;
        AccountBy acctBy = AccountBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
        String key = eOwner.getText();
        ownerAcct = prov.get(acctBy, key);
            
        // in the account namespace GetShareInfo
        // to defend against harvest attacks return "no shares" instead of error 
        // when an invalid user name/id is used.
        //
        // this is the admin namespace GetShareInfo, we want to let the admin know if 
        // the owner name is bad
        if (ownerAcct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(key);
        
        checkAdminLoginAsRight(zsc, prov, ownerAcct);
        
        Element response = zsc.createElement(AdminConstants.GET_SHARE_INFO_RESPONSE);
        
        ResultFilter resultFilter = new ResultFilterByTarget(granteeId, granteeName);
        ShareInfoVisitor visitor = new ShareInfoVisitor(prov, response, null, resultFilter);
        ShareInfo.Discover.discover(octxt, prov, null, granteeType, ownerAcct, visitor);
        visitor.finish();
        
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_adminLoginAs);
        relatedRights.add(Admin.R_adminLoginCalendarResourceAs);
        notes.add(AdminRightCheckPoint.Notes.ADMIN_LOGIN_AS);
    }
}