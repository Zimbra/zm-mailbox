/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.List;
import java.util.Map;

/**
 * @author schemers
 */
public class CreateAccount extends AdminDocumentHandler {

    /**
     * must be careful and only create accounts for the domain admin!
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String name = request.getAttribute(AdminConstants.E_NAME).toLowerCase();
	    String password = request.getAttribute(AdminConstants.E_PASSWORD, null);
	    Map<String, Object> attrs = AdminService.getAttrs(request, true);

	    checkDomainRightByEmail(zsc, name, Admin.R_createAccount);
	    checkSetAttrsOnCreate(zsc, TargetType.account, name, attrs);
	    checkCos(zsc, attrs);
        
	    Account account = prov.createAccount(name, password, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateAccount","name", name}, attrs));         

	    Element response = zsc.createElement(AdminConstants.CREATE_ACCOUNT_RESPONSE);

        ToXML.encodeAccount(response, account);

	    return response;
	}
	
	private void checkCos(ZimbraSoapContext zsc, Map<String, Object> attrs) throws ServiceException {
        String cosId = ModifyAccount.getStringAttrNewValue(Provisioning.A_zimbraCOSId, attrs);
        if (cosId == null)
            return;  // not setting it
        
        Provisioning prov = Provisioning.getInstance();

        Cos cos = prov.get(CosBy.id, cosId);
        if (cos == null) {
            throw AccountServiceException.NO_SUCH_COS(cosId);
        }
        
        // call checkRight instead of checkCosRight, because:
        // 1. no domain based access manager backward compatibility issue
        // 2. we only want to check right if we are using pure ACL based access manager. 
        checkRight(zsc, cos, Admin.R_assignCos);
    }
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_createAccount);
        
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyAccount.getName(), "account"));
        
        notes.add("Notes on " + Provisioning.A_zimbraCOSId + ": " +
                "If setting " + Provisioning.A_zimbraCOSId + ", needs the " + Admin.R_assignCos.getName() + 
                " right on the cos.");
    }
}