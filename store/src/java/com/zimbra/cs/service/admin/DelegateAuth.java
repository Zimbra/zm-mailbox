/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.DelayedIndexStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.index.MailboxIndexUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class DelegateAuth extends AdminDocumentHandler {

    // default is one hour
    private static final long DEFAULT_AUTH_LIFETIME = 60*60*1;

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";

    @Override
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element a = request.getElement(AdminConstants.E_ACCOUNT);
        String key = a.getAttribute(AdminConstants.A_BY);
        String value = a.getText();

        long lifetime = request.getAttributeLong(AdminConstants.A_DURATION, DEFAULT_AUTH_LIFETIME) * 1000;

        Provisioning prov = Provisioning.getInstance();

        Account account = null;

        if (key.equals(BY_NAME)) {
            account = prov.get(AccountBy.name, value, zsc.getAuthToken());
        } else if (key.equals(BY_ID)) {
            account = prov.get(AccountBy.id, value, zsc.getAuthToken());
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

        checkAdminLoginAsRight(zsc, prov, account);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "DelegateAuth","accountId", account.getId(),"accountName", account.getName()}));

        Element response = zsc.createElement(AdminConstants.DELEGATE_AUTH_RESPONSE);
        long maxLifetime = account.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000);

        // take the min of requested lifetime vs maxLifetime
        long expires = System.currentTimeMillis()+ Math.min(lifetime, maxLifetime);
        String token;
        Account adminAcct = prov.get(AccountBy.id, zsc.getAuthtokenAccountId(), zsc.getAuthToken());
        if (adminAcct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(zsc.getAuthtokenAccountId());

        AuthToken at = AuthProvider.getAuthToken(account, expires, false, adminAcct);
        at.encodeAuthResp(response, true);
        response.addAttribute(AdminConstants.E_LIFETIME, lifetime, Element.Disposition.CONTENT);
        if (MailboxIndexUtil.isUserAgentAllowedForChangingIndexStatus(zsc.getUserAgent())) {
            handleDelayedIndex(account);
        }
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_adminLoginAs);
        relatedRights.add(Admin.R_adminLoginCalendarResourceAs);
        notes.add(AdminRightCheckPoint.Notes.ADMIN_LOGIN_AS);
    }

    /*
    public static void main(String args[]) throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().getAccountByName("user2@slapshot.example.zimbra.com");
        Account admin = Provisioning.getInstance().getAccountByName("admin@slapshot.example.zimbra.com");
        AuthToken at = new AuthToken(acct, System.currentTimeMillis()+DEFAULT_AUTH_LIFETIME*1000, false, admin);
        String token = at.getEncoded();
        System.out.println(token);
    }
    */

    private void handleDelayedIndex(Account acct) {
        // delegate auth isn't guaranteed to run on the the target account's affinity pod, so we have to
        // make sure we don't have stale LDAP attributes
        boolean isLocal;
        try {
            isLocal = Provisioning.onLocalServer(acct);
        } catch (ServiceException e) {
            isLocal = false;
        }
        if (!isLocal) {
            //flush account cache first
            CacheEntry accountEntry = new CacheEntry(Key.CacheEntryBy.id, acct.getId());
            try {
                Provisioning.getInstance().reload(acct);
            } catch (ServiceException e) {
                ZimbraLog.index.warn("error reloading non-local account %s during delegated auth", acct.getName(), e);
            }
        }
        if (acct.isFeatureDelayedIndexEnabled() && acct.getDelayedIndexStatus() == DelayedIndexStatus.suppressed) {
            try {
                acct.setDelayedIndexStatus(DelayedIndexStatus.waitingForSearch);
                if (!isLocal) {
                    AccountUtil.broadcastFlushCache(acct);
                    ZimbraLog.index.info("updated delayed index status for non-local %s to waitingForSearch and broadcast flushCache", acct.getName());
                } else {
                    ZimbraLog.index.info("updated delayed index status for %s to waitingForSearch", acct.getName());
                }
            } catch (ServiceException e) {
                ZimbraLog.index.error("error updating delayed index status for account %s after delegate auth", acct.getName(), e);
            }
        }
    }
}
