/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.Server;

public abstract class RightBearer {
    protected NamedEntry mRightBearer;

    static RightBearer newRightBearer(NamedEntry entry) throws ServiceException {
        if (entry instanceof Account && AccessControlUtil.isGlobalAdmin((Account)entry, true)) {
            return new GlobalAdmin(entry);
        } else {
            return Grantee.getGrantee(entry);
        }
    }

    protected RightBearer(NamedEntry grantee) {
        mRightBearer = grantee;
    }

    String getId() {
        return mRightBearer.getId();
    }

    String getName() {
        return mRightBearer.getName();
    }

    static class GlobalAdmin extends RightBearer {
        private GlobalAdmin(NamedEntry grantee) throws ServiceException {
            super(grantee);
        }
    }

    // public only for unit test. TODO: cleanup unit test
    @VisibleForTesting
    public static class Grantee extends RightBearer {
        GranteeType mGranteeType;
        Domain mGranteeDomain;
        Set<String> mIdAndGroupIds;

        @VisibleForTesting
        public Grantee(NamedEntry grantee) throws ServiceException {
            this(grantee, (Set<Right>) null, true);
        }

        protected Grantee(NamedEntry grantee, boolean adminOnly) throws ServiceException {
            this(grantee, (Set<Right>) null, adminOnly);
        }

        protected Grantee(NamedEntry grantee, Set <Right> rights, boolean adminOnly) throws ServiceException {
            super(grantee);

            Provisioning prov = grantee.getProvisioning();
            GroupMembership granteeGroups = null;

            if (grantee instanceof Account) {
                mGranteeType = GranteeType.GT_USER;
                mGranteeDomain = prov.getDomain((Account)grantee);
                granteeGroups = prov.getGroupMembershipWithRights((Account)grantee, rights, adminOnly);

            } else if (grantee instanceof DistributionList) {
                mGranteeType = GranteeType.GT_GROUP;
                mGranteeDomain = prov.getDomain((DistributionList)grantee);
                granteeGroups = prov.getGroupMembership((DistributionList)grantee, adminOnly);

            } else if (grantee instanceof DynamicGroup) {
                mGranteeType = GranteeType.GT_GROUP;
                mGranteeDomain = prov.getDomain((DynamicGroup)grantee);
                // no need to get membership for dynamic groups
                // dynamic groups cannot be nested, either as a members in another
                // dynamic group or a distribution list

            } else {
                if (adminOnly) {
                    throw ServiceException.INVALID_REQUEST("invalid grantee type", null);
                } else {
                    if (grantee instanceof Domain) {
                        mGranteeType = GranteeType.GT_DOMAIN;
                        mGranteeDomain = (Domain) grantee;
                    }
                }
            }

            if (adminOnly) {
                if (!RightBearer.isValidGranteeForAdminRights(mGranteeType, grantee)) {
                    throw ServiceException.INVALID_REQUEST("invalid grantee", null);
                }
            }

            if (mGranteeDomain == null) {
                throw ServiceException.FAILURE("internal error, cannot get domain for grantee", null);
            }

            // setup grantees ids
            mIdAndGroupIds = new HashSet<String>();
            mIdAndGroupIds.add(grantee.getId());
            if (granteeGroups != null) {
                mIdAndGroupIds.addAll(granteeGroups.groupIds());
            }
        }

        protected static Grantee getGrantee(NamedEntry namedEntry) throws ServiceException {
            return getGrantee(namedEntry, (Set<Right>) null, true);
        }

        protected static Grantee getGrantee(NamedEntry namedEntry, boolean adminOnly) throws ServiceException {
            return getGrantee(namedEntry, (Set<Right>) null, adminOnly);
        }

        private static Grantee getGranteeFromCache(NamedEntry namedEntry, Set <Right> right, boolean adminOnly)
                throws ServiceException {
            Grantee grntee = null;
            final GranteeCacheKey key = new GranteeCacheKey(namedEntry, right, adminOnly);
            try {
                grntee = GRANTEE_CACHE.get(key, new Callable<Grantee>() {
                    @Override
                    public Grantee call() throws ServiceException {
                        return new Grantee(key.namedEntry, key.rights, key.adminOnly);
                    }
                });
            } catch (ExecutionException e) {
                Throwable throwable = e.getCause();
                if (throwable != null && throwable instanceof ServiceException) {
                    throw (ServiceException) throwable;
                }
                ZimbraLog.acl.debug("Unexpected escape getting from GRANTEE_CACHE", e);
            }
            return grntee;
        }

        protected static Grantee getGrantee(NamedEntry namedEntry, Set <Right> right, boolean adminOnly)
        throws ServiceException {
            if (null == GRANTEE_CACHE) {
                return new Grantee(namedEntry, right, adminOnly);
            } else {
                return getGranteeFromCache(namedEntry, right, adminOnly);
            }
        }

        boolean isAccount() {
            return mGranteeType == GranteeType.GT_USER;
        }

