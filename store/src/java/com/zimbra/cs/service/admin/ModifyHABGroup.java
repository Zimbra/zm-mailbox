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
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.ldap.entry.LdapDistributionList;
import com.zimbra.cs.account.ldap.entry.LdapDynamicGroup;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author zimbra
 *
 */
public class ModifyHABGroup extends AdminDocumentHandler {

    private final static String CN = Provisioning.A_cn + "=";
    private final static String OU = Provisioning.A_ou + "=";
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element,
     * java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element operation = request.getElement(AdminConstants.E_HAB_GROUP_OPERATION);
        if (operation == null) {
           throw ServiceException.INVALID_REQUEST(
                    String.format("HAB operation is a required element"), null);
        }
        String operationType = operation.getAttribute(AdminConstants.A_OPERATION);
        if (StringUtil.isNullOrEmpty(operationType)) {
            throw ServiceException.INVALID_REQUEST(
                String.format("Operation is required, requested op:%s", operation), null);
        }
        Element response = zsc.createElement(AdminConstants.MODIFY_HAB_GROUP_RESPONSE);
        if (operationType.equals("move")) {
            handleHABGroupMove(prov, operation, response);
        } else {
            throw ServiceException.INVALID_REQUEST(
                String.format("Only move HAB group operation is supported"), null);
        }

