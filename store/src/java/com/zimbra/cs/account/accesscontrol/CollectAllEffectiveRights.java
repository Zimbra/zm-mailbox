/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightBearer.GlobalAdmin;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.account.accesscontrol.RightCommand.AllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.SearchGrants.GrantsOnTarget;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapTODO.ACLTODO;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.soap.type.TargetBy;

@ACLTODO   // check refs of targetType.dl and handle the same for group
public class CollectAllEffectiveRights {


    /*
     * represents a "shape" of groups.  Members in the same "shape" belong to
     * all groups in the shape.
     *
     * e.g. if we are calculating shapes for group A, B, C, D, possible shapes are:
     *      A, B, C, D, AB, AC, AD, BC, BD, CD, ABC, ABD, ACD, BCD, ABCD
     *
     *      If groups are shaped in the order of A, B, C, D, the resulting shapes(at most) would look like:
     *      (numbers are the order a shape is spawn.)
     *
     *      when members in group A is being shaped: A(1)
     *      when members in group B is being shaped: A(1)                      AB(2)                           B(3)
     *      when members in group C is being shaped: A(1)        AC(4)         AB(2)          ABC(5)           B(3)         BC(6)          C(7)
     *      when members in group D is being shaped: A(1) AD(8)  AC(4) ACD(9)  AB(2) ABD(10)  ABC(5) ABCD(11)  B(3) BD(12)  BC(6) BCD(13)  C(7) CD(14)  D(15)
     *
     */
    public static class GroupShape {
        Set<String> mGroups = new HashSet<String>();   // groups all entries in mMembers are a member of
        Set<String> mMembers = new HashSet<String>();  // members belongs to all entries of mGroups

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (String group : mGroups) {
                sb.append("group " + group + "\n");
            }
            for (String member : mMembers) {
                sb.append("    member " + member + "\n");
            }
            return sb.toString();
        }

        private static void debug(String msg, Set<GroupShape> shapes) {
            if (ZimbraLog.acl.isDebugEnabled()) {
                for (GroupShape shape : shapes) {
                    ZimbraLog.acl.debug(msg + "\n" + shape.toString());
                }
            }
        }

        private void addGroups(Set<String> groups) {
            mGroups.addAll(groups);
        }

        private void addGroup(String group) {
            mGroups.add(group);
        }

        public Set<String> getGroups() {
            return mGroups;
        }

        private void addMembers(Set<String> members) {
            mMembers.addAll(members);
        }

        private void addMember(String member) {
            mMembers.add(member);
        }

        public Set<String> getMembers() {
            return mMembers;
        }

        private boolean removeMemberIfPresent(String member) {
            if (mMembers.contains(member)) {
                mMembers.remove(member);
                return true;
            } else
                return false;
        }

        private boolean hasMembers() {
            return !mMembers.isEmpty();
        }

