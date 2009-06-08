/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionList extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
	    
        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, 0);
        if (limit < 0) {
        	throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        if (offset < 0) {
        	throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
        boolean sortAscending = request.getAttributeBool(AdminConstants.A_SORT_ASCENDING, true);
        Set<String> reqAttrs = getReqAttrs(request, AttributeClass.distributionList);
        
        Element d = request.getElement(AdminConstants.E_DL);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
	    
        DistributionList distributionList = prov.get(DistributionListBy.fromString(key), value);
	    
        if (distributionList == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);

        AdminAccessControl aac = checkDistributionListRight(zsc, distributionList, AdminRight.PR_ALWAYS_ALLOW);
        AttrRightChecker arc = aac.getAttrRightChecker(distributionList);
            
        Element response = zsc.createElement(AdminConstants.GET_DISTRIBUTION_LIST_RESPONSE);
        Element dlElement = encodeDistributionList(response, distributionList, true, reqAttrs, arc);
        
        // return member info only if the authed has right to see zimbraMailForwardingAddress
        boolean allowMembers = arc == null ? true : arc.allowAttr(Provisioning.A_zimbraMailForwardingAddress);
           
        if (allowMembers)
            encodeMembers(response, dlElement, distributionList, offset, limit, sortAscending);

        return response;
    }
    
    private void encodeMembers(Element response, Element dlElement, DistributionList distributionList, 
            int offset, int limit, boolean sortAscending) throws ServiceException {
        String[] members = distributionList.getAllMembers();
        if (offset > 0 && offset >= members.length) {
            throw ServiceException.INVALID_REQUEST("offset " + offset + " greater than size " + members.length, null);
        }
        int stop = offset + limit;
        if (limit == 0) {
            stop = members.length;
        }
        if (stop > members.length) {
            stop = members.length;
        }
        
        if (sortAscending) {
            Arrays.sort(members);
        } else {
            Arrays.sort(members, Collections.reverseOrder());
        }
        for (int i = offset; i < stop; i++) {
            dlElement.addElement(AdminConstants.E_DLM).setText(members[i]);
        }
        
        response.addAttribute(AdminConstants.A_MORE, stop < members.length);
        response.addAttribute(AdminConstants.A_TOTAL, members.length);
    }

    public static Element encodeDistributionList(Element e, DistributionList d) throws ServiceException {
        return encodeDistributionList(e, d, true, null, null);
    }
    
    public static Element encodeDistributionList(Element e, DistributionList d, boolean hideZMFA, Set<String> reqAttrs, 
            AttrRightChecker attrRightChecker) throws ServiceException {
        Element distributionList = e.addElement(AdminConstants.E_DL);
        distributionList.addAttribute(AdminConstants.A_NAME, d.getName());
        distributionList.addAttribute(AdminConstants.A_ID,d.getId());
        encodeDistributionListAttrs(distributionList, d.getUnicodeAttrs(), hideZMFA, reqAttrs, attrRightChecker);
        return distributionList;
    }

    static void encodeDistributionListAttrs(Element e, Map attrs, boolean hideZMFA, Set<String> reqAttrs, AttrRightChecker attrRightChecker) 
    throws ServiceException {
        AttributeManager attrMgr = AttributeManager.getInstance();
        
        for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
           
            // Hide the postfix lookup table attr - should we though?
            if (hideZMFA && name.equals(Provisioning.A_zimbraMailForwardingAddress)) {
                continue;
            }
            
            // only return requested attrs
            if (reqAttrs != null && !reqAttrs.contains(name))
                continue;
            
            boolean allowed = attrRightChecker == null ? true : attrRightChecker.allowAttr(name);
            
            IDNType idnType = AttributeManager.idnType(attrMgr, name);
            
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    ToXML.encodeAttr(e, name, sv[i], AdminConstants.E_A, AdminConstants.A_N, idnType, allowed);
            } else if (value instanceof String) {
                ToXML.encodeAttr(e, name, (String)value, AdminConstants.E_A, AdminConstants.A_N, idnType, allowed);
            } 
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getDistributionList);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getDistributionList.getName()));
    }
}
