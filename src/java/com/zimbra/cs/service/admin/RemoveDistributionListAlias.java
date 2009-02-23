/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class RemoveDistributionListAlias extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminConstants.E_ID, null);
        String alias = request.getAttribute(AdminConstants.E_ALIAS);

	    DistributionList dl = null;
	    if (id != null)
	        dl = prov.get(DistributionListBy.id, id);
            
        String dlName = "";
        if (dl != null) {
            checkDistributionListRight(zsc, dl, Admin.R_removeDistributionListAlias);
            dlName = dl.getName();
        }
        
        // if the admin can remove an alias in the domain
        checkDomainRightByEmail(zsc, alias, Admin.R_deleteAlias);
        
        prov.removeAlias(dl, alias);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "RemoveDistributionListAlias", "name", dlName, "alias", alias})); 
        
	    Element response = zsc.createElement(AdminConstants.REMOVE_DISTRIBUTION_LIST_ALIAS_RESPONSE);
	    return response;
	}
	
    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_removeDistributionListAlias);
        relatedRights.add(Admin.R_deleteAlias);
    }
}