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

package com.zimbra.cs.account.grouphandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ExternalGroup;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.gal.ZimbraGalGroupHandler;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;

public abstract class GroupHandler {

    public abstract boolean isGroup(IAttributes ldapAttrs);

    public abstract String[] getMembers(ILdapContext ldapContext, String searchBase,
            String entryDN, IAttributes ldapAttrs) throws ServiceException;

    public abstract boolean inDelegatedAdminGroup(ExternalGroup group,
            Account acct, boolean asAdmin) throws ServiceException;

    private static Map<String, HandlerInfo> sHandlers =
        new ConcurrentHashMap<String,HandlerInfo>();

    private static class HandlerInfo {
        Class<? extends GroupHandler> mClass;

        public GroupHandler getInstance() {
            GroupHandler handler;
            try {
                handler = mClass.newInstance();
            } catch (InstantiationException e) {
                handler = newDefaultHandler();
            } catch (IllegalAccessException e) {
                handler = newDefaultHandler();
            }
            return handler;
        }
    }

    private static GroupHandler newDefaultHandler() {
        return new ZimbraGalGroupHandler();
    }

    private static HandlerInfo loadHandler(String className) {
        HandlerInfo handlerInfo = new HandlerInfo();

        try {
            handlerInfo.mClass = ExtensionUtil.findClass(className).asSubclass(GroupHandler.class);
        } catch (ClassNotFoundException e) {
            // miss configuration or the extension is disabled
            ZimbraLog.gal.warn("GAL group handler %s not found, default to ZimbraGalGroupHandler", className);
            // Fall back to ZimbraGalGroupHandler
            handlerInfo.mClass = ZimbraGalGroupHandler.class;
        }
        return handlerInfo;
    }

    public static GroupHandler getHandler(String className) {
        if (StringUtil.isNullOrEmpty(className)) {
            return newDefaultHandler();
        }

        HandlerInfo handlerInfo = sHandlers.get(className);

        if (handlerInfo == null) {
            handlerInfo = loadHandler(className);
            sHandlers.put(className, handlerInfo);
        }

        return handlerInfo.getInstance();
    }

    /*
     * callsite is responsible for closing the context after done.
     *
     * External group for delegated admin uses the external AD auth
     * settings.  The diff is, when looking for the account anywhere
     * other than authenticating the account, we have to use the
     * admin bindDN/password, because:
     *   - we no longer have the user's external LDAP password
     *   - it makes sense to do this task using the admin's credentials.
     */
    public ZLdapContext getExternalDelegatedAdminGroupsLdapContext(Domain domain, boolean asAdmin)
    throws ServiceException {
        String[] ldapUrl = domain.getAuthLdapURL();
        if (ldapUrl == null || ldapUrl.length == 0) {
            throw ServiceException.INVALID_REQUEST("ubable to search external group, " +
                    "missing " + Provisioning.A_zimbraAuthLdapURL, null);
        }

        boolean startTLSEnabled = domain.isAuthLdapStartTlsEnabled();
        String bindDN = domain.getAuthLdapSearchBindDn();
        String bindPassword = domain.getAuthLdapSearchBindPassword();

        ExternalLdapConfig ldapConfig = new ExternalLdapConfig(ldapUrl, startTLSEnabled,
                null, bindDN, bindPassword, null,
                "search external group");

        return LdapClient.getExternalContext(ldapConfig, LdapUsage.EXTERNAL_GROUP);
    }

}
