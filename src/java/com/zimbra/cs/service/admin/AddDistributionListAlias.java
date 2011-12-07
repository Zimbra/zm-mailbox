/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;

public class AddDistributionListAlias extends DistributionListDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    @Override
    protected Group getGroup(Element request) throws ServiceException {
        String id = request.getAttribute(AdminConstants.E_ID);
        return Provisioning.getInstance().getGroup(DistributionListBy.id, id);
    }

	public Element handle(Element request, Map<String, Object> context) 
	throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
        String alias = request.getAttribute(AdminConstants.E_ALIAS);

        Group group = getGroupFromContext(context);
        if (group == null) {
            String id = request.getAttribute(AdminConstants.E_ID);
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(id);
        }
        
        if (group.isDynamic()) {
            checkDynamicGroupRight(lc, (DynamicGroup) group, Admin.R_addGroupAlias);
        } else {
            checkDistributionListRight(lc, (DistributionList) group, Admin.R_addDistributionListAlias);
        }
        
        // if the admin can create an alias in the domain
        checkDomainRightByEmail(lc, alias, Admin.R_createAlias);

        prov.addGroupAlias(group, alias);
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "AddDistributionListAlias", "name", group.getName(), "alias", alias}));

        return lc.createElement(AdminConstants.ADD_DISTRIBUTION_LIST_ALIAS_RESPONSE);
	}
	
	@Override
	public void docRights(List<AdminRight> relatedRights, List<String> notes) {
	    relatedRights.add(Admin.R_addDistributionListAlias);
	    relatedRights.add(Admin.R_addGroupAlias);
	    relatedRights.add(Admin.R_createAlias);
    }
}