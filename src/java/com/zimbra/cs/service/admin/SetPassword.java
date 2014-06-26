/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.SetPasswordResult;
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
public class SetPassword extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow on accounts domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminConstants.E_ID);
        String newPassword = request.getAttribute(AdminConstants.E_NEW_PASSWORD);

	    Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);
        
        boolean enforcePasswordPolicy;
        if (account.isCalendarResource()) {
            // need a CalendarResource instance for RightChecker
            CalendarResource resource = prov.get(CalendarResourceBy.id, id);
            enforcePasswordPolicy = checkCalendarResourceRights(zsc, resource);
        } else {
            enforcePasswordPolicy = checkAccountRights(zsc, account);
        }
        
        SetPasswordResult result = prov.setPassword(account, newPassword, enforcePasswordPolicy);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "SetPassword","name", account.getName()}));


	    Element response = zsc.createElement(AdminConstants.SET_PASSWORD_RESPONSE);
	            
        if (result.hasMessage()) {
            ZimbraLog.security.info(result.getMessage());
            response.addElement(AdminConstants.E_MESSAGE).setText(result.getMessage());
        }
        
	    return response;
	}
	
	/*
	 * returns whether password strength policies should be enforced for the authed user
	 * 
	 * returns false if user can setAccountPassword
	 * returns true if user cannot setAccountPassword but can changeAccountPassword
	 * 
	 * throws PERM_DENIED if user doesn't have either right
	 */
	private boolean checkAccountRights(ZimbraSoapContext zsc, Account acct) 
	throws ServiceException {
	    try {
	        checkAccountRight(zsc, acct, Admin.R_setAccountPassword);
	        return false;
	    } catch (ServiceException e) {
	        if (ServiceException.PERM_DENIED.equals(e.getCode())) {
	            checkAccountRight(zsc, acct, Admin.R_changeAccountPassword);
	            return true;
	        } else {
	            throw e;
	        }
	    }
	}

	/*
     * returns whether password strength policies should be enforced for the authed user
     * 
     * returns false if user can setCalendarResourcePassword
     * returns true if user cannot setCalendarResourcePassword but can changeCalendarResourcePassword
     * 
     * throws PERM_DENIED if user doesn't have either right
     */
    private boolean checkCalendarResourceRights(ZimbraSoapContext zsc, CalendarResource cr) 
    throws ServiceException {
        try {
            checkCalendarResourceRight(zsc, cr, Admin.R_setCalendarResourcePassword);
            return false;
        } catch (ServiceException e) {
            if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                checkCalendarResourceRight(zsc, cr, Admin.R_changeCalendarResourcePassword);
                return true;
            } else {
                throw e;
            }
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_setAccountPassword);
        relatedRights.add(Admin.R_changeAccountPassword);
        relatedRights.add(Admin.R_setCalendarResourcePassword);
        relatedRights.add(Admin.R_changeCalendarResourcePassword);
    }
}