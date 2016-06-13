/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.gal;

import java.util.Arrays;
import java.util.List;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ExternalGroup;
import com.zimbra.cs.account.grouphandler.GroupHandler;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.IAttributes.CheckBinary;

public class ZimbraGalGroupHandler extends GroupHandler {

    @Override
    public boolean isGroup(IAttributes ldapAttrs) {
        try {
            List<String> objectclass = ldapAttrs.getMultiAttrStringAsList(
                    Provisioning.A_objectClass, IAttributes.CheckBinary.NOCHECK);
            return objectclass.contains(AttributeClass.OC_zimbraDistributionList) ||
                   objectclass.contains(AttributeClass.OC_zimbraGroup);
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("unable to get attribute " + Provisioning.A_objectClass, e);
        }
        return false;
    }

    @Override
    public String[] getMembers(ILdapContext ldapContext, String searchBase,
            String entryDN, IAttributes ldapAttrs) throws ServiceException {
        ZimbraLog.gal.debug("Fetching members for group " + ldapAttrs.getAttrString(Provisioning.A_mail));
        List<String> objectclass =
            ldapAttrs.getMultiAttrStringAsList(Provisioning.A_objectClass, CheckBinary.NOCHECK);

        String[] members = null;
        if (objectclass.contains(AttributeClass.OC_zimbraDistributionList)) {
            members = ldapAttrs.getMultiAttrString(Provisioning.A_zimbraMailForwardingAddress);
        } else if (objectclass.contains(AttributeClass.OC_zimbraGroup)) {
            String zimbraId = ldapAttrs.getAttrString(Provisioning.A_zimbraId);
            Provisioning prov = Provisioning.getInstance();
            Group group = prov.getGroupBasic(DistributionListBy.id, zimbraId);
            if (group == null) {
                throw AccountServiceException.NO_SUCH_GROUP(zimbraId);
            }
            members = prov.getGroupMembers(group);
        }

        Arrays.sort(members);
        return members;
    }

    @Override
    public boolean inDelegatedAdminGroup(ExternalGroup group, Account acct, boolean asAdmin)
    throws ServiceException {
        // this method is used for checking external group membership for checking
        // delegated admin rights.  Internal group grantees do not go through
        // this path.
        throw ServiceException.FAILURE("internal error", null);
    }
}
