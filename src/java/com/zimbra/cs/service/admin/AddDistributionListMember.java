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

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.AddDistributionListMemberResponse;

public class AddDistributionListMember extends ReloadMemberPostProxyHandler {

    /**
     * @return true - which means accept responsibility for measures to prevent account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Provisioning prov = Provisioning.getInstance();

        Group group = getGroupFromContext(context);
        String id = request.getAttribute(AdminConstants.E_ID);
        defendAgainstGroupHarvesting(group, DistributionListBy.id, id, zsc,
                Admin.R_addGroupMember, Admin.R_addDistributionListMember);

        List<String> memberList = getMemberList(request, context);
        if (memberList.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("members to add not specified", null);
        }

        String[] members = memberList.toArray(new String[0]);
        prov.addGroupMembers(group, members);
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "AddDistributionListMember","name", group.getName(),
                    "members", Arrays.deepToString(members)}));

        // send share notification email
        if (group.isDynamic()) {
            // do nothing for now
        } else {
            boolean sendShareInfoMsg =
                group.getBooleanAttr(Provisioning.A_zimbraDistributionListSendShareMessageToNewMembers, true);
            if (sendShareInfoMsg) {
                ShareInfo.NotificationSender.sendShareInfoMessage(octxt, (DistributionList) group, members);
            }
        }

        return zsc.jaxbToElement(new AddDistributionListMemberResponse());
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_addDistributionListMember);
        relatedRights.add(Admin.R_addGroupMember);
    }
}
