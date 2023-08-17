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
import com.zimbra.cs.db.Zimbra10MailItemLocatorUpgradeExec;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.AddAccountAliasResponse;
import com.zimbra.soap.admin.message.AddDistributionListMemberResponse;
import com.zimbra.soap.admin.message.Zimbra10LocatorUpgradeRequest;
import com.zimbra.soap.admin.message.Zimbra10LocatorUpgradeResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * handler class for recieving request from porter for zimbra10 locator upgrade
 */
public class Zimbra10DBLocatorUpgrade extends AdminDocumentHandler {
    /**
     * handles the request for locator update
     *
     * @param request
     * @param context
     * @return
     * @throws ServiceException
     */
    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Zimbra10LocatorUpgradeRequest req = zsc.elementToJaxb(request);
        Zimbra10MailItemLocatorUpgradeExec locatorUpgradeExec = new Zimbra10MailItemLocatorUpgradeExec();
        return zsc.jaxbToElement(new Zimbra10LocatorUpgradeResponse(locatorUpgradeExec.upgrade(req)));
    }

    @Override public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_addDistributionListMember);
        relatedRights.add(Admin.R_addGroupMember);
    }
}
