/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.service.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapDistributionList;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.HABGroup;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @author zimbra
 *
 */
public class GetHAB extends AccountDocumentHandler {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element,
     * java.util.Map)
     * 
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        String rootGrpId = request.getAttribute(AccountConstants.A_HAB_ROOT_GROUP_ID);
        Account acct = getAuthenticatedAccount(zsc);

        ZimbraLog.account.info("Request for HAB group:%s by: ", rootGrpId, acct);

        Map<String, HABGroup> groups = new HashMap<String, HABGroup>();
        List<HABGroup> childGrpList = new ArrayList<HABGroup>();
        LdapDistributionList group = (LdapDistributionList) prov
            .getGroupWithAllAttrs(Key.DistributionListBy.id, rootGrpId);
        Set<String> members = group.getAllMembersSet();

        HABGroup grp = new HABGroup();
        grp.setId(group.getId());
        grp.setName(group.getName());
        grp.setRootGroup(ZmBoolean.TRUE);
        groups.put(group.getMail(), grp);
        grp.setAttrs(Attr.fromMap(group.getAttrs()));

        for (String member : members) {
            HABGroup grpChild = new HABGroup();
            grpChild.setParentGroupId(group.getId());
            groups.put(member, grpChild);
            childGrpList.add(grpChild);
        }
        Collections.sort(childGrpList, new SortBySeniorityIndexName());
        grp.setChildGroups(childGrpList);

        List<LdapDistributionList> lists = prov
            .getAllHabGroups(prov.get(DomainBy.name, acct.getDomainName()), group.getDN());

        for (LdapDistributionList list : lists) {
            String key = list.getAttr(Provisioning.A_mail);
            HABGroup habGrp = groups.get(key);
            if (habGrp == null) {
                habGrp = new HABGroup();
            }
            if (habGrp.getRootGroup().compareTo(ZmBoolean.TRUE) == 0) {
                continue;
            }
            habGrp.setId(list.getId());
            habGrp.setName(list.getName());
            habGrp.setAttrs(Attr.fromMap(list.getAttrs()));
            groups.put(key, habGrp);
            List<HABGroup> children = new ArrayList<HABGroup>();
            for (String member : list.getAllMembersSet()) {
                HABGroup grpChild = groups.get(member);
                if (grpChild == null) {
                    grpChild = new HABGroup();
                }
                grpChild.setParentGroupId(list.getId());
                groups.put(member, grpChild);
                children.add(grpChild);
            }
            Collections.sort(children, new SortBySeniorityIndexName());
            habGrp.setChildGroups(children);
        }

        Element response = zsc.createElement(AccountConstants.E_GET_HAB_RESPONSE);
        ToXML.encodeHabGroup(response, grp, null);

        return response;
    }

    class SortBySeniorityIndexName implements Comparator<HABGroup> {

        @Override
        public int compare(HABGroup a, HABGroup b) {
            if (a == null || b == null) {
                return 0;
            }

            int s1 = a.getSeniorityIndex();
            int s2 = b.getSeniorityIndex();

            if (s1 == 0 && s2 == 0) {
                String name1 = a.getName();
                String name2 = b.getName();

                return name1.compareTo(name2);

            } else {
                return new Integer(s2).compareTo(new Integer(s1));
            }

        }
    }
}
