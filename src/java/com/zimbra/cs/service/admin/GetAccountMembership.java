/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAccountMembership extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    public static final String BY_ADMIN_NAME = "adminName";
    public static final String BY_FOREIGN_PRINCIPAL = "foreignPrincipal";
    
    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        boolean applyCos = request.getAttributeBool(AdminService.A_APPLY_COS, true);        
        Element a = request.getElement(AdminService.E_ACCOUNT);
        String key = a.getAttribute(AdminService.A_BY);
        String value = a.getText();

        Account account = null;

        if (key.equals(BY_NAME)) {
            account = prov.getAccountByName(value);
        } else if (key.equals(BY_ID)) {
            account = prov.getAccountById(value);
        } else if (key.equals(BY_ADMIN_NAME)) {
            account = prov.getAdminAccountByName(value);
        } else if (key.equals(BY_FOREIGN_PRINCIPAL)) {
            account = prov.getAccountByForeignPrincipal(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

        if (!canAccessAccount(lc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        HashMap<String,String> via = new HashMap<String, String>();
        List<DistributionList> lists = account.getDistributionLists(false, via);
        
        Element response = lc.createElement(AdminService.GET_ACCOUNT_MEMBERSHIP_RESPONSE);
        for (DistributionList dl: lists) {
            Element distributionList = response.addElement(AdminService.E_DL);
            distributionList.addAttribute(AdminService.A_NAME, dl.getName());
            distributionList.addAttribute(AdminService.A_ID,dl.getId());
            distributionList.addAttribute(AdminService.A_ISGROUP,dl.isSecurityGroup());
            String viaDl = via.get(dl.getName());
            if (viaDl != null) distributionList.addAttribute(AdminService.A_VIA, viaDl);
        }

        return response;
    }
}