        return response;
    }

    /**
     * @param prov
     * @param operation
     * @param response
     * @throws ServiceException
     */
    public void handleHABGroupMove(Provisioning prov, Element operation, Element response)
        throws ServiceException {
        String habGroupId = operation.getAttribute(AdminConstants.A_HAB_GROUP_ID);
        if (StringUtil.isNullOrEmpty(habGroupId)) {
            throw ServiceException.INVALID_REQUEST(
                String.format("HABGroupId is required attribute"), null);
        }
        Group group = prov.getGroup(Key.DistributionListBy.id, habGroupId, true, false);

        String habCurrentParentId = operation.getAttribute(AdminConstants.A_CURRENT_PARENT_HAB_GROUP_ID, null);
        boolean moveRoot = false;
        if (StringUtil.isNullOrEmpty(habCurrentParentId)) {
            GroupMembership membership = prov.getGroupMembership(group, false);
            List<String> ids = membership.groupIds();
            if (ids.size() != 0) {
                throw ServiceException.INVALID_REQUEST(
                    String.format("Current HAB parent id is required"), null);
            }
            moveRoot = true;
        }
        
        String habTargetParentId = operation.getAttribute(AdminConstants.A_TARGET_PARENT_HAB_GROUP_ID);
        if (StringUtil.isNullOrEmpty(habTargetParentId)) {
            throw ServiceException.INVALID_REQUEST(
                String.format("Target HAB parent Id is required"), null);
        }
        
        ZimbraLog.misc.debug("Modify HAB req for: %s, from parent: %s to new parent:%s", habGroupId,
            habCurrentParentId, habTargetParentId);

        Group targetGroup = prov.getGroup(Key.DistributionListBy.id, habTargetParentId, true,
            false);
        Group currentGroup = null;
        if (habCurrentParentId != null) {
            currentGroup = prov.getGroup(Key.DistributionListBy.id, habCurrentParentId, true,
                false);
        }

        if (null == group || (null == currentGroup && !moveRoot) || null == targetGroup) {
            throw ServiceException.INVALID_REQUEST(String.format("Invalid groupId provided."),
                null);
        }
        checkGroupsInMoveReqAreValid(targetGroup, group, currentGroup, moveRoot, prov);
        
        LdapDistributionList currentList = null;
        if (currentGroup != null) {
            currentList = (LdapDistributionList) currentGroup;
        }

        String cn = null;
        String groupDn = null;
        if (group instanceof LdapDistributionList) {
            cn = ((LdapDistributionList) group).getCn();
            groupDn = ((LdapDistributionList) group).getDN();
        } else if (group instanceof LdapDynamicGroup) {
            groupDn = ((LdapDynamicGroup) group).getDN();
        }

        LdapDistributionList targetList = (LdapDistributionList) targetGroup;
        String targetDn = targetList.getDN();

        if (!isGroupMovedtoSameOu(groupDn, targetDn)) {
            throw ServiceException.INVALID_REQUEST(String.format("Target group is in a different ou:%s", targetDn),
                null);
        }

        String[] members = { group.getMail() };
        if (null != currentList) {
            ((DistributionList) (currentList)).removeMembers(members);
        }
        ((DistributionList) (targetList)).addMembers(members);

        Element parent = response.addElement(AdminConstants.E_HAB_PARENT_GROUP);
        parent.addAttribute(AdminConstants.A_ID, targetGroup.getId());
        Element membersEl = parent.addElement(AdminConstants.E_MEMBERS);
        Set<String> membersSet = targetGroup.getAllMembersSet();
        for (String s : membersSet) {
            membersEl.addElement(AdminConstants.E_MEMBER).addText(s);
        }
    }

    /**
     * 
     * @param groupDn
     * @param targetDn
     * @return true if groupDn and targetDn have same ou
     */
    public static boolean isGroupMovedtoSameOu(String groupDn, String targetDn) {
        String groupOu = null;
        String targetOu = null;
        try {
            LdapName grpDnObj = new LdapName(groupDn);
            LdapName targetParentDnObj = new LdapName(targetDn);
            for (Rdn rdn : grpDnObj.getRdns()) {
                if (rdn.getType().equals("ou")) {
                    groupOu = rdn.getValue().toString();
                    break;
                }
            }

            for (Rdn rdn : targetParentDnObj.getRdns()) {
                if (rdn.getType().equals("ou")) {
                    targetOu = rdn.getValue().toString();
                    break;
                }
            }

        } catch (InvalidNameException e) {
            return false;
        }

        if (!StringUtil.isNullOrEmpty(groupOu) && !StringUtil.isNullOrEmpty(targetOu)
            && groupOu.equalsIgnoreCase(targetOu)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 
     * @param targetGroup
     * @param group
     * @param currentGroup
     * @param moveRoot
     * @param prov
     * @throws ServiceException
     */
    private void checkGroupsInMoveReqAreValid(Group targetGroup, Group group, Group currentGroup, boolean moveRoot, Provisioning prov) throws ServiceException{
        if (!group.isHABGroup() ||  !targetGroup.isHABGroup()) {
            throw ServiceException
                .INVALID_REQUEST(String.format("Operation only supported for HAB group"), null);
        }
        if (!moveRoot && !currentGroup.isHABGroup()) {
            throw ServiceException
            .INVALID_REQUEST(String.format("Operation only supported for HAB group"), null);
        }
        if (targetGroup.isDynamic()) {
            throw ServiceException.INVALID_REQUEST(String.format("Target group cannot be dynamic."),
                null);
        }
        
        if (currentGroup != null && currentGroup.isDynamic()) {
            throw ServiceException.INVALID_REQUEST(String.format("Current group cannot be dynamic."),
                null);
        }
        
        if (!currentGroup.getDomainId().equalsIgnoreCase(targetGroup.getDomainId())) {
            throw ServiceException.INVALID_REQUEST(String.format("Current group and target group domain should be same."),
                null);
        }
        
        if (group.isDynamic()) {
            List<String> dlsToCheck = new ArrayList<String>();
            dlsToCheck.add(targetGroup.getMail());
           if (prov.dlIsInDynamicHABGroup((DynamicGroup)group, dlsToCheck)) {
               throw ServiceException.INVALID_REQUEST
               (String.format("Target group:%s is a member of the group:%s being moved", targetGroup.getName(), group.getName()), null);
           }
        } else {
            Set<String> members = group.getAllMembersSet();
            for (String member : members) {
                if (member.equalsIgnoreCase(targetGroup.getMail())) {
                    throw ServiceException.INVALID_REQUEST(String.format("Group to be moved is a member of the target group"),
                        null);
                }
            }
        }
    }


}
