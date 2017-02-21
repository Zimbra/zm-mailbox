/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.List;
import java.util.Map;

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
	    
	    Domain domain = prov.get(Key.DomainBy.id, id);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(id);
        
        if (domain.isShutdown())
            throw ServiceException.PERM_DENIED("can not access domain, domain is in " + domain.getDomainStatusAsString() + " status");
        
        checkDomainRight(zsc, domain, attrs);
        
        // check to see if domain default cos is being changed, need right on new cos 
        checkCos(zsc, attrs, Provisioning.A_zimbraDomainDefaultCOSId);
        checkCos(zsc, attrs, Provisioning.A_zimbraDomainDefaultExternalUserCOSId);

        // pass in true to checkImmutable
        prov.modifyAttrs(domain, attrs, true);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyDomain","name", domain.getName()}, attrs));	    

        Element response = zsc.createElement(AdminConstants.MODIFY_DOMAIN_RESPONSE);
	    GetDomain.encodeDomain(response, domain);
	    return response;
	}
	
    private void checkCos(ZimbraSoapContext zsc, Map<String, Object> attrs, String defaultCOSIdAttrName)
            throws ServiceException {
        String newDomainCosId = ModifyAccount.getStringAttrNewValue(defaultCOSIdAttrName, attrs);
        if (newDomainCosId == null)
            return;  // not changing it
        
        Provisioning prov = Provisioning.getInstance();
        if (newDomainCosId.equals("")) {
            // they are unsetting it, no problem
            return; 
        } 

        Cos cos = prov.get(Key.CosBy.id, newDomainCosId);
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