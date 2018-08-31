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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapDistributionList;
import com.zimbra.cs.account.ldap.entry.LdapDynamicGroup;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.service.util.SortBySeniorityIndexThenName;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.HABGroup;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @author zimbra
 *
 */
public class GetHAB extends AccountDocumentHandler {

    private final static String OU = Provisioning.A_ou + "=";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        String rootGrpId = request.getAttribute(AccountConstants.A_HAB_ROOT_GROUP_ID);
        Account acct = getAuthenticatedAccount(zsc);

        ZimbraLog.account.info("Request for HAB group:%s by: ", rootGrpId, acct.getName());

        Map<String, HABGroup> groups = new HashMap<String, HABGroup>();
        List<HABGroup> childGrpList = new ArrayList<HABGroup>();
        LdapDistributionList group = (LdapDistributionList) prov
            .getGroup(Key.DistributionListBy.id, rootGrpId, true, false);
        if (group == null || !group.isHABGroup()) {
            throw ServiceException.INVALID_REQUEST(
                String.format("HABGroupId:%s is invalid or is not an HAB group", rootGrpId), null);
        }
        Set<String> members = group.getAllMembersSet();

        HABGroup grp = new HABGroup();
        grp.setId(group.getId());
        grp.setName(group.getName());
        grp.setRootGroup(ZmBoolean.TRUE);
        grp.setSeniorityIndex(group.getIntAttr(Provisioning.A_zimbraHABSeniorityIndex, 0));
        groups.put(group.getMail(), grp);
        grp.setAttrs(Attr.fromMap(group.getAttrs()));

        for (String member : members) {
            HABGroup grpChild = new HABGroup();
            grpChild.setParentGroupId(group.getId());
            grpChild.setName(member);
            groups.put(member, grpChild);
            childGrpList.add(grpChild);
        }
        
        grp.setChildGroups(childGrpList);
        ZimbraLog.account.debug("The root group :%s has children:%s", grp.getName(), childGrpList);

        String rootDn = group.getDN();
        int ouIndex = rootDn.indexOf(OU);
        rootDn = rootDn.substring(ouIndex);
        
        ZimbraLog.misc.info("Searching under dn:%s", rootDn);
        Domain domain = group.getDomain();
        List<LdapDistributionList> lists = prov.getAllHabGroups(domain, rootDn);

        for (LdapEntry entry : lists) {
            String id = null;
            String name = null;
            Map<String, Object> attrs = null;
            int seniorityIndex = 0;
            String key = null;
            Set<String> memberSet = null;
            if (entry instanceof LdapDistributionList) {
                LdapDistributionList list = (LdapDistributionList)entry;
                id = list.getId();
                name = list.getName();
                attrs = list.getAttrs();
                memberSet = list.getAllMembersSet();
                key = list.getAttr(Provisioning.A_mail);
                seniorityIndex = list.getIntAttr(Provisioning.A_zimbraHABSeniorityIndex, 0);
            } else if (entry instanceof LdapDynamicGroup) {
                LdapDynamicGroup dyGrp = (LdapDynamicGroup)entry;
                id = dyGrp.getId();
                name = dyGrp.getName();
                memberSet= new HashSet<String>();
                attrs = dyGrp.getAttrs();
                key = dyGrp.getAttr(Provisioning.A_mail);
                seniorityIndex = dyGrp.getIntAttr(Provisioning.A_zimbraHABSeniorityIndex, 0);
            }
            
            HABGroup habGrp = groups.get(key);
            if (habGrp == null) {
                habGrp = new HABGroup();
            }
            if (habGrp.getRootGroup().compareTo(ZmBoolean.TRUE) == 0) {
                continue;
            }
            habGrp.setId(id);
            habGrp.setName(name);
            habGrp.setAttrs(Attr.fromMap(attrs));
            habGrp.setSeniorityIndex(seniorityIndex);
            groups.put(key, habGrp);
            List<HABGroup> children = new ArrayList<HABGroup>();
            
            ZimbraLog.account.debug("The HAB group: %s has children:%s", name, memberSet);
            for (String member : memberSet) {
                HABGroup grpChild = groups.get(member);
                if (grpChild == null) {
                    grpChild = new HABGroup();
                    grpChild.setName(member);
                }
                grpChild.setParentGroupId(id);
                groups.put(member, grpChild);
                children.add(grpChild);
            }
            habGrp.setChildGroups(children);
        }
        sortChildren(grp);
        Element response = zsc.createElement(AccountConstants.GET_HAB_RESPONSE);
        Element ou = zsc.createElement(Provisioning.A_ou);
        response.addElement(ou);
        String ouName = extractOuName(rootDn);
        if (ouName == null) {
            throw ServiceException.NOT_FOUND("ou name cannot be found, cannot generate a response");
        }
        ou.addAttribute(AccountConstants.A_NAME, ouName);
        ToXML.encodeHabGroup(ou, grp, null);

        return response;
    }

    private void sortChildren(HABGroup grp) {
        List<HABGroup> children = grp.getChildGroups();
        Collections.sort(children, new SortBySeniorityIndexThenName());
        for (HABGroup grpChild: children) {
            sortChildren(grpChild);
        }
    }
    /**
     * @param rootDn
     * @return the name of ou
     */
    private String extractOuName(String rootDn) {
        if (rootDn.contains(OU)) {
            int index = rootDn.indexOf(OU);
            int endIndex = rootDn.indexOf(",", index + OU.length());
            return rootDn.substring(index + OU.length(), endIndex);
        } 
        return null;
    }
 

}