        Account getAccount() throws ServiceException {
            if (mGranteeType != GranteeType.GT_USER) {
                throw ServiceException.FAILURE("internal error", null);
            }
            return (Account)mRightBearer;
        }

        Domain getDomain() {
            return mGranteeDomain;
        }

        Set<String> getIdAndGroupIds() {
            return mIdAndGroupIds;
        }

        private static final Cache<GranteeCacheKey, Grantee> GRANTEE_CACHE;
        private static final long MAX_CACHE_EXPIRY = 30 * Constants.MILLIS_PER_MINUTE;

        static {
            long granteeCacheSize= 0;
            long granteeCacheExpireAfterMillis = 0;
            try {
                Server server = Provisioning.getInstance().getLocalServer();
                granteeCacheSize = server.getShortTermGranteeCacheSize();
                if (granteeCacheSize > 0) {
                    granteeCacheExpireAfterMillis = server.getShortTermGranteeCacheExpiration();
                    if (granteeCacheExpireAfterMillis < 0) {
                        granteeCacheExpireAfterMillis = 0;
                        granteeCacheSize = 0;
                    } else if (granteeCacheExpireAfterMillis > MAX_CACHE_EXPIRY) {
                        granteeCacheExpireAfterMillis = MAX_CACHE_EXPIRY;
                    }
                }
            } catch (ServiceException e) {
                granteeCacheSize = 0;
            }
            if (granteeCacheSize > 0) {
                GRANTEE_CACHE =
                        CacheBuilder.newBuilder()
                        .maximumSize(granteeCacheSize)
                        .expireAfterWrite(granteeCacheExpireAfterMillis,
                                TimeUnit.MILLISECONDS) /* regard data as potentially stale after this time */
                                .build();
                ZimbraLog.acl.trace("RightBearer GRANTEE_CACHE BUILD size=%d expire=%dms",
                        granteeCacheSize, granteeCacheExpireAfterMillis);
            } else {
                GRANTEE_CACHE = null;
            }
        }

        public static void clearGranteeCache() {
            if (null != GRANTEE_CACHE) {
                ZimbraLog.acl.debug("Clearing short term grantee cache of %d items.", GRANTEE_CACHE.size());
                GRANTEE_CACHE.invalidateAll();
            }
        }

        private final static class GranteeCacheKey {
            private final NamedEntry namedEntry;
            private final Set <Right> rights;
            private final boolean adminOnly;
            private GranteeCacheKey(NamedEntry namedEntry, Set <Right> rights, boolean adminOnly) {
                this.namedEntry = namedEntry;
                this.rights = rights;
                this.adminOnly = adminOnly;
            }

            @Override
            public boolean equals(final Object other) {
                if (!(other instanceof GranteeCacheKey)) {
                    return false;
                }
                GranteeCacheKey ogck = (GranteeCacheKey) other;
                if (adminOnly != ogck.adminOnly) {
                    return false;
                }
                if (!namedEntry.getName().equals(ogck.namedEntry.getName())) {
                    return false;
                }
                if (rights == null) {
                    return (ogck.rights == null);
                } else {
                    return rights.equals(ogck.rights);
                }
            }

            @Override
            public int hashCode() {
                int code = namedEntry.getName().hashCode() + Boolean.valueOf(adminOnly).hashCode();
                if (rights != null) {
                    code += rights.hashCode();
                }
                return code;
            }
        }
    }

    /**
     * returns true if grantee {@code gt} can be granted Admin Rights
     *
     * Note:
     *     - system admins cannot receive grants - they don't need any
     *
     * @param gt
     * @param grantee
     * @return
     */
    static boolean isValidGranteeForAdminRights(GranteeType gt, NamedEntry grantee) {
        if (gt == GranteeType.GT_USER) {
            return (!grantee.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false) &&
                    grantee.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false));
        } else if (gt == GranteeType.GT_GROUP) {
            return grantee.getBooleanAttr(Provisioning.A_zimbraIsAdminGroup, false);
        } else if (gt == GranteeType.GT_EXT_GROUP) {
            return true;
        } else {
            return false;
        }
    }

    static boolean matchesGrantee(Grantee grantee, ZimbraACE ace) throws ServiceException {
        // set of zimbraIds the grantee in question can assume: including
        //   - zimbraId of the grantee
        //   - all zimbra internal admin groups the grantee is in
        Set<String> granteeIds = grantee.getIdAndGroupIds();
        if (granteeIds.contains(ace.getGrantee())) {
            return true;
        } else if (ace.getGranteeType() == GranteeType.GT_EXT_GROUP) {
            if (grantee.isAccount()) {
                return ace.matchesGrantee(grantee.getAccount(), true);
            } else {
                // we are collecting rights for a group grantee
                // TODO
                throw ServiceException.FAILURE("Not yet implemented", null);
            }
        } else {
            return false;
        }
    }


}
