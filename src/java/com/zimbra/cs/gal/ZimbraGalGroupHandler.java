/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.gal;

import java.util.Arrays;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ExternalGroup;
import com.zimbra.cs.account.grouphandler.GroupHandler;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.ILdapContext;

public class ZimbraGalGroupHandler extends GroupHandler {

    private static String[] sEmptyMembers = new String[0];
    
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
            String entryDN, IAttributes ldapAttrs) {
        try {
            ZimbraLog.gal.debug("Fetching members for group " + ldapAttrs.getAttrString(Provisioning.A_mail));
            String[] members = ldapAttrs.getMultiAttrString(Provisioning.A_zimbraMailForwardingAddress);
            Arrays.sort(members);
            return members;
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("unable to retrieve group members ", e);
            return sEmptyMembers;
        }
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
