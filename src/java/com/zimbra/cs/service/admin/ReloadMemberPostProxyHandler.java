/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;

public abstract class ReloadMemberPostProxyHandler extends
        DistributionListDocumentHandler {

    @Override
    public boolean domainAuthSufficient(@SuppressWarnings("rawtypes") Map context) {
        return true;
    }

    @Override
    protected Group getGroup(Element request) throws ServiceException {
        String id = request.getAttribute(AdminConstants.E_ID);
        return Provisioning.getInstance().getGroup(DistributionListBy.id, id);
    }

    protected List<String> getMemberList(Element request, Map<String, Object> context) throws ServiceException {
        List<String> memberList = new LinkedList<String>();
        for (Element elem : request.listElements(AdminConstants.E_DLM)) {
            memberList.add(elem.getTextTrim());
        }
        return memberList;
    }

    @Override
    public void postProxy(Element request, Element response,
            Map<String, Object> context) throws ServiceException {
        List<String> memberList = getMemberList(request, context);
        Provisioning prov = Provisioning.getInstance();
        for (String memberName : memberList) {
            Account acct = prov.get(AccountBy.name, memberName);
            if (acct != null) {
                prov.reload(acct);
            }
        }
    }
}
