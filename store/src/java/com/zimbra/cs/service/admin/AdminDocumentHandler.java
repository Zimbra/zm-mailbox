/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.account.Key.ServerBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.PseudoTarget;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.names.NameUtil;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.CosSelector.CosBy;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.type.AccountSelector;

/**
 * @since Oct 4, 2004
 * @author schemers
 */
public abstract class AdminDocumentHandler extends DocumentHandler implements AdminRightCheckPoint {

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    @Override
    public boolean needsAdminAuth(Map<String, Object> context) {
        return true;
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    protected String[] getProxiedAccountPath() {
        return null;
    }

    protected String[] getProxiedAccountElementPath() {
        return null;
    }

    protected String[] getProxiedResourcePath() {
        return null;
    }

    protected String[] getProxiedResourceElementPath() {
        return null;
    }

    protected String[] getProxiedServerPath() {
        return null;
    }

    protected Account getAccount(Provisioning prov, AccountBy accountBy, String value, AuthToken authToken)
            throws ServiceException {
        Account acct = null;

        // first try getting it from master if not in cache
        try {
            acct = prov.get(accountBy, value, true, authToken);
        } catch (ServiceException e) {
            // try the replica
            acct = prov.get(accountBy, value, false, authToken);
        }
        return acct;
    }

    private CalendarResource getCalendarResource(Provisioning prov, Key.CalendarResourceBy crBy, String value)
            throws ServiceException {
        CalendarResource cr = null;

        // first try getting it from master if not in cache
        try {
            cr = prov.get(crBy, value, true);
        } catch (ServiceException e) {
            // try the replica
            cr = prov.get(crBy, value, false);
        }
        return cr;
    }

    public static Entry pseudoTargetInSameDomainAsEmail(TargetType targetType, String emailAddr) {
        String parts[] = EmailUtil.getLocalPartAndDomain(emailAddr);
        if (parts == null || parts.length < 2) {
            return null;
        }
        String domainStr = parts[1];
        try {
            return PseudoTarget.createPseudoTarget(Provisioning.getInstance(), targetType, DomainBy.name, domainStr,
                    false, null, null);
        } catch (ServiceException e) {
            return null;
        }
    }

    protected void defendAgainstAccountHarvestingWhenAbsent(AccountBy by, String selectorKey, ZimbraSoapContext zsc,
            AccountHarvestingChecker checker) throws ServiceException {
        AuthToken authToken = zsc.getAuthToken();
        if (authToken.isAdmin()) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(selectorKey);
        } else {
            if (AccountBy.name.equals(by) && AuthToken.isAnyAdmin(authToken)) {
                Entry pseudoTarget = pseudoTargetInSameDomainAsEmail(TargetType.account, selectorKey);
                if (pseudoTarget != null) {
                    checker.check((Account) pseudoTarget, selectorKey);
                    throw AccountServiceException.NO_SUCH_ACCOUNT(selectorKey); // passed the check
                }
            }
        }
        throw ServiceException.DEFEND_ACCOUNT_HARVEST(selectorKey);
    }

    protected void defendAgainstAccountHarvesting(Account account, AccountBy by, String selectorKey,
            ZimbraSoapContext zsc, AccountHarvestingChecker checker) throws ServiceException {
        if (account == null) {
            defendAgainstAccountHarvestingWhenAbsent(by, selectorKey, zsc, checker);
            return;
        }
        checker.check(account, selectorKey);
    }

    protected void defendAgainstAccountHarvestingWhenAbsent(AccountBy by, String selectorKey, ZimbraSoapContext zsc,
            Object needed) throws ServiceException {
        defendAgainstAccountHarvestingWhenAbsent(by, selectorKey, zsc,
                new AccountHarvestingCheckerUsingCheckAccountRight(zsc, needed));
    }

    protected void defendAgainstAccountHarvesting(Account account, AccountBy by, String selectorKey,
            ZimbraSoapContext zsc, Object needed) throws ServiceException {
        AccountHarvestingCheckerUsingCheckAccountRight checker = new AccountHarvestingCheckerUsingCheckAccountRight(
                zsc, needed);
        if (account == null) {
            defendAgainstAccountHarvestingWhenAbsent(by, selectorKey, zsc, checker);
            return;
        }
        checker.check(account, selectorKey);
    }

    protected void defendAgainstAccountOrCalendarResourceHarvestingWhenAbsent(AccountBy accountBy, String selectorKey,
            ZimbraSoapContext zsc, AdminRight rightForAcct, AdminRight rightForCalRes) throws ServiceException {
        try {
            CalendarResourceBy calResBy = CalendarResourceBy.fromString(accountBy.toString());
            defendAgainstCalResourceHarvestingWhenAbsent(calResBy, selectorKey, zsc,
                    new CalResourceHarvestingCheckerUsingCheckCalendarResourceRight(zsc, rightForCalRes));
        } catch (ServiceException e) {
            defendAgainstAccountHarvestingWhenAbsent(accountBy, selectorKey, zsc, rightForAcct);
        }
    }

