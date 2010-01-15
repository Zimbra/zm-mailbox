/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionList extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
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
        boolean sortAscending = request.getAttributeBool(AdminConstants.A_SORT_ASCENDING, true);

        Element d = request.getElement(AdminConstants.E_DL);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
	    
        DistributionList distributionList = prov.get(DistributionListBy.fromString(key), value);
	    
        if (distributionList == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);

        if (!canAccessEmail(lc, distributionList.getName()))
            throw ServiceException.PERM_DENIED("can not access dl");
            
        Element response = lc.createElement(AdminConstants.GET_DISTRIBUTION_LIST_RESPONSE);
        Element dlElement = doDistributionList(response, distributionList);
        
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
        return response;
    }

    public static Element doDistributionList(Element e, DistributionList d) throws ServiceException {
        Element distributionList = e.addElement(AdminConstants.E_DL);
        distributionList.addAttribute(AdminConstants.A_NAME, d.getName());
        distributionList.addAttribute(AdminConstants.A_ID,d.getId());
        doAttrs(distributionList, d.getUnicodeAttrs());
        return distributionList;
    }

    static void doAttrs(Element e, Map attrs) {
        for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
           Map.Entry entry = (Entry) mit.next();
           String name = (String) entry.getKey();
           Object value = entry.getValue();
           
           // Hide the postfix lookup table attr - should we though?
           if (name.equals(Provisioning.A_zimbraMailForwardingAddress)) {
        	   continue;
           }
           
           if (value instanceof String[]) {
               String sv[] = (String[]) value;
               for (int i = 0; i < sv.length; i++)
                   e.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(sv[i]);
           } else if (value instanceof String) {
               e.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText((String) value);
           }
       }       
   }

    
}
