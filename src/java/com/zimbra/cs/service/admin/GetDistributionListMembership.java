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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
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
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetDistributionListMembership extends AdminDocumentHandler {

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        int limit = (int) request.getAttributeLong(AdminService.A_LIMIT, 0);
        if (limit < 0) {
            throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        int offset = (int) request.getAttributeLong(AdminService.A_OFFSET, 0);
        if (offset < 0) {
            throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
//        boolean sortAscending = request.getAttributeBool(AdminService.A_SORT_ASCENDING, true);        

        Element d = request.getElement(AdminService.E_DL);
        String key = d.getAttribute(AdminService.A_BY);
        String value = d.getText();
        
        DistributionList distributionList = prov.get(DistributionListBy.fromString(key), value);        
        
        if (distributionList == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);

        if (!canAccessEmail(lc, distributionList.getName()))
            throw ServiceException.PERM_DENIED("can not access dl");

        HashMap<String,String> via = new HashMap<String, String>();
        List<DistributionList> lists = prov.getDistributionLists(distributionList, false, via);
        
        Element response = lc.createElement(AdminService.GET_DISTRIBUTION_LIST_MEMBERSHIP_RESPONSE);
        for (DistributionList dl: lists) {
            Element dlEl = response.addElement(AdminService.E_DL);
            dlEl.addAttribute(AdminService.A_NAME, dl.getName());
            dlEl.addAttribute(AdminService.A_ID,dl.getId());
            String viaDl = via.get(dl.getName());
            if (viaDl != null) dlEl.addAttribute(AdminService.A_VIA, viaDl);
        }

        return response;
    }
}