    protected void defendAgainstAccountOrCalendarResourceHarvesting(Account account, AccountBy accountBy,
            String selectorKey, ZimbraSoapContext zsc, AdminRight rightForAcct, AdminRight rightForCalRes)
            throws ServiceException {
        if (account == null) {
            defendAgainstAccountOrCalendarResourceHarvestingWhenAbsent(accountBy, selectorKey, zsc, rightForAcct,
                    rightForCalRes);
        } else if (account.isCalendarResource()) {
            Provisioning prov = Provisioning.getInstance();
            CalendarResource resource = prov.get(CalendarResourceBy.id, account.getId());
            CalendarResourceBy calResBy = CalendarResourceBy.fromString(accountBy.toString());
            defendAgainstCalResourceHarvesting(resource, calResBy, selectorKey, zsc, rightForCalRes);
        } else {
            defendAgainstAccountHarvesting(account, accountBy, selectorKey, zsc, rightForAcct);
        }
    }

    protected void defendAgainstCalResourceHarvestingWhenAbsent(CalendarResourceBy by, String selectorKey,
            ZimbraSoapContext zsc, CalResourceHarvestingChecker checker) throws ServiceException {
        AuthToken authToken = zsc.getAuthToken();
        if (authToken.isAdmin()) {
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(selectorKey);
        } else {
            if (CalendarResourceBy.name.equals(by) && AuthToken.isAnyAdmin(authToken)) {
                Entry pseudoTarget = pseudoTargetInSameDomainAsEmail(TargetType.calresource, selectorKey);
                if (pseudoTarget != null) {
                    checker.check((CalendarResource) pseudoTarget, selectorKey);
                    throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(selectorKey); // passed the check
                }
            }
        }
        throw ServiceException.DEFEND_CALENDAR_RESOURCE_HARVEST(selectorKey);
    }

    protected void defendAgainstCalResourceHarvesting(CalendarResource calRes, CalendarResourceBy by,
            String selectorKey, ZimbraSoapContext zsc, CalResourceHarvestingChecker checker) throws ServiceException {
        if (calRes == null) {
            defendAgainstCalResourceHarvestingWhenAbsent(by, selectorKey, zsc, checker);
            return;
        }
        checker.check(calRes, selectorKey);
    }

    protected void defendAgainstCalResourceHarvesting(CalendarResource calRes, CalendarResourceBy by,
            String selectorKey, ZimbraSoapContext zsc, Object needed) throws ServiceException {
        CalResourceHarvestingCheckerUsingCheckCalendarResourceRight checker = new CalResourceHarvestingCheckerUsingCheckCalendarResourceRight(
                zsc, needed);
        if (calRes == null) {
            defendAgainstCalResourceHarvestingWhenAbsent(by, selectorKey, zsc, checker);
            return;
        }
        checker.check(calRes, selectorKey);
    }

    protected void defendAgainstServerNameHarvesting(Server server, ServerBy by, String selectorKey,
            ZimbraSoapContext zsc, AdminRight needed) throws ServiceException {
        if (server == null) {
            defendAgainstServerNameHarvestingWhenAbsent(by, selectorKey, zsc, needed);
        } else {
            checkRight(zsc, server, needed);
        }
    }

    protected void defendAgainstServerNameHarvestingWhenAbsent(ServerBy by, String selectorKey, ZimbraSoapContext zsc,
            AdminRight needed) throws ServiceException {
        AuthToken authToken = zsc.getAuthToken();
        if (authToken.isAdmin()) {
            throw AccountServiceException.NO_SUCH_SERVER(selectorKey);
        } else {
            Entry psedoTarget = PseudoTarget.createPseudoTarget(Provisioning.getInstance(), TargetType.server, null,
                    null, false, null, null);
            checkRight(zsc, psedoTarget, needed);
            throw AccountServiceException.NO_SUCH_SERVER(selectorKey);
        }
    }

    protected interface AccountHarvestingChecker {
        public void check(Account account, String selectorKey) throws ServiceException;
    }

    protected abstract class AccountHarvestingCheckerBase implements AccountHarvestingChecker {
        protected final ZimbraSoapContext zsc;
        protected final AuthToken authToken;
        protected ServiceException firstException = null;

        protected abstract void doRightsCheck(Account account) throws ServiceException;

