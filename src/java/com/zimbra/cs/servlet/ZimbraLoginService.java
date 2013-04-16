/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 VMware, Inc.
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

package com.zimbra.cs.servlet;

import java.security.Principal;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.MappedLoginService.KnownUser;
import org.eclipse.jetty.security.MappedLoginService.RolePrincipal;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;

/**
 * Jetty login service which handles HTTP BASIC authentication requests via Zimbra Provisioning
 *
 */
public class ZimbraLoginService implements LoginService {

    protected IdentityService identityService = new DefaultIdentityService();
    protected String name;

    @Override
    public void setIdentityService(IdentityService idService) {
        identityService = idService;
    }

    @Override
    public IdentityService getIdentityService() {
        return identityService;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void logout(UserIdentity user) {
    }

    @Override
    public boolean validate(UserIdentity user) {
        return false;
    }

    @Override
    public UserIdentity login(String username, Object credentials) {
        Account account;
        try {
            Provisioning prov = Provisioning.getInstance();
            account = prov.get(AccountBy.name, username);

            if (account != null) {
                prov.authAccount(account, (String) credentials, AuthContext.Protocol.http_basic);
                return makeUserIdentity(username);
            }
        } catch (AuthFailedServiceException e) {
            ZimbraLog.security.debug("Auth failed");
        } catch (ServiceException e) {
            ZimbraLog.security.warn("ServiceException in auth", e);
        }
        return null;
    }

    UserIdentity makeUserIdentity(String userName) {
        //blank password/credentials. this is just a placeholder; we always check credentials via prov on each login
        Credential credential = Credential.getCredential("");
        //only need 'user' role for current implementation protecting /zimbra/downloads - expand to admin if needed later
        String roleName = "user";
        Principal userPrincipal = new KnownUser(userName, credential);
        Subject subject = new Subject();
        subject.getPrincipals().add(userPrincipal);
        subject.getPrivateCredentials().add(credential);
        subject.getPrincipals().add(new RolePrincipal(roleName));
        subject.setReadOnly();

        UserIdentity identity = identityService.newUserIdentity(subject, userPrincipal, new String[]{roleName});
        return identity;
    }
}
