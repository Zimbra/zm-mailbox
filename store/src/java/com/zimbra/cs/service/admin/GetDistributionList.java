/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Group.GroupOwner;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetDistributionListRequest;
import com.zimbra.soap.admin.type.DistributionListSelector;

public class GetDistributionList extends DistributionListDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    @Override
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    /**
     * @return true - which means accept responsibility for measures to prevent account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    @Override
    protected Group getGroup(Element request) throws ServiceException {
        Element eDL = request.getElement(AdminConstants.E_DL);
        String key = eDL.getAttribute(AdminConstants.A_BY);
        String value = eDL.getText();

        return Provisioning.getInstance().getGroup(Key.DistributionListBy.fromString(key), value);
    }

    private static String[] minimumAttrs = {Provisioning.A_zimbraId, Provisioning.A_zimbraMailAlias};

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        GetDistributionListRequest req = zsc.elementToJaxb(request);

        int limit = (req.getLimit() == null) ? 0 : req.getLimit();
        if (limit < 0) {
            throw ServiceException.INVALID_REQUEST("limit" + limit + " is negative", null);
        }
        int offset = (req.getOffset() == null) ? 0 : req.getOffset();
        if (offset < 0) {
            throw ServiceException.INVALID_REQUEST("offset" + offset + " is negative", null);
        }
        boolean sortAscending = !Boolean.FALSE.equals(req.isSortAscending());
        Set<String> reqAttrs = getReqAttrs(req.getAttrs(), AttributeClass.distributionList);
        DistributionListSelector dlSel = req.getDl();
        DistributionListBy dlBy = dlSel.getBy().toKeyDistributionListBy();
        AttrRightChecker arc = null;
        Group group = getGroupFromContext(context);
        if (group == null) {
            if (DistributionListBy.name.equals(dlBy)) {
                Entry pseudoTarget = pseudoTargetInSameDomainAsEmail(TargetType.dl, dlSel.getKey());
                if (null != pseudoTarget) {
                    AdminAccessControl aac = checkDistributionListRight(zsc,
                            (DistributionList) pseudoTarget, AdminRight.PR_ALWAYS_ALLOW);
                    arc = aac.getAttrRightChecker(pseudoTarget);
                }
            }
            if (arc != null) {
                defendAgainstGroupHarvestingWhenAbsent(dlBy, dlSel.getKey(), zsc,
                        new GroupHarvestingCheckerUsingGetAttrsPerms(zsc, arc, Arrays.asList(minimumAttrs)));
            } else {
                defendAgainstGroupHarvestingWhenAbsent(dlBy, dlSel.getKey(), zsc, Admin.R_getDistributionList);
            }
        } else if (group.isDynamic()) {
            AdminAccessControl aac = checkDynamicGroupRight(zsc, (DynamicGroup) group, AdminRight.PR_ALWAYS_ALLOW);
            arc = aac.getAttrRightChecker(group);
        } else {
            AdminAccessControl aac = checkDistributionListRight(zsc,
                    (DistributionList) group, AdminRight.PR_ALWAYS_ALLOW);
            arc = aac.getAttrRightChecker(group);
        }
        defendAgainstGroupHarvesting(group, dlBy, dlSel.getKey(), zsc,
                        new GroupHarvestingCheckerUsingGetAttrsPerms(zsc, arc, Arrays.asList(minimumAttrs)));

        Element response = zsc.createElement(AdminConstants.GET_DISTRIBUTION_LIST_RESPONSE);
        Element eDL = encodeDistributionList(response, group, true, false, reqAttrs, arc);

        // return member info only if the authed has right to see zimbraMailForwardingAddress
        boolean allowMembers = true;
        if (group.isDynamic()) {
            allowMembers = arc == null ? true : arc.allowAttr(Provisioning.A_member);
        } else {
            allowMembers = arc == null ? true : arc.allowAttr(Provisioning.A_zimbraMailForwardingAddress);
        }

        if (allowMembers) {
            encodeMembers(response, eDL, group, offset, limit, sortAscending);
        }
        return response;
    }

    private void encodeMembers(Element response, Element dlElement, Group group,
            int offset, int limit, boolean sortAscending) throws ServiceException {
        String[] members;
        if (group instanceof DynamicGroup) {
            members = ((DynamicGroup)group).getAllMembers(true);
        } else {
            members = group.getAllMembers();
        }

        if (offset > 0 && offset >= members.length) {
            throw ServiceException.INVALID_REQUEST("offset " + offset +
                    " greater than size " + members.length, null);
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

    public static Element encodeDistributionList(Element e, Group group)
            throws ServiceException {
        return encodeDistributionList(e, group, true, false, null, null);
    }

    public static Element encodeDistributionList(Element e, Group group,
            boolean hideMembers, boolean hideOwners, Set<String> reqAttrs,
            AttrRightChecker attrRightChecker) throws ServiceException {
        return encodeDistributionList(e, group, hideMembers, hideOwners, true,
                reqAttrs, attrRightChecker);
    }

    public static Element encodeDistributionList(Element e, Group group,
            boolean hideMembers, boolean hideOwners, boolean encodeAttrs,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker)
                    throws ServiceException {
        Element eDL = e.addElement(AdminConstants.E_DL);
        eDL.addAttribute(AdminConstants.A_NAME, group.getUnicodeName());
        eDL.addAttribute(AdminConstants.A_ID,group.getId());
        eDL.addAttribute(AdminConstants.A_DYNAMIC, group.isDynamic());

        if (encodeAttrs) {
            Set<String> hideAttrs = null;
            if (hideMembers) {
                hideAttrs = new HashSet<String>();
                if (group.isDynamic()) {
                    hideAttrs.add(Provisioning.A_member);
                } else {
                    hideAttrs.add(Provisioning.A_zimbraMailForwardingAddress);
                }
            }

            ToXML.encodeAttrs(eDL, group.getUnicodeAttrs(),
                    AdminConstants.A_N, reqAttrs, hideAttrs, attrRightChecker);
        }

        if (!hideOwners) {
            encodeOwners(eDL, group);
        }

        return eDL;
    }

    public static Element encodeOwners(Element eParent, Group group) throws ServiceException {
        Element eOwners = null;

        List<GroupOwner> owners = GroupOwner.getOwners(group, true);
        if (!owners.isEmpty()) {
            eOwners = eParent.addElement(AdminConstants.E_DL_OWNERS);

            for (GroupOwner owner : owners) {
                Element eOwner = eOwners.addElement(AdminConstants.E_DL_OWNER);

                eOwner.addAttribute(AdminConstants.A_TYPE, owner.getType().getCode());
                eOwner.addAttribute(AdminConstants.A_ID, owner.getId());
                eOwner.addAttribute(AdminConstants.A_NAME, owner.getName());
            }
        }

        return eOwners;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getDistributionList);
        relatedRights.add(Admin.R_getGroup);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getDistributionList.getName()));
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getGroup.getName()));
    }

}