        public AccountHarvestingCheckerBase(ZimbraSoapContext zsc) {
            this.zsc = zsc;
            authToken = zsc.getAuthToken();
        }

        protected boolean hasRight(Account account, String selectorKey) {
            try {
                doRightsCheck(account);
                return true;
            } catch (ServiceException se) {
                if (firstException == null) {
                    if (authToken.isAdmin()) {
                        firstException = se;
                    } else {
                        firstException = ServiceException.DEFEND_ACCOUNT_HARVEST(selectorKey);
                    }
                }
                return false;
            }
        }
    }

    protected class AccountHarvestingCheckerUsingCheckAccountRight extends AccountHarvestingCheckerBase {
        private Object needed = null;

        public AccountHarvestingCheckerUsingCheckAccountRight(ZimbraSoapContext zsc, Object needed) {
            super(zsc);
            this.needed = needed;
        }

        @Override
        protected void doRightsCheck(Account account) throws ServiceException {
            checkAccountRight(zsc, account, needed);
        }

        @Override
        public void check(Account account, String selectorKey) throws ServiceException {
            if (hasRight(account, selectorKey)) {
                return;
            }
            throw firstException;
        }
    }

    protected class AccountHarvestingCheckerUsingCheckRight extends AccountHarvestingCheckerBase {
        private final AdminRight adminRight;
        private final Map<String, Object> context;

        public AccountHarvestingCheckerUsingCheckRight(ZimbraSoapContext zsc, Map<String, Object> context,
                AdminRight right) {
            super(zsc);
            this.adminRight = right;
            this.context = context;
        }

        @Override
        protected void doRightsCheck(Account account) throws ServiceException {
            checkRight(zsc, context, account, adminRight);
        }

        @Override
        public void check(Account account, String selectorKey) throws ServiceException {
            if (hasRight(account, selectorKey)) {
                return;
            }
            throw firstException;
        }
    }

    protected interface CalResourceHarvestingChecker {
        public void check(CalendarResource account, String selectorKey) throws ServiceException;
    }

    protected abstract class CalResourceHarvestingCheckerBase implements CalResourceHarvestingChecker {
        protected final ZimbraSoapContext zsc;
        protected final AuthToken authToken;
        protected ServiceException firstException = null;

        protected abstract void doRightsCheck(CalendarResource calRes) throws ServiceException;

        public CalResourceHarvestingCheckerBase(ZimbraSoapContext zsc) {
            this.zsc = zsc;
            authToken = zsc.getAuthToken();
        }

        protected boolean hasRight(CalendarResource calRes, String selectorKey) {
            try {
                doRightsCheck(calRes);
                return true;
            } catch (ServiceException se) {
                if (firstException == null) {
                    if (authToken.isAdmin()) {
                        firstException = se;
                    } else {
                        firstException = ServiceException.DEFEND_CALENDAR_RESOURCE_HARVEST(selectorKey);
                    }
                }
                return false;
            }
        }
    }

    protected class CalResourceHarvestingCheckerUsingCheckCalendarResourceRight extends
            CalResourceHarvestingCheckerBase {
        private Object needed = null;

        public CalResourceHarvestingCheckerUsingCheckCalendarResourceRight(ZimbraSoapContext zsc, Object needed) {
            super(zsc);
            this.needed = needed;
        }

        @Override
        protected void doRightsCheck(CalendarResource calRes) throws ServiceException {
            checkCalendarResourceRight(zsc, calRes, needed);
        }

        @Override
        public void check(CalendarResource calRes, String selectorKey) throws ServiceException {
            if (hasRight(calRes, selectorKey)) {
                return;
            }
            throw firstException;
        }
    }

    protected interface GroupHarvestingChecker {
        public void check(Group group, String groupSelectorKey) throws ServiceException;
    }

    protected abstract class GroupHarvestingCheckerBase implements GroupHarvestingChecker {
        protected final ZimbraSoapContext zsc;
        protected final AuthToken authToken;
        protected ServiceException firstException = null;

        protected abstract void doRightsCheck(Group group) throws ServiceException;

        public GroupHarvestingCheckerBase(ZimbraSoapContext zsc) {
            this.zsc = zsc;
            authToken = zsc.getAuthToken();
        }

        protected boolean hasRight(Group group, String selectorKey) {
            try {
                doRightsCheck(group);
                return true;
            } catch (ServiceException se) {
                if (firstException == null) {
                    if (authToken.isAdmin()) {
                        firstException = se;
                    } else {
                        firstException = ServiceException.DEFEND_DL_HARVEST(selectorKey);
                    }
                }
                return false;
            }
        }
    }

    protected class GroupHarvestingCheckerUsingCheckGroupRight extends GroupHarvestingCheckerBase {
        private Object needed = null;

