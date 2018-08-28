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

import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapDistributionList;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author zimbra
 *
 */
public class ModifyHABGroup extends AdminDocumentHandler{

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map)
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
                String.format("Operation is required, requested op:%s",operation), null);
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
        
        String habCurrentParentId = operation.getAttribute(AdminConstants.A_CURRENT_PARENT_HAB_GROUP_ID);
        if (StringUtil.isNullOrEmpty(habCurrentParentId)) {
            throw ServiceException.INVALID_REQUEST(
                String.format("Current HAB parent id is required"), null);
        }
        
        String habTargetParentId = operation.getAttribute(AdminConstants.A_TARGET_PARENT_HAB_GROUP_ID);
        if (StringUtil.isNullOrEmpty(habTargetParentId)) {
            throw ServiceException.INVALID_REQUEST(
                String.format("Target HAB parent Id is required"), null);
        }
        
        ZimbraLog.misc.debug("Modify HAB req for: %s, from parent: %s to new parent:%s", habGroupId,
            habCurrentParentId, habTargetParentId);
        
        LdapDistributionList group = (LdapDistributionList) prov
            .getGroup(Key.DistributionListBy.id, habGroupId, true, false);
        LdapDistributionList targetGroup = (LdapDistributionList) prov
            .getGroup(Key.DistributionListBy.id, habTargetParentId, true, false);
        LdapDistributionList currentGroup = (LdapDistributionList) prov
            .getGroup(Key.DistributionListBy.id, habCurrentParentId, true, false);
        
        
        if (!group.isHABGroup() || !currentGroup.isHABGroup() || !targetGroup.isHABGroup()) {
            throw ServiceException.INVALID_REQUEST(
                String.format("Operation only supported for HAB group"), null);
        }
        
        String groupDn = group.getDN();
        String parentDn = targetGroup.getDN();
        ZimbraLog.misc.debug("HAB group dn:%s", groupDn);
        ZimbraLog.misc.info("Target parent dn:%s",parentDn);
        
       
        String targetDn = "cn="+group.getCn() + "," + parentDn;
        prov.changeHABGroupParent(groupDn, targetDn);
        
        String [] members = {group.getMail()};
        ((DistributionList)(currentGroup)).removeMembers(members);
        ((DistributionList)(targetGroup)).addMembers(members);
        
      
        Element parent  = response.addElement(AdminConstants.E_HAB_PARENT_GROUP);
        parent.addAttribute(AdminConstants.A_ID, targetGroup.getId());
        Set<String> membersL = targetGroup.getAllMembersSet();
        for (String s: membersL) {
            parent.addElement(AdminConstants.E_MEMBER).addText(s);
        }
    }

}
