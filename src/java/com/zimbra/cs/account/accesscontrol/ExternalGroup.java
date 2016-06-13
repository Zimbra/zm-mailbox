/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ZimbraACE.ExternalGroupInfo;
import com.zimbra.cs.account.cache.NamedEntryCache;
import com.zimbra.cs.account.grouphandler.GroupHandler;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.ldap.ZSearchResultEntry;

public class ExternalGroup extends NamedEntry {

    private static final NamedEntryCache<ExternalGroup> CACHE =
        new NamedEntryCache<ExternalGroup>(
                LC.ldap_cache_group_maxsize.intValue(),
                LC.ldap_cache_group_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    private final String dn;
    private final GroupHandler groupHandler;
    private final String zimbraDomainId;

    /*
     * id:   {zimbra domain id}:{external group name}
     * name: {zimbra domain name}:{external group name}
     */
    ExternalGroup(String dn, String id, String name, String zimbraDomainId,
            ZAttributes attrs, GroupHandler groupHandler, Provisioning prov)
    throws LdapException {
        super(name, id, attrs.getAttrs(), null, prov);
        this.dn = dn;
        this.groupHandler = groupHandler;
        this.zimbraDomainId = zimbraDomainId;
    }

    public String getDN() {
        return dn;
    }

    public String getZimbraDomainId() {
        return zimbraDomainId;
    }

    boolean inGroup(MailTarget mailTarget, boolean asAdmin) throws ServiceException {
        if (!(mailTarget instanceof Account)) {
            return false;
        }
        return inGroup((Account) mailTarget, asAdmin);
    }

    boolean inGroup(Account acct, boolean asAdmin) throws ServiceException {
        return groupHandler.inDelegatedAdminGroup(this, acct, asAdmin);
    }

    private static GroupHandler getGroupHandler(Domain domain) {
        String className = domain.getExternalGroupHandlerClass();
        return GroupHandler.getHandler(className);
    }

    private static ExternalGroup makeExternalGroup(Domain domain,
            GroupHandler groupHandler, String extGroupName, String dn,
            ZAttributes attrs) throws ServiceException {
        String id = ExternalGroupInfo.encode(domain.getId(), extGroupName);
        String name = ExternalGroupInfo.encode(domain.getName(), extGroupName);

        ExternalGroup extGroup = new ExternalGroup(
                dn, id, name, domain.getId(), attrs, groupHandler, LdapProv.getInst());
        return extGroup;
    }

    /*
     * domainBy: id when extGroupGrantee is obtained in fron persisted ZimbraACE
     *           name when extGroupGrantee is provided to zmprov or SOAP.
     *
     */
    static ExternalGroup get(/* AuthToken authToken, */ DomainBy domainBy,
            String extGroupGrantee, boolean asAdmin) throws ServiceException {
        ExternalGroup group = null;

        if (DomainBy.name == domainBy) {
            group = CACHE.getByName(extGroupGrantee);
        } else {
            group = CACHE.getById(extGroupGrantee);
        }

        if (group != null) {
            return group;
        }

        group = searchGroup(domainBy, extGroupGrantee, asAdmin);

        if (group != null) {
            CACHE.put(group);
        }

        return group;
    }

    private static ExternalGroup searchGroup(DomainBy domainBy, String extGroupGrantee,
            boolean asAdmin) throws ServiceException {
        LdapProv prov = LdapProv.getInst();

        ExternalGroupInfo extGrpInfo = ExternalGroupInfo.parse(extGroupGrantee);
        String zimbraDomain = extGrpInfo.getZimbraDmain();
        String extGroupName = extGrpInfo.getExternalGroupName();

        Domain domain = prov.get(domainBy, zimbraDomain);
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(zimbraDomain);
        }

        String searchBase = domain.getExternalGroupLdapSearchBase();
        String filterTemplate = domain.getExternalGroupLdapSearchFilter();

        if (searchBase == null) {
            searchBase = LdapConstants.DN_ROOT_DSE;
        }
        String searchFilter = LdapUtil.computeDn(extGroupName, filterTemplate);

        GroupHandler groupHandler = getGroupHandler(domain);

        ZLdapContext zlc = null;
        try {
            zlc = groupHandler.getExternalDelegatedAdminGroupsLdapContext(domain, asAdmin);

            ZSearchResultEntry entry = prov.getHelper().searchForEntry(
                    searchBase, FilterId.EXTERNAL_GROUP, searchFilter, zlc, new String[]{"mail"});

            if (entry != null) {
                return makeExternalGroup(domain, groupHandler, extGroupName,
                        entry.getDN(), entry.getAttributes());
            } else {
                return null;
            }
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

}
