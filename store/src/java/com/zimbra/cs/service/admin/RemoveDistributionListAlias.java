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

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.RemoveDistributionListAliasRequest;
import com.zimbra.soap.admin.message.RemoveDistributionListAliasResponse;

public class RemoveDistributionListAlias extends DistributionListDocumentHandler {

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
        String id = request.getAttribute(AdminConstants.E_ID, null);
        if (id != null) {
            return Provisioning.getInstance().getGroup(DistributionListBy.id, id);
        } else {
            return null;
        }
    }

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        RemoveDistributionListAliasRequest req = zsc.elementToJaxb(request);

        String id = req.getId();
        String alias = req.getAlias();
        Group group = getGroupFromContext(context);

        String dlName = "";
        try {
            defendAgainstGroupHarvesting(group, DistributionListBy.id, id, zsc,
                    Admin.R_removeGroupAlias, Admin.R_removeDistributionListAlias);
        } catch (AccountServiceException ase) {
            // still may want to remove the alias, even if it doesn't point at anything
            // note: if we got a permission denied instead of AccountServiceException,
            //       means we don't have the rights so shouldn't get any further
        }

        if (group != null) {
            dlName = group.getName();
        }

        // if the admin can remove an alias in the domain
        checkDomainRightByEmail(zsc, alias, Admin.R_deleteAlias);

        // even if dl is null, we still invoke removeAlias and throw an exception afterwards.
        // this is so dangling aliases can be cleaned up as much as possible
        prov.removeGroupAlias(group, alias);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "RemoveDistributionListAlias", "name", dlName, "alias", alias}));

        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(id);
        }

        return zsc.jaxbToElement(new RemoveDistributionListAliasResponse());
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_removeDistributionListAlias);
        relatedRights.add(Admin.R_removeGroupAlias);
        relatedRights.add(Admin.R_deleteAlias);
    }
}
