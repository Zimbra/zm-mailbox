/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.listeners.AccountListener;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ModifyAccountRequest;
import com.zimbra.soap.admin.type.CacheEntryType;

/**
 * @author schemers
 */
public class ModifyAccount extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };

    @Override
    protected String[] getProxiedAccountPath() {
        return TARGET_ACCOUNT_PATH;
    }

    /**
     * must be careful and only allow modifies to accounts/attrs domain admin has access to
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
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        ModifyAccountRequest req = zsc.elementToJaxb(request);
        AuthToken authToken = zsc.getAuthToken();
        String id = req.getId();
        if (null == id) {
            throw ServiceException.INVALID_REQUEST("missing required attribute: " + AdminConstants.E_ID, null);
        }

        Account account = prov.get(AccountBy.id, id, authToken);

        Map<String, Object> attrs = req.getAttrsAsOldMultimap();
        defendAgainstAccountHarvesting(account, AccountBy.id, id, zsc, attrs);

        // check to see if quota is being changed
        long curQuota = account.getLongAttr(Provisioning.A_zimbraMailQuota, 0);

        /*
         * // Note: isDomainAdminOnly *always* returns false for pure ACL based AccessManager // checkQuota is called
         * only for domain based access manager, remove when we // can totally deprecate domain based access manager if
         * (isDomainAdminOnly(zsc)) checkQuota(zsc, account, attrs);
         */

        /*
         * for bug 42896, the above is no longer true.
         * 
         * For quota, we have to support the per admin limitation zimbraDomainAdminMaxMailQuota, until we come up with a
         * framework to support constraints on a per admin basis.
         * 
         * for now, always call checkQuota, which will check zimbraDomainAdminMaxMailQuota.
         * 
         * If the access manager, and if we have come here, it has already passed the constraint checking, in the
         * checkAccountRight call. If it had violated any constraint, it would have errored out. i.e. for
         * zimbraMailQuota, both zimbraConstraint and zimbraDomainAdminMaxMailQuota are enforced.
         */
        checkQuota(zsc, account, attrs);

        // check to see if cos is being changed, need right on new cos
        checkCos(zsc, account, attrs);

        Server newServer = null;
        String newServerName = getStringAttrNewValue(Provisioning.A_zimbraMailHost, attrs);
        if (newServerName != null) {
            newServer = Provisioning.getInstance().getServerByName(newServerName);
            defendAgainstServerNameHarvesting(newServer, Key.ServerBy.name, newServerName, zsc, Admin.R_listServer);
        }
        String oldStatus = account.getAccountStatus(prov);
        boolean rollbackOnFailure = LC.rollback_on_account_listener_failure.booleanValue();

        String oldSuspendReason = account.getAccountSuspensionReason();
        if (attrs.containsKey(Provisioning.A_zimbraAccountStatus)) {
            String newStatus = (String) attrs.get(Provisioning.A_zimbraAccountStatus);
            //clearing account suspension reason if new status is active and old status other than active.
            if (!oldStatus.equals(AccountStatus.active.toString())
                    && newStatus.equals(AccountStatus.active.toString())) {
                attrs.put(Provisioning.A_zimbraAccountSuspensionReason, "");
            }
            if ((newStatus.equals(AccountStatus.locked.toString()) || newStatus.equals(AccountStatus.lockout.toString()))
                    && attrs.containsKey(Provisioning.A_zimbraAccountSuspensionReason)) {
                account.setAccountSuspensionReason((String) attrs.get(Provisioning.A_zimbraAccountSuspensionReason));
            }
            try {
                AccountListener.invokeOnStatusChange(account, oldStatus, newStatus, zsc, rollbackOnFailure);
            } catch (ServiceException se) {
                ZimbraLog.account.error(se.getMessage());
                account.setAccountStatus(AccountStatus.fromString(oldStatus));
                if (!StringUtil.isNullOrEmpty(oldSuspendReason)) {
                    account.setAccountSuspensionReason(oldSuspendReason);
                }
                throw se;
            }
        }
        if (!attrs.containsKey(Provisioning.A_zimbraAccountStatus) && attrs.containsKey(Provisioning.A_zimbraAccountSuspensionReason)) {
            ZimbraLog.account.info("Account Modify request having suspension reason without account status, ignoring updation");
            attrs.remove(Provisioning.A_zimbraAccountSuspensionReason);
        }
        if (attrs.containsKey(Provisioning.A_givenName) || attrs.containsKey(Provisioning.A_sn)) {
            String firstName = (String) attrs.get(Provisioning.A_givenName);
            String lastName = (String) attrs.get(Provisioning.A_sn);
            try {
                AccountListener.invokeOnNameChange(account, firstName, lastName, zsc, rollbackOnFailure);
            } catch (ServiceException se) {
                ZimbraLog.account.error(se.getMessage());
                if (rollbackOnFailure) {
                    // roll back status change account listener updates, if within same request
                    if (attrs.containsKey(Provisioning.A_zimbraAccountStatus)) {
                        String newStatus = (String) attrs.get(Provisioning.A_zimbraAccountStatus);
                        if (attrs.containsKey(Provisioning.A_zimbraAccountSuspensionReason) && !StringUtil.isNullOrEmpty(oldSuspendReason)) {
                            account.setAccountSuspensionReason(oldSuspendReason);
                        }
                        try {
                            AccountListener.invokeOnStatusChange(account, newStatus, oldStatus, zsc, rollbackOnFailure);
                        } catch (ServiceException sse) {
                            ZimbraLog.account.error(se.getMessage());
                            throw se;
                        }
                    }
                } else {
                    ZimbraLog.account.warn("No rollback on account listener for zimbra account status change failure, there may be inconsistency in account. %s", se.getMessage());
                }
                throw se;
            }
        }

        try {
            // pass in true to checkImmutable
            prov.modifyAttrs(account, attrs, true);
        } catch (ServiceException se) {
            if (rollbackOnFailure) {
                ZimbraLog.account.debug("Exception occured while modifying account in zimbra for %s, roll back listener updates.", account.getMail());
                // roll back status change updates
                if (attrs.containsKey(Provisioning.A_zimbraAccountStatus)) {
                    String newStatus = (String) attrs.get(Provisioning.A_zimbraAccountStatus);
                    ZimbraLog.account.debug("Listener rollback for account status change of from \"%s\" to \"%s\".", newStatus, oldStatus);
                    if (attrs.containsKey(Provisioning.A_zimbraAccountSuspensionReason)
                            && !StringUtil.isNullOrEmpty(oldSuspendReason)) {
                        account.setAccountSuspensionReason(oldSuspendReason);
                    }
                    try {
                        AccountListener.invokeOnStatusChange(account, newStatus, oldStatus, zsc, rollbackOnFailure);
                    } catch (ServiceException sse) {
                        ZimbraLog.account.error(sse.getMessage());
                        throw sse;
                    }
                }
                // roll back name change updates
                if (attrs.containsKey(Provisioning.A_givenName) || attrs.containsKey(Provisioning.A_sn)) {
                    try {
                        ZimbraLog.account.debug("Listener rollback for account name change. First name: %s Last name:  %s.", account.getGivenName(), account.getSn());
                        AccountListener.invokeOnNameChange(account, account.getGivenName(), account.getSn(), zsc, rollbackOnFailure);
                    } catch (ServiceException nse) {
                        ZimbraLog.account.error(nse.getMessage());
                        throw nse;
                    }
                }
            } else {
                ZimbraLog.account.warn("No rollback on account listener for zimbra modify account failure, there may be inconsistency in account. %s", se.getMessage());
            }
            throw se;
        }

        // get account again, in the case when zimbraCOSId or zimbraForeignPrincipal
        // is changed, the cache object(he one we are holding on to) would'd been
        // flushed out from cache. Get the account again to get the fresh one.
        account = prov.get(AccountBy.id, id, zsc.getAuthToken());

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] { "cmd", "ModifyAccount", "name", account.getName() }, attrs));

        if (newServer != null) {
            checkNewServer(zsc, context, account, newServer);
        }

        long newQuota = account.getLongAttr(Provisioning.A_zimbraMailQuota, 0);
        if (newQuota != curQuota) {
            // clear the quota cache
            AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
            if (session != null) {
                GetQuotaUsage.clearCachedQuotaUsage(session);
            }
        }

        Element response = zsc.createElement(AdminConstants.MODIFY_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account);
        return response;
    }

    public static String getStringAttrNewValue(String attrName, Map<String, Object> attrs) throws ServiceException {
        Object object = attrs.get(attrName);
        if (object == null) {
            object = attrs.get("+" + attrName);
        }
        if (object == null) {
            object = attrs.get("-" + attrName);
        }
        if (object == null) {
            return null;
        }

        if (!(object instanceof String)) {
            throw ServiceException.PERM_DENIED("can not modify " + attrName + "(single valued attribute)");
        }
        return (String) object;
    }

    private void checkQuota(ZimbraSoapContext zsc, Account account, Map<String, Object> attrs) throws ServiceException {
        String quotaAttr = getStringAttrNewValue(Provisioning.A_zimbraMailQuota, attrs);
        if (quotaAttr == null) {
            return; // not changing it
        }
        long quota;

        if (quotaAttr.equals("")) {
            // they are unsetting it, so check the COS
            quota = Provisioning.getInstance().getCOS(account).getIntAttr(Provisioning.A_zimbraMailQuota, 0);
        } else {
            try {
                quota = Long.parseLong(quotaAttr);
            } catch (NumberFormatException e) {
                throw AccountServiceException.INVALID_ATTR_VALUE("can not modify mail quota (invalid format): "
                        + quotaAttr, e);
            }
        }

        if (!canModifyMailQuota(zsc, account, quota))
            throw ServiceException
                    .PERM_DENIED("can not modify mail quota, domain admin can only modify quota if "
                            + "zimbraDomainAdminMaxMailQuota is set to 0 or set to a certain value and quota is less than that value.");
    }

    private void checkCos(ZimbraSoapContext zsc, Account account, Map<String, Object> attrs) throws ServiceException {
        String newCosId = getStringAttrNewValue(Provisioning.A_zimbraCOSId, attrs);
        if (newCosId == null) {
            return; // not changing it
        }

        Provisioning prov = Provisioning.getInstance();
        if (newCosId.equals("")) {
            // they are unsetting it, so check the domain
            Domain domain = prov.getDomain(account);
            if (domain != null) {
                newCosId = account.isIsExternalVirtualAccount() ? domain.getDomainDefaultExternalUserCOSId() : domain
                        .getDomainDefaultCOSId();
                if (newCosId == null) {
                    return; // no domain cos, use the default COS, which is available to all
                }
            }
        }

        Cos cos = prov.get(Key.CosBy.id, newCosId);
        if (cos == null) {
            throw AccountServiceException.NO_SUCH_COS(newCosId);
        }

        // call checkRight instead of checkCosRight, because:
        // 1. no domain based access manager backward compatibility issue
        // 2. we only want to check right if we are using pure ACL based access manager.
        checkRight(zsc, cos, Admin.R_assignCos);
    }

    /*
     * if the account's home server is changed as a result of this command and the new server is no longer this server,
     * need to send a flush cache command to the new server so we don't get into the following:
     *
     * account is on server A (this server)
     *
     * on server B: zmprov ma {account} zimbraMailHost B (the ma is proxied to server A; and on server B, the account
     * still appears to be on A)
     *
     * zmprov ma {account} {any attr} {value} ERROR: service.TOO_MANY_HOPS Until the account is expired from cache on
     * server B.
     */
    private void checkNewServer(ZimbraSoapContext zsc, Map<String, Object> context, Account acct, Server newServer) {
        try {
            if (!Provisioning.onLocalServer(acct)) {
                // in the case when zimbraMailHost is being removed, newServer will be null
                if (newServer != null) {
                    SoapProvisioning soapProv = new SoapProvisioning();
                    String adminUrl = URLUtil.getAdminURL(newServer, AdminConstants.ADMIN_SERVICE_URI, true);
                    soapProv.soapSetURI(adminUrl);
                    soapProv.soapZimbraAdminAuthenticate();
                    soapProv.flushCache(CacheEntryType.account,
                            new CacheEntry[] { new CacheEntry(CacheEntryBy.id, acct.getId()) });
                }
            }
        } catch (ServiceException e) {
            // ignore any error and continue
            ZimbraLog.mailbox.warn(
                    "cannot flush account cache on server " + (newServer == null ? "" : newServer.getName()) + " for "
                            + acct.getName(), e);
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_assignCos);

        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, Admin.R_modifyAccount.getName(), "account")
                + "\n");

        notes.add("Notes on " + Provisioning.A_zimbraCOSId + ": " + "If setting " + Provisioning.A_zimbraCOSId
                + ", needs the " + Admin.R_assignCos.getName() + " right on the cos." + "If removing "
                + Provisioning.A_zimbraCOSId + ", needs the " + Admin.R_assignCos.getName()
                + " right on the domain default cos. (in domain attribute " + Provisioning.A_zimbraDomainDefaultCOSId
                + ").");
        notes.add(String.format("When changing %s attribute, %s right on the server identified by new %s is required.",
                Provisioning.A_zimbraMailHost, Admin.R_listServer, Provisioning.A_zimbraMailHost));
    }
}
