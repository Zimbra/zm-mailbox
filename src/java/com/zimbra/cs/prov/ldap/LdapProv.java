/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.prov.ldap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.prov.ldap.LdapHelper;

public abstract class LdapProv extends Provisioning {
    
    protected LdapDIT mDIT;
    protected LdapHelper helper;
    
    protected static final long ONE_DAY_IN_MILLIS = 1000*60*60*24;
    
    protected static final String[] sInvalidAccountCreateModifyAttrs = {
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraMailDeliveryAddress,
            Provisioning.A_uid
    };

    protected static final String[] sMinimalDlAttrs = {
            Provisioning.A_displayName,
            Provisioning.A_zimbraShareInfo,
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraId,
            Provisioning.A_uid,
            Provisioning.A_zimbraACE,
            Provisioning.A_zimbraIsAdminGroup,
            Provisioning.A_zimbraAdminConsoleUIComponents
    };
    
    public static LdapProv getInst() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        if (prov instanceof LdapProv) {
            return (LdapProv) prov;
        } else {
            throw ServiceException.FAILURE("not an instance of LdapProv", null);
        }
    }
    
    protected void setDIT() {
        mDIT = new LdapDIT(this);
    }

    public LdapDIT getDIT() {
        return mDIT;
    }
    
    protected void setHelper(LdapHelper helper) {
        this.helper = helper;
    }
    
    public LdapHelper getHelper() {
        return helper;
    }
    
    public abstract void searchOCsForSuperClasses(Map<String, Set<String>> ocs);
    
    public abstract List<MimeTypeInfo> getAllMimeTypesByQuery() throws ServiceException;
    public abstract List<MimeTypeInfo> getMimeTypesByQuery(String mimeType) throws ServiceException;
    
    public abstract void externalLdapAuth(Domain d, String authMech, Account acct, String password, 
            Map<String, Object> authCtxt) throws ServiceException;

}
