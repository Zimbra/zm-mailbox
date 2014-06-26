/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
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
public class AddAccountAlias extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminConstants.E_ID);
        String alias = request.getAttribute(AdminConstants.E_ALIAS);

	    Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);

        // if the admin can add an alias for the account
        if (account.isCalendarResource()) {
            // need a CalendarResource instance for RightChecker
            CalendarResource resource = prov.get(Key.CalendarResourceBy.id, id);
            checkCalendarResourceRight(zsc, resource, Admin.R_addCalendarResourceAlias);
        } else
            checkAccountRight(zsc, account, Admin.R_addAccountAlias);

        // if the admin can create an alias in the domain
        checkDomainRightByEmail(zsc, alias, Admin.R_createAlias);

        prov.addAlias(account, alias);
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "AddAccountAlias","name", account.getName(), "alias", alias})); 
        
	    Element response = zsc.createElement(AdminConstants.ADD_ACCOUNT_ALIAS_RESPONSE);
	    return response;
	}
	
	@Override
	public void docRights(List<AdminRight> relatedRights, List<String> notes) {
	    relatedRights.add(Admin.R_addCalendarResourceAlias);
	    relatedRights.add(Admin.R_addAccountAlias);
	    relatedRights.add(Admin.R_createAlias);
	    
	    notes.add("Need " + Admin.R_createAlias.getName() + " right on the domain in which the alias is to be created.");
	    notes.add("Need " + Admin.R_addAccountAlias.getName() + " right if adding alias for an account.");
	    notes.add("Need " + Admin.R_addCalendarResourceAlias.getName() + " right if adding alias for a calendar resource.");
	}
}