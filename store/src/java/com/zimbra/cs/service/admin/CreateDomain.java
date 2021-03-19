/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.PseudoTarget;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.listeners.DomainListener;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CreateDomain extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
	    String name = request.getAttribute(AdminConstants.E_NAME).toLowerCase();
	    Map<String, Object> attrs = AdminService.getAttrs(request, true);
	    
	    // check permission
	    if (name.indexOf('.') == -1) {
	        // is a top domain
	        checkRight(zsc, context, null, Admin.R_createTopDomain);
	    } else {
	        // go up the domain hierarchy see if any of the parent domains exist.
            // If yes, check the createSubDomain right on the lowest existing parent domain.
            // If not, allow it if the admin has both the createTopDomain on globalgrant; and 
            // use a pseudo Domain object as the target to check the createSubDomain right
            // (because createSubDomain is a domain right, we cannot use globalgrant for the target).
    	    String domainName = name;
    	    Domain parentDomain = null;
    	    while (parentDomain == null) {
    	        int nextDot = domainName.indexOf('.');
                if (nextDot == -1) {
                    // reached the top, check if the admin has the createTopDomain right on globalgrant
                    checkRight(zsc, context, null, Admin.R_createTopDomain);
                    
                    // then create a pseudo domain for checking the createSubDomain right
                    parentDomain = PseudoTarget.createPseudoDomain(prov);
                    break;
                } else {
                    domainName = domainName.substring(nextDot+1);
                    parentDomain = Provisioning.getInstance().get(Key.DomainBy.name, domainName);
                }
    	    }
    	    checkRight(zsc, context, parentDomain, Admin.R_createSubDomain);
	    }
	    
	    // check if all the attrs can be set and within constraints
        checkSetAttrsOnCreate(zsc, TargetType.domain, name, attrs);
	    
	    Domain domain = prov.createDomain(name, attrs);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateDomain","name", name}, attrs));         

	    Element response = zsc.createElement(AdminConstants.CREATE_DOMAIN_RESPONSE);
	    GetDomain.encodeDomain(response, domain);

	    DomainListener.invokeOnDomainCreation(domain);

	    return response;
	}
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_createTopDomain);
        relatedRights.add(Admin.R_createSubDomain);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyDomain.getName(), "domain"));
    }
}