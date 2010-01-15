/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ModifyDomain extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminConstants.E_ID);
	    Map<String, Object> attrs = AdminService.getAttrs(request);
	    
	    Domain domain = prov.get(DomainBy.id, id);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(id);
        
        if (domain.isShutdown())
            throw ServiceException.PERM_DENIED("can not access domain, domain is in " + domain.getDomainStatusAsString() + " status");
        
        checkDomainRight(zsc, domain, attrs);
        
        // check to see if domain default cos is being changed, need right on new cos 
        checkCos(zsc, attrs);
        
        // pass in true to checkImmutable
        prov.modifyAttrs(domain, attrs, true);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyDomain","name", domain.getName()}, attrs));	    

        Element response = zsc.createElement(AdminConstants.MODIFY_DOMAIN_RESPONSE);
	    GetDomain.encodeDomain(response, domain);
	    return response;
	}
	
    private void checkCos(ZimbraSoapContext zsc, Map<String, Object> attrs) throws ServiceException {
        String newDomainCosId = ModifyAccount.getStringAttrNewValue(Provisioning.A_zimbraDomainDefaultCOSId, attrs);
        if (newDomainCosId == null)
            return;  // not changing it
        
        Provisioning prov = Provisioning.getInstance();
        if (newDomainCosId.equals("")) {
            // they are unsetting it, no problem
            return; 
        } 

        Cos cos = prov.get(CosBy.id, newDomainCosId);
        if (cos == null) {
            throw AccountServiceException.NO_SUCH_COS(newDomainCosId);
        }
        
        // call checkRight instead of checkCosRight, because:
        // 1. no domain based access manager backward compatibility issue
        // 2. we only want to check right if we are using pure ACL based access manager. 
        checkRight(zsc, cos, Admin.R_assignCos);
    }
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyDomain.getName(), "domain"));
        
        notes.add("Notes on " + Provisioning.A_zimbraDomainDefaultCOSId + ": " +
                "If setting " + Provisioning.A_zimbraDomainDefaultCOSId + ", needs the " + Admin.R_assignCos.getName() + 
                " right on the cos.");
    }
}