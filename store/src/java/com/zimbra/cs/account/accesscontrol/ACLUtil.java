/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.generated.UserRights;

public final class ACLUtil {
    private static final String ACL_CACHE_KEY = "ENTRY.ACL_CACHE";

    private ACLUtil() {
    }

    /**
     * Returns all ACEs granted on the entry.
     *
     * @param entry the entry on which rights are granted
     * @return all ACEs granted on the entry.
     */
    public static List<ZimbraACE> getAllACEs(Entry entry) throws ServiceException {
        ZimbraACL acl = getACL(entry);
        return acl != null ? acl.getAllACEs() : null;
    }

    public static Set<ZimbraACE> getAllowedNotDelegableACEs(Entry entry)
    throws ServiceException {
        ZimbraACL acl = getACL(entry);
        return acl != null ? acl.getAllowedNotDelegableACEs() : null;
    }

    public static Set<ZimbraACE> getAllowedDelegableACEs(Entry entry)
    throws ServiceException {
        ZimbraACL acl = getACL(entry);
        return acl != null ? acl.getAllowedDelegableACEs() : null;
    }

    public static Set<ZimbraACE> getDeniedACEs(Entry entry) throws
    ServiceException {
        ZimbraACL acl = getACL(entry);
        return acl != null ? acl.getDeniedACEs() : null;
    }

    /**
     * Returns a Set of ACEs with the specified rights granted on the entry.
     *
     * @param entry the entry on which rights are granted
     * @param rights rights of interest
     * @return a Set of ACEs with the specified rights granted on the entry.
     */
    public static List<ZimbraACE> getACEs(Entry entry, Set<? extends Right> rights)
    throws ServiceException {
        ZimbraACL acl = getACL(entry);
        return acl != null ? acl.getACEs(rights) : null;
    }

    private static Multimap<Right, Entry> getGrantedRights(Account grantee, Set<String> fetchAttrs)
            throws ServiceException {
        SearchGrants search = new SearchGrants(grantee.getProvisioning(), EnumSet.of(TargetType.account),
                RightBearer.Grantee.getGrantee(grantee, false).getIdAndGroupIds());
        search.addFetchAttribute(fetchAttrs);
        Set<SearchGrants.GrantsOnTarget> results = search.doSearch().getResults();
        Multimap<Right, Entry> map = HashMultimap.create();
        for (SearchGrants.GrantsOnTarget grants : results) {
            ZimbraACL acl = grants.getAcl();
            for (ZimbraACE ace : acl.getAllACEs()) {
                if (ace.getGrantee().equals(grantee.getId())) {
                    map.put(ace.getRight(), grants.getTargetEntry());
                }
            }
        }
        return map;
    }

    /**
     * Returns {@link UserRights#R_sendOnBehalfOf} rights granted to the grantee.
     */
    public static List<Identity> getSendOnBehalfOf(Account grantee) throws ServiceException {
        Multimap<Right, Entry> rights = getGrantedRights(grantee, Collections.singleton(Provisioning.A_displayName));
        ImmutableList.Builder<Identity> result = ImmutableList.<Identity>builder();
        for (Entry entry : rights.get(UserRights.R_sendOnBehalfOf)) {
            Account grantor = (Account) entry;
            String mail = grantor.getName();
            String name = MoreObjects.firstNonNull(grantor.getDisplayName(), mail);
            Map<String, Object> attrs = ImmutableMap.<String, Object>builder()
                .put(Provisioning.A_zimbraPrefIdentityId, grantor.getId())
                .put(Provisioning.A_zimbraPrefIdentityName, name)
                .put(Provisioning.A_zimbraPrefFromDisplay, name)
                .put(Provisioning.A_zimbraPrefFromAddress, mail)
                .put(Provisioning.A_objectClass, AttributeClass.OC_zimbraAclTarget)
                .build();
            result.add(new Identity(grantee, name, grantor.getId(), attrs, grantee.getProvisioning()));
        }
        return result.build();
    }


    /**
     * Grant rights on a target entry.
     */
    public static List<ZimbraACE> grantRight(Provisioning prov, Entry target, Set<ZimbraACE> aces)
    throws ServiceException {
        for (ZimbraACE ace : aces) {
            ZimbraACE.validate(ace);
        }
        ZimbraACL acl = getACL(target, Boolean.TRUE);
        List<ZimbraACE> granted = null;

        if (acl == null) {
            acl = new ZimbraACL(aces);
            granted = acl.getAllACEs();
        } else {
            // Make a copy so we don't interfere with others that are using the acl.
            // This instance of acl will never be used in any AccessManager code path.
            // It only lives within this method for serialization.
            // serialize will erase the cached ZimbraACL object on the target object.
            // The new ACL will be loaded when it is needed.
            acl = acl.clone();
            granted = acl.grantAccess(aces);
        }

        serialize(prov, target, acl);

        PermissionCache.invalidateCache(target);

        return granted;
    }

    /**
     * Revoke(remove) rights from a target entry.
     * If a right was not previously granted on the target, NO error is thrown.
     * @return a Set of grants that are actually revoked by this call
     */
    public static List<ZimbraACE> revokeRight(Provisioning prov, Entry target, Set<ZimbraACE> aces)
    throws ServiceException {
        ZimbraACL acl = getACL(target, Boolean.TRUE);
        if (acl == null) {
            return new ArrayList<ZimbraACE>(); // return empty list
        }
        // Make a copy so we don't interfere with others that are using the acl.
        // This instance of acl will never be used in any AccessManager code path.
        // It only lives within this method for serialization.
        // serialize will erase the cached ZimbraACL object on the target object.
        // The new ACL will be loaded when it is needed.
        acl = acl.clone();
        List<ZimbraACE> revoked = acl.revokeAccess(aces);
        serialize(prov, target, acl);

        PermissionCache.invalidateCache(target);

        return revoked;
    }

    /**
     * Persists grants in LDAP
     */
    private static void serialize(Provisioning prov, Entry entry, ZimbraACL acl)
    throws ServiceException {
        // modifyAttrs will erase cached ACL and permission cache on the target
        prov.modifyAttrs(entry, Collections.singletonMap(Provisioning.A_zimbraACE, acl.serialize()));
    }

    /**
     * Get cached grants, if not in cache, load from LDAP.
     *
     * @param entry
     * @return
     * @throws ServiceException
     */
    static ZimbraACL getACL(Entry entry) throws ServiceException {
        return getACL(entry, Boolean.FALSE);
    }
    /**
     * Get cached grants, if not in cache, load from LDAP.
     *
     * @param entry the LDAP entry object for which we need ACLs
     * @param loadFromLdap when true we always load from LDAP, when false we try the cache first.
     * @return
     * @throws ServiceException
     */
    static ZimbraACL getACL(Entry entry, boolean loadFromLdap) throws ServiceException {
        ZimbraACL acl = null;
        if (!loadFromLdap) {
            acl = (ZimbraACL) entry.getCachedData(ACL_CACHE_KEY);
        }
        if (acl != null) {
            return acl;
        } else {
            acl = null;
            String[] aces = entry.getMultiAttr(Provisioning.A_zimbraACE);
            if (aces.length == 0) {
                return null;
            } else {
                acl = new ZimbraACL(aces, TargetType.getTargetType(entry), entry.getLabel());
                entry.setCachedData(ACL_CACHE_KEY, acl);
            }
        }
        return acl;
    }

}
