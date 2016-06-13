/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.RemoveDistributionListMemberResponse;

public class RemoveDistributionListMember extends ReloadMemberPostProxyHandler {

    /**
     * @return true - which means accept responsibility for measures to prevent account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    @Override
    protected List<String> getMemberList(Element request, Map<String, Object> context)
            throws ServiceException {
        List<String> memberList = super.getMemberList(request, context);
        Group group = getGroupFromContext(context);
        memberList = addMembersFromAccountElements(request, memberList, group);
        return memberList;
    }

    private List<String> addMembersFromAccountElements(Element request, List<String> memberList, Group group) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        for (Element elem : request.listElements(AdminConstants.E_ACCOUNT)) {
            Set<String> listAddresses = group.getAllMembersSet();
            Account account = prov.getAccount(elem.getTextTrim());
            if(account != null) {
                if(listAddresses.contains(account.getMail())) {
                    memberList.add(account.getMail());
                }
                List<String> accountAddresses = Arrays.asList(account.getAliases());
                for(String addr : accountAddresses) {
                    if(listAddresses.contains(addr)) {
                        memberList.add(addr);
                    }
                }
            }
        }
        return memberList;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        List<String> memberList = getMemberList(request, context);

        Group group = getGroupFromContext(context);
        String id = request.getAttribute(AdminConstants.E_ID);
        defendAgainstGroupHarvesting(group, DistributionListBy.id, id, zsc,
                Admin.R_removeGroupMember, Admin.R_removeDistributionListMember);

        memberList = addMembersFromAccountElements(request, memberList, group);

        String[] members = memberList.toArray(new String[0]);
        prov.removeGroupMembers(group, members);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "RemoveDistributionListMember", "name", group.getName(),
                "member", Arrays.deepToString(members)}));
        return zsc.jaxbToElement(new RemoveDistributionListMemberResponse());
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_removeDistributionListMember);
        relatedRights.add(Admin.R_removeGroupMember);
    }
}
