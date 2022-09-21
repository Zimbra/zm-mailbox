/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllDistributionLists extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    /**
     * must be careful and only allow on dls domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element response = null;
        
        Element d = request.getOptionalElement(AdminConstants.E_DOMAIN);
        Domain domain = null;
        
        if (d != null) {
            String key = d.getAttribute(AdminConstants.A_BY);
            String value = d.getText();
        
            if (key.equals(BY_NAME)) {
                domain = prov.get(Key.DomainBy.name, value);
            } else if (key.equals(BY_ID)) {
                domain = prov.get(Key.DomainBy.id, value);
            } else {
                throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
            }
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(value);
        }
        
        if (isDomainAdminOnly(zsc)) {
            if (domain != null) { 
                checkDomainRight(zsc, domain, AdminRight.PR_ALWAYS_ALLOW); 
            }
            domain = getAuthTokenAccountDomain(zsc);
        }

        if (domain != null) {
            response = zsc.createElement(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            doDomain(zsc, response, domain);
        } else {
            response = zsc.createElement(AdminConstants.GET_ALL_DISTRIBUTION_LISTS_RESPONSE);
            List domains = prov.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain dm = (Domain) dit.next();
                doDomain(zsc, response, dm);
            }
        }
        return response;
    }

    private void doDomain(ZimbraSoapContext zsc, Element e, Domain d) throws ServiceException {
        List dls = Provisioning.getInstance().getAllGroups(d);
        Element eDL = null;
        AdminAccessControl aac = null;
        AttrRightChecker arc = null;
        for (Iterator it = dls.iterator(); it.hasNext(); ) {
            Group dl = (Group) it.next();
            boolean hasRightToList = true;
            boolean allowMembers = true;

            if (dl.isDynamic()) {
                aac = checkDynamicGroupRight(zsc, (DynamicGroup) dl, AdminRight.PR_ALWAYS_ALLOW);
                arc = aac.getAttrRightChecker(dl);
                hasRightToList = true;
                allowMembers = arc == null ? true : arc.allowAttr(Provisioning.A_member);
            } else {
                aac = checkDistributionListRight(zsc,
                        (DistributionList) dl, AdminRight.PR_ALWAYS_ALLOW);
                arc = aac.getAttrRightChecker(dl);
                allowMembers = arc == null ? true : arc.allowAttr(Provisioning.A_zimbraMailForwardingAddress);
                hasRightToList = aac.hasRightsToList(dl, Admin.R_listDistributionList, null);
            }
            if (hasRightToList) {
                eDL = GetDistributionList.encodeDistributionList(e, dl, true, false, null, arc);
            }
            if (allowMembers) {
                encodeMembers(e, eDL, dl);
            }
        }
    }

    private void encodeMembers(Element response, Element dlElement, Group group)
            throws ServiceException {
       String[] members;
       if (group instanceof DynamicGroup) {
           members = ((DynamicGroup)group).getAllMembers(true);
       } else {
           members = group.getAllMembers();
       }

       Arrays.sort(members);
       for (int i = 0; i < members.length; i++) {
           dlElement.addElement(AdminConstants.E_DLM).setText(members[i]);
       }

       response.addAttribute(AdminConstants.A_TOTAL, members.length);
   }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listDistributionList);
        relatedRights.add(Admin.R_getDistributionList);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