        public GroupHarvestingCheckerUsingCheckGroupRight(ZimbraSoapContext zsc, Object needed) {
            super(zsc);
            this.needed = needed;
        }

        @Override
        protected void doRightsCheck(Group group) throws ServiceException {
            if (group.isDynamic()) {
                checkDynamicGroupRight(zsc, (DynamicGroup) group, needed);
            } else {
                checkDistributionListRight(zsc, (DistributionList) group, needed);
            }
        }

        @Override
        public void check(Group group, String selectorKey) throws ServiceException {
            if (hasRight(group, selectorKey)) {
                return;
            }
            throw firstException;
        }
    }

    protected class GroupHarvestingCheckerUsingGetAttrsPerms extends GroupHarvestingCheckerBase {
        private final AttrRightChecker arc;
        private final List<String> getAttrs;
        private String currAttr;

        public GroupHarvestingCheckerUsingGetAttrsPerms(ZimbraSoapContext zsc, AttrRightChecker arc,
                List<String> getAttrs) {
            super(zsc);
            this.arc = arc;
            this.getAttrs = getAttrs;
        }

        @Override
        protected void doRightsCheck(Group group) throws ServiceException {
            if ((arc != null) && !arc.allowAttr(currAttr)) {
                throw ServiceException.DEFEND_DL_HARVEST(group.getName());
            }
        }

        @Override
        public void check(Group group, String selectorKey) throws ServiceException {
            for (String attr : getAttrs) {
                currAttr = attr;
                if (hasRight(group, selectorKey)) {
                    return;
                }
            }
            throw firstException;
        }
    }

    protected class GroupHarvestingCheckerUsingCheckRight extends GroupHarvestingCheckerBase {
        private final AdminRight adminRight;
        private final Map<String, Object> context;

        public GroupHarvestingCheckerUsingCheckRight(ZimbraSoapContext zsc, Map<String, Object> context,
                AdminRight right) {
            super(zsc);
            this.adminRight = right;
            this.context = context;
        }

        @Override
        protected void doRightsCheck(Group group) throws ServiceException {
            checkRight(zsc, context, group, adminRight);
        }

        @Override
        public void check(Group group, String selectorKey) throws ServiceException {
            if (hasRight(group, selectorKey)) {
                return;
            }
            throw firstException;
        }
    }

