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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.admin;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class RemoveDistributionListMember extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminService.E_ID);
        List<String> memberList = new LinkedList<String>();
        for (Element elem : request.listElements(AdminService.E_DLM)) {
        	memberList.add(elem.getTextTrim());
        }

        DistributionList dl = prov.get(DistributionListBy.ID, id);
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(id);

        if (!canAccessEmail(lc, dl.getName()))
            throw ServiceException.PERM_DENIED("can not access dl");

        String[] members = (String[]) memberList.toArray(new String[0]); 
        dl.removeMembers(members);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                                                      new String[] {"cmd", "RemoveDistributionListMember", "name", dl.getName(), "member", Arrays.deepToString(members)})); 

        Element response = lc.createElement(AdminService.REMOVE_DISTRIBUTION_LIST_MEMBER_RESPONSE);
        return response;
    }
}