        public static void shapeMembers(TargetType targetType, Set<GroupShape> shapes,
                AllGroupMembers group)
        throws ServiceException {

            // Stage newly spawn GroupShape's in a separate Set so
            // we don't add entries to shapes while iterating through it.
            // Add entries in the new Set back into shapes after iterating
            // through it for this group.
            //
            Set<GroupShape> newShapes = new HashSet<GroupShape>();

            // holds members in the group being shaped that
            // do not belong to any shape in the current discovered shapes
            GroupShape newShape = new GroupShape();
            newShape.addGroup(group.getGroupName());
            newShape.addMembers(group.getMembers(targetType));

            for (GroupShape shape : shapes) {
                // holds intersect of members in this shape and
                // in the group being shaped.
                GroupShape intersectShape = new GroupShape();

                for (String member : group.getMembers(targetType)) {
                    if (shape.removeMemberIfPresent(member)) {
                        intersectShape.addMember(member);
                        newShape.removeMemberIfPresent(member);
                    }
                }

                if (intersectShape.hasMembers()) {
                    // found a new shape

                    // describe it
                    intersectShape.addGroups(shape.getGroups());
                    intersectShape.addGroup(group.getGroupName());

                    // keep it
                    newShapes.add(intersectShape);
                }
                // no intersect, toss the intersectShape
            }

            // add newly spawn GroupShape's in shapes
            shapes.addAll(newShapes);

            // add the new shape that contain members that are not in
            // any other shapes
            if (newShape.hasMembers()) {
                shapes.add(newShape);
            }
        }
    }

    public static class AllGroupMembers {
        String mGroupName; // name of the group

        Set<String> mAccounts = new HashSet<String>();  // all account members of the group
        Set<String> mCalendarResources = new HashSet<String>(); // all calendar resource memners of the group
        Set<String> mDistributionLists = new HashSet<String>(); // all distribution list members of the group

        AllGroupMembers(String groupName) {
            mGroupName = groupName;
        }

        String getGroupName() {
            return mGroupName;
        }

        Set<String> getMembers(TargetType targetType) throws ServiceException {
            switch (targetType) {
            case account:
                return mAccounts;
            case calresource:
                return mCalendarResources;
            case dl:
                return mDistributionLists;
            }
            throw ServiceException.FAILURE("internal error", null);
        }
    }

    private static class Visitor extends SearchLdapVisitor {
        private final LdapProv mProv;
        private final LdapDIT mLdapDIT;

        // set of names
        private final Set<String> mNames = new HashSet<String>();

        Visitor(LdapProv prov) {
            super(false);
            mProv = prov;
            mLdapDIT = mProv.getDIT();
        }

        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            try {
                String name = mLdapDIT.dnToEmail(dn, ldapAttrs);
                mNames.add(name);
            } catch (ServiceException e) {
                ZimbraLog.acl.warn("cannot get name from dn [" + dn + "]", e);
            }
        }

        private Set<String> getResult() {
            return mNames;
        }
    }


    private final RightBearer mRightBearer;
    private Grantee mGrantee;
    private final boolean mExpandSetAttrs;
    private final boolean mExpandGetAttrs;
    private final AllEffectiveRights mResult;
    private final LdapProv mProv;

    static void getAllEffectiveRights(RightBearer rightBearer,
            boolean expandSetAttrs, boolean expandGetAttrs,
            AllEffectiveRights result) throws ServiceException {

        CollectAllEffectiveRights caer =
            new CollectAllEffectiveRights(rightBearer, expandSetAttrs,  expandGetAttrs, result);
        caer.collect();
    }

    public CollectAllEffectiveRights(RightBearer rightBearer,
            boolean expandSetAttrs, boolean expandGetAttrs,
            AllEffectiveRights result) throws ServiceException {

        mRightBearer = rightBearer;

        if (mRightBearer instanceof Grantee) {
            mGrantee = (Grantee)mRightBearer;
        }

        mExpandSetAttrs = expandSetAttrs;
        mExpandGetAttrs = expandGetAttrs;
        mResult = result;

        mProv = LdapProv.getInst();
    }

    private void collect() throws ServiceException {

        if (mRightBearer instanceof GlobalAdmin) {
            for (TargetType tt : TargetType.values()) {
                EffectiveRights er = new EffectiveRights(
                        tt.getCode(), null, null,
                        mRightBearer.getId(), mRightBearer.getName());

                Entry target;
                if (TargetType.config == tt) {
                    target = mProv.getConfig();
                } else if (TargetType.global == tt) {
                    target = mProv.getGlobalGrant();
                } else {
                    target = PseudoTarget.createPseudoTarget(mProv, tt, null, null, true, null, null, null);
                }
                CollectEffectiveRights.getEffectiveRights(mRightBearer, target, tt, mExpandSetAttrs, mExpandGetAttrs, er);
                mResult.setAll(tt, er);
            }
            return;
        }

        // we want all target types
        Set<TargetType> targetTypesToSearch = new HashSet<TargetType>(Arrays.asList(TargetType.values()));

        // get the set of zimbraId of the grantees to search for
        Set<String> granteeIdsToSearch = mGrantee.getIdAndGroupIds();

        // add external group grants that *may* apply
        if (mGrantee.isAccount()) {
            Domain domain = mProv.getDomain(mGrantee.getAccount());
            granteeIdsToSearch.add(ZimbraACE.ExternalGroupInfo.encode(domain.getId(), ""));
        }

        SearchGrants searchGrants = new SearchGrants(mProv, targetTypesToSearch, granteeIdsToSearch);
        Set<GrantsOnTarget> grantsOnTargets = searchGrants.doSearch().getResults(true);

        // staging for group grants
        Set<Group> groupsWithGrants = new HashSet<Group>();

        //
        // Stage1
        //
        // process grants granted on inheritable entries:
        //     globalgrant - populate the "all" field in AllEffectiveRights
        //     domains     - populate the "all entries in this domain" field in AllEffectiveRights
        //     groups      - remember the groups and process them in stage 2.
        //
        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
            ZimbraACL acl = grantsOnTarget.getAcl();
            TargetType targetType = TargetType.getTargetType(grantedOnEntry);

            if (targetType == TargetType.global) {
                computeRightsInheritedFromGlobalGrant();
            } else if (targetType == TargetType.domain) {
                computeRightsInheritedFromDomain((Domain)grantedOnEntry);
                computeSubDomainRightsInheritedFromDomain(acl, (Domain)grantedOnEntry);
            } else if (targetType == TargetType.dl) {
                groupsWithGrants.add((DistributionList)grantedOnEntry);
            } else if (targetType == TargetType.group) {
                groupsWithGrants.add((DynamicGroup)grantedOnEntry);
            }
        }

        //
        // Stage 2
        //
        // process group grants
        //
        // first, shape all members in all groups with grants into "shapes"
        //
        // e.g. if the grant search returned three groups: A, B, C
        //      group A contains members m1, m2, m3
        //      group B contains members m2, m3, m4
        //      group C contains members m5
        //
        //      (assuming all m{X} are accounts)
        //
        //      After "shaping", the accountShapes Set will contain 4 shapes:
        //      shape A  - m1
        //      shape AB - m2, m3
        //      shape B  - m4
        //      shape C  - m5
        //

        /*
         * because of bug 68820, we have to also take into accounts all sub groups
         * of groupsWithGrants when we build shapes - even if the sub groups don't
         * have any grants.
         *
         * Prior to bug 68820, we didn't have to do this(i.e. add in sub groups
         * that don't have any grants when shapes are computed), because sub groups
         * dont't have grants would never affect how grants are inherited - all grants
         * get inherited to sub groups and their member accounts/crs.
         *
         * But bug 68820 introduced a new right modifier - DISINHERIT_SUB_GROUPS,
         * that controls whether a grant on a group can be inherited by sub groups and
         * their account/cr members.
         *
         * Now the input groups for calculating shapes are:
         * union of (groups have grants and all their sub groups)
         *
         * This will result in more shares than before if non ofthe sub groups has grants,
         * but if spawned shapes actually have the same effective rights, they will be
         * merged by RightsByTargetType.addAggregation(), in that it checks if ther are
         * already an aggregation with the exact the same right.  If there are, then just
         * add the targets to the existing aggregation, instead of adding new ones.
         */
        Set<String> processedGroups = new HashSet<String>();

        Set<GroupShape> accountShapes = new HashSet<GroupShape>();
        Set<GroupShape> calendarResourceShapes = new HashSet<GroupShape>();
        Set<GroupShape> distributionListShapes = new HashSet<GroupShape>();

        for (Group group : groupsWithGrants) {
            String groupName = group.getName().toLowerCase();
            if (processedGroups.contains(groupName)) {
                continue;
            } else {
                processedGroups.add(groupName);
            }

            if (group.isDynamic()) {
                DynamicGroup dg = (DynamicGroup)group;
                if (dg.isMembershipDefinedByCustomURL()) {
                    // cannot search members of dynamic group with custom memberURL, so do not process this group.
                    ZimbraLog.acl.debug("Dynamic group with custom memberURL found '%s', ignoring further process on same.", dg.getName());
                    continue;
                }
            }

            AllGroupMembers allMembers = getAllGroupMembers(group);
            GroupShape.shapeMembers(TargetType.account, accountShapes, allMembers);
            GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembers);
            GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembers);
            // no need to get TargetType.group members of the group, because
            // dynamic group cannot be a member of a Distribution list or another
            // dynamic group

            processedGroups.add(group.getId());

            /*
             * handle sub groups.  allMembers already contains a flat set of all members
             * of group that is a DistributionList, just go through the flat set and compute
             * shares for each.  If group is a dynamic group, we should never get into
             * the following loop, because there should be no nested groups member of
             * dynamic group.
             */
            for (String nestedGoupMember : allMembers.getMembers(TargetType.dl)) {
                String nestedGoupMemberName = nestedGoupMember.toLowerCase();
                if (processedGroups.contains(nestedGoupMemberName)) {
                    continue;
                } else {
                    processedGroups.add(nestedGoupMemberName);
                }

                DistributionList subDl = mProv.get(DistributionListBy.name, nestedGoupMemberName);
                // sanity check, shout not be null
                if (subDl != null) {
                    AllGroupMembers allMembersOfSubDl = getAllGroupMembers(subDl);
                    GroupShape.shapeMembers(TargetType.account, accountShapes, allMembersOfSubDl);
                    GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembersOfSubDl);
                    GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembersOfSubDl);
                }
            }
        }

        if (ZimbraLog.acl.isDebugEnabled()) {
            GroupShape.debug("accountShapes", accountShapes);
            GroupShape.debug("calendarResourceShapes", calendarResourceShapes);
            GroupShape.debug("distributionListShapes", distributionListShapes);
        }

        // then, for each group shape, generate a RightAggregation and record in the AllEffectiveRights.
        // if any of the entries in a shape also have grants as an individual, the effective rigths for
        // those entries will be replaced in stage 3.
        Set<String> entryIdsHasGrants = new HashSet<String>();
        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
            if (grantedOnEntry instanceof NamedEntry) {
                entryIdsHasGrants.add(((NamedEntry)grantedOnEntry).getId());
            }
        }

        computeRightsOnGroupShape(TargetType.account, accountShapes,entryIdsHasGrants);
        computeRightsOnGroupShape(TargetType.calresource, calendarResourceShapes, entryIdsHasGrants);
        computeRightsOnGroupShape(TargetType.dl, distributionListShapes, entryIdsHasGrants);

        //
        // Stage 3
        //
        // process grants on the granted entry
        //
        for (GrantsOnTarget grantsOnTarget : grantsOnTargets) {
            Entry grantedOnEntry = grantsOnTarget.getTargetEntry();
            ZimbraACL acl = grantsOnTarget.getAcl();
            TargetType targetType = TargetType.getTargetType(grantedOnEntry);

            if (targetType != TargetType.global) {
                computeRightsOnEntry(targetType, grantedOnEntry);
            }
        }
    }

    public AllGroupMembers getAllGroupMembers(Group group) throws ServiceException {
        /*
         * get all groups and crs in the front and use the Set.contains method
         * to test if a member name is a cr/group
         *
         * much more efficient than doing Provisioning.get(...), which has a lot
         * more overhead and may cause extra LDAP searches if the entry is not in cache.
         */
        Set<String> allGroups = getAllGroups();
        Set<String> allCalendarResources = getAllCalendarResources();

        AllGroupMembers allMembers = new AllGroupMembers(group.getName());
        getAllGroupMembers(group, allGroups, allCalendarResources, allMembers);

        return allMembers;
    }

    private void getAllGroupMembers(Group group, Set<String> allGroups,
            Set<String> allCalendarResources, AllGroupMembers result)
    throws ServiceException {

        Set<String> members = group.getAllMembersSet();
        Set<String> accountMembers = new HashSet<String>(members);  // make a copy, assuming all members are account

        // expand if a member is a group
        for (String member : members) {
            // if member is a group, remove it from the result
            // and expand the group if it has not been expanded yet
            // In this case member must be a DistributionList, it
            // must be a Dynamic group.
            if (allGroups.contains(member)) {
                // remove it from the accountMembers
                accountMembers.remove(member);

                // haven't expand this group yet
                if (!result.getMembers(TargetType.dl).contains(member)) {
                    result.getMembers(TargetType.dl).add(member);
                    DistributionList grp = mProv.get(DistributionListBy.name, member);
                    if (grp != null) {
                        // collect members recursively
                        getAllGroupMembers(grp, allGroups, allCalendarResources, result);
                    }
                }
            } else if (allCalendarResources.contains(member)) {
                accountMembers.remove(member);
                result.getMembers(TargetType.calresource).add(member);
            }
        }
        result.getMembers(TargetType.account).addAll(accountMembers);
    }

    private Set<String> getAllGroups() throws ServiceException {
        LdapDIT ldapDIT = mProv.getDIT();
        String base = ldapDIT.mailBranchBaseDN();
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().allGroups();

        // hack, see LDAPDIT.dnToEmail, for now we get naming rdn for both default and possible custom DIT
        String[] returnAttrs = new String[] {Provisioning.A_cn, Provisioning.A_uid};

        Visitor visitor = new Visitor(mProv);
        mProv.searchLdapOnMaster(base, filter, returnAttrs, visitor);
        return visitor.getResult();
    }

    private Set<String> getAllCalendarResources() throws ServiceException {
        LdapDIT ldapDIT = mProv.getDIT();
        String base = ldapDIT.mailBranchBaseDN();
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().allCalendarResources();

        // hack, see LDAPDIT.dnToEmail, for now we get naming rdn for both default and possible custom DIT
        String[] returnAttrs = new String[] {Provisioning.A_cn, Provisioning.A_uid};

        Visitor visitor = new Visitor(mProv);
        mProv.searchLdapOnMaster(base, filter, returnAttrs, visitor);
        return visitor.getResult();
    }

    private void computeRightsInheritedFromGlobalGrant() throws ServiceException {

        for (TargetType tt : TargetType.values()) {
            Entry targetEntry;
            if (tt == TargetType.global) {
                targetEntry = mProv.getGlobalGrant();
            } else if (tt == TargetType.config) {
                targetEntry = mProv.getConfig();
            } else {
                targetEntry = PseudoTarget.createPseudoTarget(mProv, tt, null, null, true, null, null);
            }

            EffectiveRights er = new EffectiveRights(
                    tt.getCode(), TargetType.getId(targetEntry), targetEntry.getLabel(),
                    mGrantee.getId(), mGrantee.getName());

            CollectEffectiveRights.getEffectiveRights(mGrantee, targetEntry, mExpandSetAttrs, mExpandGetAttrs, er);

            mResult.setAll(tt, er);
        }
    }

    private void computeRightsInheritedFromDomain(Domain grantedOnDomain) throws ServiceException {

        computeRightsInheritedFromDomain(TargetType.account, grantedOnDomain);

        computeRightsInheritedFromDomain(TargetType.calresource, grantedOnDomain);

        computeRightsInheritedFromDomain(TargetType.dl, grantedOnDomain);

        computeRightsInheritedFromDomain(TargetType.group, grantedOnDomain);
    }

    private void computeRightsInheritedFromDomain(TargetType targetType, Domain grantedOnDomain)
    throws ServiceException {

        String domainId = TargetType.getId(grantedOnDomain);
        String domainName = grantedOnDomain.getLabel();

        // create a pseudo object(account, cr, dl) in this domain
        Entry pseudoTarget = PseudoTarget.createPseudoTarget(
                mProv, targetType, DomainBy.id, grantedOnDomain.getId(),
                false, null, null);

        // get effective rights on the pseudo target
        EffectiveRights er = new EffectiveRights(
                targetType.getCode(), TargetType.getId(pseudoTarget), pseudoTarget.getLabel(),
                mGrantee.getId(), mGrantee.getName());
        CollectEffectiveRights.getEffectiveRights(mGrantee, pseudoTarget, mExpandSetAttrs, mExpandGetAttrs, er);

        // add to the domianed scope in AllEffectiveRights
        mResult.addDomainEntry(targetType, domainName, er);
    }

    private void computeSubDomainRightsInheritedFromDomain(ZimbraACL acl, Domain grantedOnDomain)
    throws ServiceException {

        boolean noSubDomainGrants = acl.getSubDomainACEs().isEmpty();
        if (noSubDomainGrants) {
            return;
        }

        // get all sub domains of the grantedOnDomain
        List<Domain> subDomains = searchSubDomains(grantedOnDomain);

        for (Domain subDomain : subDomains) {
            String targetId = subDomain.getId();
            String targetName = subDomain.getName();

            EffectiveRights er = new EffectiveRights(
                    TargetType.domain.getCode(), targetId, targetName,
                    mGrantee.getId(), mGrantee.getName());

            CollectEffectiveRights.getEffectiveRights(mGrantee, subDomain,
                    mExpandSetAttrs, mExpandGetAttrs, er);
            mResult.addEntry(TargetType.domain, targetName, er);
        }
    }

    private List<Domain> searchSubDomains(Domain domain) throws ServiceException {

        List<Domain> subDomains = new ArrayList<Domain>();

        String base = mProv.getDIT().domainNameToDN(domain.getName());
        ZLdapFilter filter = ZLdapFilterFactory.getInstance().allDomains();
        String returnAttrs[] = new String[] {Provisioning.A_zimbraId};
        SearchSubDomainVisitor visitor = new SearchSubDomainVisitor();

        mProv.searchLdapOnMaster(base, filter, returnAttrs, visitor);

        List<String> zimbraIds = visitor.getResults();
        for (String zimbraId : zimbraIds) {
            if (zimbraId.equalsIgnoreCase(domain.getId())) {
                // the search returns the domain itself too, skip it
                continue;
            }

            try {
                Domain subDomain = (Domain)TargetType.lookupTarget(mProv,
                        TargetType.domain, TargetBy.id, zimbraId);
                subDomains.add(subDomain);
            } catch (ServiceException e) {
                ZimbraLog.acl.warn("canot find domain by id " + zimbraId, e);
            }
        }

        return subDomains;
    }

    private static class SearchSubDomainVisitor extends SearchLdapVisitor {
        List<String> mDomainIds = new ArrayList<String>();

        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            String zimbraId = (String)attrs.get(Provisioning.A_zimbraId);
            if (zimbraId != null) {
                mDomainIds.add(zimbraId);
            }
        }

        private List<String> getResults() {
            return mDomainIds;
        }
    }


    /*
     * We do not have a group scope in AllEffectiveRights.
     *
     * Reasons:
     *     1. If we return something like:
     *           have effective rights X, Y, Z on members in groups A, B, C
     *           have effective rights P, Q, R on members in groups M, N
     *        then client will have to figure out if an account/cr/dl are in which groups.
     *
     *     2. If a group-ed(i.e. account/cr/dl) are in multiple groups, that's even messier
     *        for the client (admin console).
     *
     * Instead, we classify group-ed entries in groups with grants into "shapes", and
     * represent them in a RightAggregation, like:
     *       - has effective rights X, Y on accounts user1, user5, user8
     *       - has effective rights X on accounts user2, user3, user4
     *       - has effective rights on calendar resources cr1, cr88
     *       - has effective rights on distribution lists dl38, dl99
     */
    private void computeRightsOnGroupShape(TargetType targetType, Set<GroupShape> groupShapes,
            Set<String> entryIdsHasGrants) throws ServiceException {

        for (GroupShape shape : groupShapes) {
            // get any one member in the shape and use that as a pilot target to get
            // an EffectiveRights.  Note, the pilot target entry itself cannot have
            // any grants or else it will not result in the same EffectiveRights for
            // the group shape.  Entries have grants will be recorded in stage 3; and
            // will overwrite the entry rights recorded here.
            //
            // if for some reason the member cannot be found (e.g. account is deleted
            // but somehow not removed from a group, l=not likely though), just skip
            // to use another one in the shape.
            //
            //

            Entry target = null;
            EffectiveRights er = null;
            for (String memberName : shape.getMembers()) {
                target = TargetType.lookupTarget(mProv, targetType, TargetBy.name, memberName, false);
                if (target != null) {
                    String targetId = TargetType.getId(target);
                    if (!entryIdsHasGrants.contains(targetId)) {
                        er = new EffectiveRights(
                                targetType.getCode(), targetId, target.getLabel(),
                                mGrantee.getId(), mGrantee.getName());
                        CollectEffectiveRights.getEffectiveRights(mGrantee, target,
                                mExpandSetAttrs, mExpandGetAttrs, er);
                        break;
                    } // else the member itself has grants, skip it for being used as a pilot target entry
                }
            }

            if (er != null) {
                mResult.addAggregation(targetType, shape.getMembers(), er);
        }
    }
    }

    private void computeRightsOnEntry(TargetType grantedOnTargetType, Entry grantedOnEntry)
    throws ServiceException {
        String targetId = TargetType.getId(grantedOnEntry);
        String targetName = grantedOnEntry.getLabel();

        EffectiveRights er = new EffectiveRights(
                grantedOnTargetType.getCode(), targetId, targetName,
                mGrantee.getId(), mGrantee.getName());

        CollectEffectiveRights.getEffectiveRights(mGrantee, grantedOnEntry,
                mExpandSetAttrs, mExpandGetAttrs, er);
        mResult.addEntry(grantedOnTargetType, targetName, er);
    }

    /*
     * ==========
     * unit tests
     * ==========
     */
    private static AllGroupMembers allGroupMembers(DistributionList dl) throws ServiceException {
        CollectAllEffectiveRights caer = new CollectAllEffectiveRights(null, false, false, null);
        return caer.getAllGroupMembers(dl);
    }

    private static void groupTest() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        DistributionList dl = prov.get(DistributionListBy.name, "group1@phoebe.mac");

        AllGroupMembers allMembers = allGroupMembers(dl);

        System.out.println("\naccounts");
        for (String member : allMembers.getMembers(TargetType.account))
            System.out.println("  " + member);

        System.out.println("\ncalendar resources");
        for (String member : allMembers.getMembers(TargetType.calresource))
            System.out.println("  " + member);

        System.out.println("\ngroups");
        for (String member : allMembers.getMembers(TargetType.dl))
            System.out.println("  " + member);
    }

    private static void setupShapeTest1() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        // create test
        String domainName = "test.com";
        Domain domain = prov.createDomain(domainName, new HashMap<String, Object>());

        DistributionList groupA = prov.createDistributionList("groupA@"+domainName, new HashMap<String, Object>());
        DistributionList groupB = prov.createDistributionList("groupB@"+domainName, new HashMap<String, Object>());
        DistributionList groupC = prov.createDistributionList("groupC@"+domainName, new HashMap<String, Object>());
        DistributionList groupD = prov.createDistributionList("groupD@"+domainName, new HashMap<String, Object>());

        String pw = "test123";
        Account A = prov.createAccount("A@"+domainName, pw, null);
        Account B = prov.createAccount("B@"+domainName, pw, null);
        Account C = prov.createAccount("C@"+domainName, pw, null);
        Account D = prov.createAccount("D@"+domainName, pw, null);
        Account AB = prov.createAccount("AB@"+domainName, pw, null);
        Account AC = prov.createAccount("AC@"+domainName, pw, null);
        Account AD = prov.createAccount("AD@"+domainName, pw, null);
        Account BC = prov.createAccount("BC@"+domainName, pw, null);
        Account BD = prov.createAccount("BD@"+domainName, pw, null);
        Account CD = prov.createAccount("CD@"+domainName, pw, null);
        Account ABC = prov.createAccount("ABC@"+domainName, pw, null);
        Account ABD = prov.createAccount("ABD@"+domainName, pw, null);
        Account ACD = prov.createAccount("ACD@"+domainName, pw, null);
        Account BCD = prov.createAccount("BCD@"+domainName, pw, null);
        Account ABCD = prov.createAccount("ABCD@"+domainName, pw, null);

        groupA.addMembers(new String[]{A.getName(),
                                       AB.getName(), AC.getName(), AD.getName(),
                                       ABC.getName(), ABD.getName(), ACD.getName(),
                                       ABCD.getName()});

        groupB.addMembers(new String[]{B.getName(),
                                       AB.getName(), BC.getName(), BD.getName(),
                                       ABC.getName(), ABD.getName(), BCD.getName(),
                                       ABCD.getName()});

        groupC.addMembers(new String[]{C.getName(),
                                       AC.getName(), BC.getName(), CD.getName(),
                                       ABC.getName(), ACD.getName(), BCD.getName(),
                                       ABCD.getName()});

        groupD.addMembers(new String[]{D.getName(),
                                       AD.getName(), BD.getName(), CD.getName(),
                                       ABD.getName(), ACD.getName(), BCD.getName(),
                                       ABCD.getName()});
    }

    private static void shapeTest1() throws ServiceException {
        setupShapeTest1();

        Provisioning prov = Provisioning.getInstance();

        // create test
        Set<DistributionList> groupsWithGrants = new HashSet<DistributionList>();
        String domainName = "test.com";
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupA@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupB@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupC@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupD@"+domainName));

        Set<GroupShape> accountShapes = new HashSet<GroupShape>();
        Set<GroupShape> calendarResourceShapes = new HashSet<GroupShape>();
        Set<GroupShape> distributionListShapes = new HashSet<GroupShape>();

        for (DistributionList group : groupsWithGrants) {
            // group is an AclGroup, which contains only upward membership, not downward membership.
            // re-get the DistributionList object, which has the downward membership.
            DistributionList dl = prov.get(DistributionListBy.id, group.getId());
            AllGroupMembers allMembers = allGroupMembers(dl);
            GroupShape.shapeMembers(TargetType.account, accountShapes, allMembers);
            GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembers);
            GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembers);
        }

        int count = 1;
        for (GroupShape shape : accountShapes) {
            System.out.println("\n" + count++);
            for (String group : shape.getGroups())
                System.out.println("group " + group);
            for (String member : shape.getMembers())
                System.out.println("    " + member);
        }
    }

    private static void setupShapeTest2() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        // create test
        String domainName = "test.com";
        Domain domain = prov.createDomain(domainName, new HashMap<String, Object>());

        DistributionList groupA = prov.createDistributionList("groupA@"+domainName, new HashMap<String, Object>());
        DistributionList groupB = prov.createDistributionList("groupB@"+domainName, new HashMap<String, Object>());
        DistributionList groupC = prov.createDistributionList("groupC@"+domainName, new HashMap<String, Object>());
        DistributionList groupD = prov.createDistributionList("groupD@"+domainName, new HashMap<String, Object>());

        String pw = "test123";
        Account A = prov.createAccount("A@"+domainName, pw, null);
        Account B = prov.createAccount("B@"+domainName, pw, null);
        Account C = prov.createAccount("C@"+domainName, pw, null);
        Account D = prov.createAccount("D@"+domainName, pw, null);


        groupA.addMembers(new String[]{A.getName(), groupB.getName()});

        groupB.addMembers(new String[]{B.getName(), groupC.getName()});

        groupC.addMembers(new String[]{C.getName(), groupD.getName()});

        groupD.addMembers(new String[]{D.getName()});
    }


    private static void shapeTest2() throws ServiceException {
        setupShapeTest2();

        Provisioning prov = Provisioning.getInstance();

        // create test
        Set<DistributionList> groupsWithGrants = new HashSet<DistributionList>();
        String domainName = "test.com";
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupA@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupB@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupC@"+domainName));
        groupsWithGrants.add(prov.get(DistributionListBy.name, "groupD@"+domainName));

        Set<GroupShape> accountShapes = new HashSet<GroupShape>();
        Set<GroupShape> calendarResourceShapes = new HashSet<GroupShape>();
        Set<GroupShape> distributionListShapes = new HashSet<GroupShape>();

        for (DistributionList group : groupsWithGrants) {
            // group is an AclGroup, which contains only upward membership, not downward membership.
            // re-get the DistributionList object, which has the downward membership.
            DistributionList dl = prov.get(DistributionListBy.id, group.getId());
            AllGroupMembers allMembers = allGroupMembers(dl);
            GroupShape.shapeMembers(TargetType.account, accountShapes, allMembers);
            GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembers);
            GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembers);
        }

        int count = 1;
        for (GroupShape shape : accountShapes) {
            System.out.println("\n" + count++);
            for (String group : shape.getGroups())
                System.out.println("group " + group);
            for (String member : shape.getMembers())
                System.out.println("    " + member);
        }
    }

    public static void main(String[] args) throws ServiceException {
        // shapeTest1();
        shapeTest2();
    }
}