    protected void defendAgainstGroupHarvestingWhenAbsent(DistributionListBy dlBy, String groupSelectorKey,
            ZimbraSoapContext zsc, GroupHarvestingChecker checker) throws ServiceException {
        AuthToken authToken = zsc.getAuthToken();
        if (authToken.isAdmin()) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(groupSelectorKey);
        } else {
            if (DistributionListBy.name.equals(dlBy) && AuthToken.isAnyAdmin(authToken)) {
                Entry pseudoTarget = pseudoTargetInSameDomainAsEmail(TargetType.dl, groupSelectorKey);
                if (pseudoTarget != null) {
                    checker.check((DistributionList) pseudoTarget, groupSelectorKey);
                    throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(groupSelectorKey); // passed the check
                }
            }
        }
        throw ServiceException.DEFEND_DL_HARVEST(groupSelectorKey);
    }

    protected void defendAgainstGroupHarvesting(Group group, DistributionListBy dlBy, String groupSelectorKey,
            ZimbraSoapContext zsc, GroupHarvestingChecker checker) throws ServiceException {
        if (group == null) {
            defendAgainstGroupHarvestingWhenAbsent(dlBy, groupSelectorKey, zsc, checker);
            return;
        }
        checker.check(group, groupSelectorKey);
    }

    protected void defendAgainstGroupHarvestingWhenAbsent(DistributionListBy dlBy, String groupSelectorKey,
            ZimbraSoapContext zsc, AdminRight dlRight) throws ServiceException {
        defendAgainstGroupHarvestingWhenAbsent(dlBy, groupSelectorKey, zsc,
                new GroupHarvestingCheckerUsingCheckGroupRight(zsc, dlRight));
    }

    protected void defendAgainstGroupHarvesting(Group group, DistributionListBy dlBy, String groupSelectorKey,
            ZimbraSoapContext zsc, Object dynamicGroupNeeded, Object dlNeeded) throws ServiceException {
        if (group == null) {
            defendAgainstGroupHarvestingWhenAbsent(dlBy, groupSelectorKey, zsc,
                    new GroupHarvestingCheckerUsingCheckGroupRight(zsc, dlNeeded));
            return;
        }
        GroupHarvestingCheckerUsingCheckGroupRight checker = new GroupHarvestingCheckerUsingCheckGroupRight(zsc,
                group.isDynamic() ? dynamicGroupNeeded : dlNeeded);
        checker.check(group, groupSelectorKey);
    }

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        // if we've explicitly been told to execute here, don't proxy
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (zsc.getProxyTarget() != null)
            return null;

        try {
            Provisioning prov = Provisioning.getInstance();

            Provisioning.Reasons reasons = new Provisioning.Reasons();
            // check whether we need to proxy to the home server of a target account
            String[] xpath = getProxiedAccountPath();
            String acctId = (xpath != null ? getXPath(request, xpath) : null);
            if (acctId != null) {
                Account acct = getAccount(prov, AccountBy.id, acctId, zsc.getAuthToken());

                if (acct != null && !DocumentHandler.onLocalServer(acct, reasons)) {
                    ZimbraLog.soap.info("Proxying request: ProxiedAccountPath=%s reason: %s",
                            Joiner.on("/").join(xpath), reasons.getReason());
                    return proxyRequest(request, context, acctId);
                }
            }

            xpath = getProxiedAccountElementPath();
            Element acctElt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (acctElt != null) {
                Account acct = getAccount(prov, AccountBy.fromString(acctElt.getAttribute(AdminConstants.A_BY)),
                        acctElt.getText(), zsc.getAuthToken());

                if (acct != null && !DocumentHandler.onLocalServer(acct, reasons)) {
                    ZimbraLog.soap.info("Proxying request: ProxiedAccountElementPath=%s acctElt=%s reason: %s",
                            Joiner.on("/").join(xpath), acctElt.toString(), reasons.getReason());
                    return proxyRequest(request, context, acct.getId());
                }
            }

            // check whether we need to proxy to the home server of a target calendar resource
            xpath = getProxiedResourcePath();
            String rsrcId = (xpath != null ? getXPath(request, xpath) : null);
            if (rsrcId != null) {
                CalendarResource rsrc = getCalendarResource(prov, Key.CalendarResourceBy.id, rsrcId);
                if (rsrc != null && !DocumentHandler.onLocalServer(rsrc, reasons)) {
                    ZimbraLog.soap.info("Proxying request: ProxiedResourcePath=%s rsrcId=%s reason: %s",
                            Joiner.on("/").join(xpath), rsrcId, reasons.getReason());
                    return proxyRequest(request, context, rsrcId);
                }
            }

            xpath = getProxiedResourceElementPath();
            Element resourceElt = (xpath != null ? getXPathElement(request, xpath) : null);
            if (resourceElt != null) {
                CalendarResource rsrc = getCalendarResource(prov,
                        Key.CalendarResourceBy.fromString(resourceElt.getAttribute(AdminConstants.A_BY)),
                        resourceElt.getText());
                if (rsrc != null && !DocumentHandler.onLocalServer(rsrc, reasons)) {
                    ZimbraLog.soap.info("Proxying request: ProxiedResourceElementPath=%s resourceElt=%s reason: %s",
                            Joiner.on("/").join(xpath), resourceElt.toString(), reasons.getReason());
                    return proxyRequest(request, context, rsrc.getId());
                }
            }

            // check whether we need to proxy to a target server
            xpath = getProxiedServerPath();
            String serverId = (xpath != null ? getXPath(request, xpath) : null);
            if (serverId != null) {
                Server server = prov.get(Key.ServerBy.id, serverId);
                if (server != null && !getLocalHostId().equalsIgnoreCase(server.getId())) {
                    ZimbraLog.soap
                            .info("Proxying request: ProxiedServerPath=%s serverId=%s server=%s reason: server ID=%s != localHostId=%s",
                                    Joiner.on("/").join(xpath), serverId, server.getName(), server.getId(),
                                    getLocalHostId());
                    return proxyRequest(request, context, server);
                }
            }

            return null;
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode()))
                return null;
            // but if it's a real error, it's a real error
            throw e;
        }
    }

    @Override
    public Session.Type getDefaultSessionType() {
        return Session.Type.ADMIN;
    }

    protected Set<String> getReqAttrs(Element request, AttributeClass klass) throws ServiceException {
        String attrsStr = request.getAttribute(AdminConstants.A_ATTRS, null);
        return getReqAttrs(attrsStr, klass);
    }

    /*
     * if specific attrs are requested on Get{ldap-object}: - INVALID_REQUEST is thrown if any of the requested attrs is
     * not a valid attribute on the entry - PERM_DENIED is thrown if the authed account does not have get attr right for
     * all the requested attrs.
     *
     * Because for the get{Object} calls, we want to be strict, as opposed to misleading the client that a requested
     * attribute is not set on the entry.
     *
     * Note: the behavior is different than the behavior of SearchDirectory, in that: - if any of the requested attrs is
     * not a valid attribute on the entry: ignored - if the authed account does not have get attr right for all the
     * requested attrs: the entry is not included in the response
     */
    protected Set<String> getReqAttrs(String attrsStr, AttributeClass klass) throws ServiceException {
        if (attrsStr == null) {
            return null;
        }

        String[] attrs = attrsStr.split(",");

        Set<String> attrsOnEntry = AttributeManager.getInstance().getAllAttrsInClass(klass);
        Set<String> validAttrs = new HashSet<String>();

        for (String attr : attrs) {
            if (attrsOnEntry.contains(attr)) {
                validAttrs.add(attr);
            } else {
                throw ServiceException.INVALID_REQUEST("requested attribute " + attr + " is not on " + klass.name(),
                        null);
            }
        }

        // check and throw if validAttrs is empty?
        // probably not, to be compatible with SearchDirectory

        return validAttrs;
    }

    public boolean isDomainAdminOnly(ZimbraSoapContext zsc) {
        return AccessManager.getInstance().isDomainAdminOnly(zsc.getAuthToken());
    }

    public Domain getAuthTokenAccountDomain(ZimbraSoapContext zsc) throws ServiceException {
        return AccessManager.getInstance().getDomain(zsc.getAuthToken());
    }

    protected boolean canAccessDomain(ZimbraSoapContext zsc, String domainName) throws ServiceException {
        return AccessManager.getInstance().canAccessDomain(zsc.getAuthToken(), domainName);
    }

    protected boolean canAccessDomain(ZimbraSoapContext zsc, Domain domain) throws ServiceException {
        return canAccessDomain(zsc, domain.getName());
    }

    protected boolean canModifyMailQuota(ZimbraSoapContext zsc, Account target, long mailQuota) throws ServiceException {
        return AccessManager.getInstance().canModifyMailQuota(zsc.getAuthToken(), target, mailQuota);
    }

    /*
     * TODO: can't be private yet, still called from ZimbraAdminExt and ZimbraCustomerServices/hosted Need to fix those
     * callsite to call one of the check*** methods.
     *
     * after that, move this method and related methods to AdminAccessControl and only call this method from there.
     */
    public boolean canAccessEmail(ZimbraSoapContext zsc, String email) throws ServiceException {
        return canAccessDomain(zsc, NameUtil.EmailAddress.getDomainNameFromEmail(email));
    }

    /*
     * ====================================================================== Connector methods between domain based
     * access manager and pure ACL based access manager.
     * ======================================================================
     */

    /**
     * only called for domain based access manager
     *
     * @param attrClass
     * @param attrs
     * @throws ServiceException
     */
    public void checkModifyAttrs(ZimbraSoapContext zsc, AttributeClass attrClass, Map<String, Object> attrs)
            throws ServiceException {
        AdminAccessControl.getAdminAccessControl(zsc).checkModifyAttrs(attrClass, attrs);
    }

    /**
     * This has to be called *after* the *can create* check. For domain based AccessManager, all attrs are allowed if
     * the admin can create.
     */
    protected void checkSetAttrsOnCreate(ZimbraSoapContext zsc, TargetType targetType, String entryName,
            Map<String, Object> attrs) throws ServiceException {
        AdminAccessControl.getAdminAccessControl(zsc).checkSetAttrsOnCreate(targetType, entryName, attrs);
    }

    protected boolean hasRightsToList(ZimbraSoapContext zsc, NamedEntry target, AdminRight listRight,
            Object getAttrRight) throws ServiceException {
        return AdminAccessControl.getAdminAccessControl(zsc).hasRightsToList(target, listRight, getAttrRight);
    }

    protected boolean hasRightsToListCos(ZimbraSoapContext zsc, Cos target, AdminRight listRight, Object getAttrRight)
            throws ServiceException {
        return AdminAccessControl.getAdminAccessControl(zsc).hasRightsToListCos(target, listRight, getAttrRight);
    }

    /**
     * ------------------- non-domained rights (i.e. not: account, calendar resource, distribution list, domain)
     *
     * For domain based access manager, if the authed admin is domain admin only, it should have been rejected in
     * SoapEngine. So it should just return true here. But we sanity check again, just in case. -------------------
     */
    protected AdminAccessControl checkRight(ZimbraSoapContext zsc, Map<String, Object> context, Entry target,
            Object needed) throws ServiceException {
        AccessManager am = AccessManager.getInstance();

        //
        // yuck, the isDomainBasedAccessManager logic has to be here
        // only because of the call to domainAuthSufficient
        //
        if (AdminAccessControl.isDomainBasedAccessManager(am)) {
            // sanity check, this path is really for global admins
            if (isDomainAdminOnly(zsc)) {
                if (!domainAuthSufficient(context))
                    throw ServiceException.PERM_DENIED("cannot access entry");
            }

            // yuck, return a AdminAccessControl object instead of null so
            // we don't NPE at callsites or having to check null if they need
            // to use the aac.
            // this whole method should probably be deleted anyway.
            return AdminAccessControl.getAdminAccessControl(zsc);

        } else {
            return checkRight(zsc, target, needed);
        }
    }

    /**
     * This API is for checking ACL rights only, domain based access manager will always return OK. This should be
     * called only when
     *
     * (1) domain based permission checking has passed. or (2) from SOAP handlers that are actually admin commands, but
     * can't inherit from AdminDocumentHanlder because it already inherited from otehr class, e.g.
     * SearchMultipleMailboxes. This method is static just because of that. Ideally it should call the other checkRight
     * API, which does sanity checking for domain admins if the active AccessManager is a domain based access manager.
     * But that API has other dependency on AdminDocumentHanlder/DocumentHandler instance methods, also, the sanity is
     * really not necessary, we should remove it at some point when we can completely retire the domain based access
     * manager.
     *
     * TODO: find callsites of non AdminDocumentHanlder and call AdminAccessControl directly, after this method can be
     * protected and non-static
     *
     * @param zsc
     * @param target
     * @param needed
     * @throws ServiceException
     */
    public static AdminAccessControl checkRight(ZimbraSoapContext zsc, Entry target, Object needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkRight(target, needed);
        return aac;
    }


    /*
     * ------------- cos right -------------
     */
    protected AdminAccessControl checkCosRight(ZimbraSoapContext zsc, Cos cos, Object needed) throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkCosRight(cos, needed);
        return aac;
    }

    /*
     * ------------- account right -------------
     */
    protected AdminAccessControl checkAccountRight(ZimbraSoapContext zsc, Account account, Object needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkAccountRight(this, account, needed);
        return aac;
    }

    /*
     * ----------------------- calendar resource right -----------------------
     */
    protected AdminAccessControl checkCalendarResourceRight(ZimbraSoapContext zsc, CalendarResource cr, Object needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkCalendarResourceRight(this, cr, needed);
        return aac;
    }

    /*
     * convenient method for checking the admin login as right
     */
    protected AdminAccessControl checkAdminLoginAsRight(ZimbraSoapContext zsc, Provisioning prov, Account account)
            throws ServiceException {
        if (account.isCalendarResource()) {
            // need a CalendarResource instance for RightChecker
            CalendarResource resource = prov.get(Key.CalendarResourceBy.id, account.getId());
            return checkCalendarResourceRight(zsc, resource, Admin.R_adminLoginCalendarResourceAs);
        } else {
            return checkAccountRight(zsc, account, Admin.R_adminLoginAs);
        }
    }

    /*
     * -------- DL right --------
     */
    protected AdminAccessControl checkDistributionListRight(ZimbraSoapContext zsc, DistributionList dl, Object needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDistributionListRight(this, dl, needed);
        return aac;
    }

    /*
     * -------- Dynamic group right --------
     */
    protected AdminAccessControl checkDynamicGroupRight(ZimbraSoapContext zsc, DynamicGroup group, Object needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDynamicGroupRight(this, group, needed);
        return aac;
    }

    /*
     * ------------ domain right ------------
     */
    /**
     * called by handlers that need to check right on a domain for domain-ed objects: account, alias, cr, dl.
     *
     * Note: this method *do* check domain status.
     */
    protected AdminAccessControl checkDomainRightByEmail(ZimbraSoapContext zsc, String email, AdminRight needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDomainRightByEmail(this, email, needed);
        return aac;
    }

    /**
     * Note: this method do *not* check domain status.
     */
    protected AdminAccessControl checkDomainRight(ZimbraSoapContext zsc, String domainName, Object needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDomainRight(this, domainName, needed);
        return aac;
    }

    /**
     * Note: this method do *not* check domain status.
     */
    protected AdminAccessControl checkDomainRight(ZimbraSoapContext zsc, Domain domain, Object needed)
            throws ServiceException {
        AdminAccessControl aac = AdminAccessControl.getAdminAccessControl(zsc);
        aac.checkDomainRight(this, domain, needed);
        return aac;
    }

    // ==========================================
    // bookkeeping and documenting gadgets
    // ==========================================

    // book mark for callsites still needs ACL checking but is not done yet
    // in the end, no one should call this method
    protected void checkRightTODO() {
    }

    // for documenting rights needed and notes for a SOAP.
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.TODO);
    }

    public Account verifyAccountHarvestingAndPerms(AccountSelector acctSel, ZimbraSoapContext zsc) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        if (acctSel == null) {
            ServiceException se = ServiceException.INVALID_REQUEST(String.format("missing <%s>", AdminConstants.E_ACCOUNT), null);
            ZimbraLog.filter.debug("AccountSelector not found", se);
            throw se;
        }
        String accountSelectorKey = acctSel.getKey();
        AccountBy by = acctSel.getBy().toKeyAccountBy();
        Account account = prov.get(by, accountSelectorKey, zsc.getAuthToken());

        defendAgainstAccountHarvesting(account, by, accountSelectorKey, zsc, Admin.R_getAccountInfo);
        if (!canModifyOptions(zsc, account)) {
            ServiceException se = ServiceException.PERM_DENIED("cannot modify options");
            ZimbraLog.filter.debug("Do not have permission to modify options on account %s", account.getName(), se);
            throw se;
        }
        return account;
    }

    public Domain verifyDomainPerms(DomainSelector domainSelector, ZimbraSoapContext zsc) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (domainSelector == null) {
            ServiceException se = ServiceException.INVALID_REQUEST(String.format("missing <%s>", AdminConstants.E_DOMAIN), null);
            ZimbraLog.filter.debug("DomainSelector not found", se);
            throw se;
        }
        Domain domain = prov.get(domainSelector);
        if(domain == null) {
            ServiceException se = ServiceException.FAILURE(String.format("failed to get domain"), null);
            ZimbraLog.filter.debug("DomainSelector failed to get domain - %s:%s", domainSelector.getBy().toString(), domainSelector.getKey(), se);
            throw se;
        }
        checkDomainRight(zsc, domain, Admin.R_getDomain);
        return domain;
    }

    public Cos verifyCosPerms(CosSelector cosSelector, ZimbraSoapContext zsc) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (cosSelector == null) {
            ServiceException se = ServiceException.INVALID_REQUEST(String.format("missing <%s>", AdminConstants.E_COS), null);
            ZimbraLog.filter.debug("CosSelector not found", se);
            throw se;
        }
        Cos cos = null;
        if(cosSelector.getBy().toString().equals(CosBy.id.toString())) {
            cos = prov.getCosById(cosSelector.getKey());
        } else if(cosSelector.getBy().toString().equals(CosBy.name.toString())) {
            cos = prov.getCosByName(cosSelector.getKey());
        } else {
            ServiceException se = ServiceException.INVALID_REQUEST(String.format("invalid cosby"), null);
            ZimbraLog.filter.debug("CosSelector not valid - %s:%s", cosSelector.getBy().toString(), cosSelector.getKey(), se);
            throw se;
        }
        if(cos == null) {
            ServiceException se = ServiceException.FAILURE(String.format("failed to get cos"), null);
            ZimbraLog.filter.debug("CosSelector failed to get cos - %s:%s", cosSelector.getBy().toString(), cosSelector.getKey(), se);
            throw se;
        }
        checkCosRight(zsc, cos, Admin.R_getCos);
        return cos;
    }

    public Server verifyServerPerms(ServerSelector serverSelector, ZimbraSoapContext zsc) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        if (serverSelector == null) {
            ServiceException se = ServiceException.INVALID_REQUEST(String.format("missing <%s>", AdminConstants.E_SERVER), null);
            ZimbraLog.filter.debug("ServerSelector not found", se);
            throw se;
        }
        Server server = null;
        if(serverSelector.getBy().toString().equals(com.zimbra.soap.admin.type.ServerSelector.ServerBy.id.toString())) {
            server = prov.getServerById(serverSelector.getKey());
        } else if(serverSelector.getBy().toString().equals(com.zimbra.soap.admin.type.ServerSelector.ServerBy.name.toString())) {
            server = prov.getServerByName(serverSelector.getKey());
        } else if(serverSelector.getBy().toString().equals(com.zimbra.soap.admin.type.ServerSelector.ServerBy.serviceHostname.toString())) {
            server = prov.getServerByServiceHostname(serverSelector.getKey());
        } else {
            ServiceException se = ServiceException.INVALID_REQUEST(String.format("invalid serverby"), null);
            ZimbraLog.filter.debug("ServerSelector not valid - %s:%s", serverSelector.getBy().toString(), serverSelector.getKey(), se);
            throw se;
        }
        if(server == null) {
            ServiceException se = ServiceException.FAILURE(String.format("failed to get server"), null);
            ZimbraLog.filter.debug("ServerSelector failed to get server - %s:%s", serverSelector.getBy().toString(), serverSelector.getKey(), se);
            throw se;
        }
        checkRight(zsc, server, Admin.R_getServer);
        return server;
    }
}
