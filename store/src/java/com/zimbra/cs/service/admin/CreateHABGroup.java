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

import org.apache.commons.lang.StringUtils;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.CreateHABGroupRequest;

public class CreateHABGroup extends CreateDistributionList {

    @Override
    protected void preGroupCreation(Element request, ZimbraSoapContext zsc, Map<String, Object> attrs)
            throws ServiceException {
        CreateHABGroupRequest req = zsc.elementToJaxb(request);
        String orgUnitRDN = req.getHabOrgUnit();
        if (StringUtils.isEmpty(orgUnitRDN)) {
            throw ServiceException.INVALID_REQUEST(String.format("missing %s", AdminConstants.A_HAB_ORG_UNIT), null);
        }

        boolean dynamic = Boolean.TRUE.equals(req.getDynamic());
        if (dynamic) {
            String memberURL = attrs.get(Provisioning.A_memberURL) != null ? (String) attrs.get(Provisioning.A_memberURL) : null;
            if (StringUtils.isEmpty(memberURL)) {
                throw ServiceException.INVALID_REQUEST(String.format("missing attribute: %s", Provisioning.A_memberURL), null);
            }
            attrs.put(Provisioning.A_zimbraIsACLGroup, "FALSE");
        }

        attrs.put(AdminConstants.A_HAB_ORG_UNIT, orgUnitRDN);
        attrs.put(AdminConstants.A_HAB_DISPLAY_NAME, req.getHabDisplayName());
    }

    @Override
    protected Element getResponseElement(ZimbraSoapContext zsc) {
        return zsc.createElement(AdminConstants.CREATE_HAB_GROUP_RESPONSE);
    }

}
