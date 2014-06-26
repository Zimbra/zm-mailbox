/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
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
        
        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, 0);
        if (limit < 0) {
            throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        if (offset < 0) {
            throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
//        boolean sortAscending = request.getAttributeBool(AdminService.A_SORT_ASCENDING, true);        

        Element d = request.getElement(AdminConstants.E_DL);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
        
        DistributionList distributionList = prov.get(Key.DistributionListBy.fromString(key), value);        
        
        if (distributionList == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
        
        checkDistributionListRight(lc, distributionList, Admin.R_getDistributionListMembership);

        HashMap<String,String> via = new HashMap<String, String>();
        List<DistributionList> lists = prov.getDistributionLists(distributionList, false, via);
        
        Element response = lc.createElement(AdminConstants.GET_DISTRIBUTION_LIST_MEMBERSHIP_RESPONSE);
        for (DistributionList dl: lists) {
            Element dlEl = response.addElement(AdminConstants.E_DL);
            dlEl.addAttribute(AdminConstants.A_NAME, dl.getName());
            dlEl.addAttribute(AdminConstants.A_ID,dl.getId());
            String viaDl = via.get(dl.getName());
            if (viaDl != null) dlEl.addAttribute(AdminConstants.A_VIA, viaDl);
        }

        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getDistributionListMembership);
    }
}
