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
package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginException;

import com.zimbra.common.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.krb5.Krb5Login;

class AutoProvisionLazy extends AutoProvision {
    private String loginName;
    private String loginPassword;
    private AutoProvAuthMech authedByMech;

    AutoProvisionLazy(LdapProv prov, Domain domain, String loginName, String loginPassword,
            AutoProvAuthMech authedByMech) {
        super(prov, domain);
        this.loginName = loginName;
        this.loginPassword = loginPassword;
        this.authedByMech = authedByMech;
    }

    @Override
    Account handle() throws ServiceException {
        if (domain == null) {
            domain = prov.getDefaultDomain();
            if (domain == null) {
                return null; 
            }
        }
       
        if (authedByMech == null) {
            // principal had not been authenticated, try to auth it
            authedByMech = auth();
        }
        
        if (authedByMech == null) {
            // principal cannot be authenticated by the auth mechanism 
            // configured on the domain
            return null;
        }
        
        if (!autoProvisionEnabled()) {
            return null;
        }
        
        return createAccount();
    }
    
    private boolean autoProvisionEnabled() {
        Set<String> authMechsEnabled = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvAuthMech);
        Set<String> modesEnabled = domain.getMultiAttrSet(Provisioning.A_zimbraAutoProvMode);
        return authMechsEnabled.contains(authedByMech.name()) && modesEnabled.contains(AutoProvMode.LAZY.name());
    }
    
    private Account createAccount() throws ServiceException {
        ExternalEntry externalEntry = getExternalAttrsByName(loginName);
        String acctZimbraName = mapName(externalEntry.getAttrs(), loginName);
        
        ZimbraLog.autoprov.info("auto creating account in LAZY mode: " + acctZimbraName);
        return createAccount(acctZimbraName, externalEntry);
    }
    
    private AutoProvAuthMech auth() {
        String authMechStr = domain.getAttr(Provisioning.A_zimbraAuthMech);
        AuthMech authMech = null;
        
        try {
            authMech = AuthMech.fromString(authMechStr);
        } catch (ServiceException e) {
            ZimbraLog.autoprov.debug("invalid auth mech " + authMechStr, e);
        }
        
        // only support external LDAP auth for now
        if (AuthMech.ldap == authMech  || AuthMech.ad == authMech) {
            Map<String, Object> authCtxt = new HashMap<String, Object>();
            try {
                prov.externalLdapAuth(domain, authMech, loginName, loginPassword, authCtxt);
                return AutoProvAuthMech.LDAP;
            } catch (ServiceException e) {
                ZimbraLog.autoprov.info("unable to authenticate " + loginName + " for auto provisioning", e);
            }
        } else if (AuthMech.kerberos5 == authMech) {
            try {
                Krb5Login.verifyPassword(loginName, loginPassword);
                return AutoProvAuthMech.KRB5;
            } catch (LoginException e) {
                ZimbraLog.autoprov.info("unable to authenticate " + loginName + " for auto provisioning", e);
            }
        } else {
            // unsupported auth mechanism for lazy auto provision
            
            // Provisioning.AM_CUSTOM is not supported because the custom auth 
            // interface required a Zimrba Account instance.
        }
        
        return null;
    }

}