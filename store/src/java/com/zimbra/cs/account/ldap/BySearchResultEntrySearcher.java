/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.ldap;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;

public class BySearchResultEntrySearcher {
    public interface SearchEntryProcessor {
        public void processSearchEntry(ZSearchResultEntry sr);
    }

    private final LdapProvisioning prov;
    private final Domain domain;
    private final SearchEntryProcessor visitor;
    private final String[] returnAttrs;
    private final ZLdapContext zlc;
    public BySearchResultEntrySearcher(LdapProvisioning prov, ZLdapContext zlc, Domain domain,
            String [] retAttrs, SearchEntryProcessor visitor) {
        this.prov = prov;
        this.zlc = zlc;
        this.domain = domain;
        this.returnAttrs = retAttrs;
        this.visitor = visitor;
    }

    public void doSearch(ZLdapFilter filter, Set<ObjectType> types) throws ServiceException {
        String[] bases = prov.getSearchBases(domain, types);
        for (String base : bases) {
            try {
                ZSearchControls ctrl = ZSearchControls.createSearchControls(ZSearchScope.SEARCH_SCOPE_SUBTREE,
                        ZSearchControls.SIZE_UNLIMITED, returnAttrs);
                ZSearchResultEnumeration results =
                        prov.getHelper().searchDir(base, filter, ctrl, zlc, LdapServerType.REPLICA);
                while(results.hasMore()) {
                    ZSearchResultEntry sr = results.next();
                    visitor.processSearchEntry(sr);
                }
                results.close();
            } catch (ServiceException e) {
                ZimbraLog.search.debug("Unexpected exception whilst searching", e);
            }
        }
    }
}
