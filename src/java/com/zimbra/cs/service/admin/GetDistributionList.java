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

package com.zimbra.cs.service.admin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class GetDistributionList extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    public Element handle(Element request, Map context) throws ServiceException {
	    
        ZimbraContext lc = getZimbraContext(context);
        Provisioning prov = Provisioning.getInstance();
	    
        int limit = (int) request.getAttributeLong(AdminService.A_LIMIT, 0);
        if (limit < 0) {
        	throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        int offset = (int) request.getAttributeLong(AdminService.A_OFFSET, 0);
        if (offset < 0) {
        	throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
        boolean sortAscending = request.getAttributeBool(AdminService.A_SORT_ASCENDING, true);        

        Element d = request.getElement(AdminService.E_DL);
        String key = d.getAttribute(AdminService.A_BY);
        String value = d.getText();
	    
        DistributionList distributionList = null;
        
        if (key.equals(BY_NAME)) {
            distributionList = prov.getDistributionListByName(value);
        } else if (key.equals(BY_ID)) {
            distributionList = prov.getDistributionListById(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }
	    
        if (distributionList == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);

        Element response = lc.createElement(AdminService.GET_DISTRIBUTION_LIST_RESPONSE);
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
        	dlElement.addElement(AdminService.E_DLM).setText(members[i]);
        }
        
        response.addAttribute(AdminService.A_MORE, stop < members.length);
        response.addAttribute(AdminService.A_TOTAL, members.length);        
        return response;
    }

    public static Element doDistributionList(Element e, DistributionList d) throws ServiceException {
        Element distributionList = e.addElement(AdminService.E_DL);
        distributionList.addAttribute(AdminService.A_NAME, d.getName());
        distributionList.addAttribute(AdminService.A_ID,d.getId());
        doAttrs(distributionList, d.getAttrs());
        return distributionList;
    }

    static void doAttrs(Element e, Map attrs) throws ServiceException {
        for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
           Map.Entry entry = (Entry) mit.next();
           String name = (String) entry.getKey();
           Object value = entry.getValue();
           
           // Hide the postfix lookup table attr - should we though?
           if (name.equals(Provisioning.A_zimbraMailAlias)) {
               continue;
           }
           
           // Hide the postfix lookup table attr - should we though?
           if (name.equals(Provisioning.A_zimbraMailForwardingAddress)) {
        	   continue;
           }

           if (value instanceof String[]) {
               String sv[] = (String[]) value;
               for (int i = 0; i < sv.length; i++)
                   e.addAttribute(name, sv[i], Element.DISP_ELEMENT);
           } else if (value instanceof String) {
               e.addAttribute(name, (String) value, Element.DISP_ELEMENT);
           }
       }       
   }

    
}
