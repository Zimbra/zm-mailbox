/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.EmailUtil;

public class DomainAccessManager extends AccessManager {

    public boolean isDomainAdminOnly(AuthToken at) {
        return at.isDomainAdmin() && !at.isAdmin();
    }

    public boolean canAccessAccount(AuthToken at, Account target) throws ServiceException {
        if (at.isAdmin()) return true;
        if (!at.isDomainAdmin()) return false;
        // don't allow a domain-only admin to access a global admin's account
        if (target.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false)) return false;
        return getDomain(at).getId().equals(target.getDomain().getId());
    }

    public boolean canAccessDomain(AuthToken at, String domainName) throws ServiceException {
        if (at.isAdmin()) return true;
        if (!at.isDomainAdmin()) return false;
        return getDomain(at).getName().equalsIgnoreCase(domainName);
    }

    public boolean canAccessDomain(AuthToken at, Domain domain) throws ServiceException {
        return canAccessDomain(at, domain.getName());
    }

    public boolean canAccessEmail(AuthToken at, String email) throws ServiceException {
        String parts[] = EmailUtil.getLocalPartAndDomain(email);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+email, null);
        return canAccessDomain(at, parts[1]);
    }
}
