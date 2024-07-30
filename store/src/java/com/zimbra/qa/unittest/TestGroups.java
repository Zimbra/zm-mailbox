/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.generated.RightConsts;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.soap.admin.type.GranteeSelector;
import com.zimbra.soap.type.TargetBy;

public class TestGroups extends TestCase {

    final String domainName = "tgcache.test";
    final String domainName1 = "tgcache1.test";
    final String domainName2 = "tgcache2.test";
    final String acctPatt = "person%03d@" + domainName;
    final String acctWithAlias = "tgacctwithalias@" + domainName;
    final String acctAlias = "tgacctalias@" + domainName;
    final String acctWithAlias2 = "tgacctwithalias2@" + domainName;
    final String acctAlias2 = "tgacctalias2@" + domainName;
    final String dlWithAlias = "tgdlwithalias@" + domainName;
    final String dlAlias = "tgdlalias@" + domainName;
    final String dlWithAlias2 = "tgdlwithalias2@" + domainName;
    final String dlAlias2 = "tgdlalias2@" + domainName;
    final String normalDLPatt = "normaldl%03d@" + domainName;
    final String dynamicDLPatt = "dynamicdl%03d@" + domainName;
    final String customDLPatt = "cosdl%03d@" + domainName;
    final String cosPatt = "cdgcachecos%03d";

    final int NUM_ACCOUNTS_SMALL = 8;
    final int NUM_NORMAL_DL_SMALL = 8;
    final int NUM_DYNAMIC_DL_SMALL = 8;

    final int NUM_ACCOUNTS_PERF = 40;
    final int NUM_NORMAL_DL_PERF = 1000;
    final int NUM_DYNAMIC_DL_PERF = 200;

    /* Change these to PERF values for testing.  Note, test execution times include setup times which
     * are significant here, so look at times written to mailbox.log
     */
    final int NUM_ACCOUNTS = NUM_ACCOUNTS_SMALL;
    final int NUM_NORMAL_DL = NUM_NORMAL_DL_SMALL;
    final int NUM_DYNAMIC_DL = NUM_DYNAMIC_DL_SMALL;

    final int NUM_COS = NUM_ACCOUNTS < 4 ? NUM_ACCOUNTS : 4;
    final int NUM_CUSTOM_DL = NUM_COS;

    private Domain domain = null;
    private Domain domain1 = null;
    private Domain domain2 = null;
    private SoapProvisioning soapProv = null;
    private LdapProvisioning ldapProv = null;
    private final Map<String, Group> groups = Maps.newHashMap();

    private class GetMembershipClientThread
    implements Runnable {

        private final Account acct;
        private final LdapProvisioning prov;
        private final Set<Right> urights;

        private GetMembershipClientThread(LdapProvisioning p, Account account, Set<Right> rights) {
            prov = p;
            acct = account;
            urights = rights;
        }

        @Override
        public void run() {
            long before;
            GroupMembership membership;
            try {
                for (int cnt = 0; cnt < 20; cnt++) {
                    before = System.currentTimeMillis();
                    membership = prov.getGroupMembership(acct, false);
                    ZimbraLog.test.info("XXX getGroupMembership ms=%s acct=%s NUM=%s",
                            ZimbraLog.elapsedTime(before, System.currentTimeMillis()),
                            acct.getName(), membership.groupIds().size());
                    before = System.currentTimeMillis();
                    membership = prov.getGroupMembershipWithRights(acct, urights, false);
                    ZimbraLog.test.info("XXX getGroupMembershipWithRights ms=%s acct=%s NUM=%s",
                            ZimbraLog.elapsedTime(before, System.currentTimeMillis()),
                            acct.getName(), membership.groupIds().size());
                }
            } catch (Exception e) {
                ZimbraLog.test.error("Unable to get membership for %s.", acct, e);
            }
        }
    }

    /**
     * For testing performance (after adjusting setup parameters) - see Bug 89504
     */
    public void ENABLE_FOR_PERFORMANCE_TESTStestCustomDynamicGroups() throws Exception {
        long start = System.currentTimeMillis();
        RightManager rightMgr = RightManager.getInstance();
        Set<Right>rights = Sets.newHashSet();
        rights.add(rightMgr.getUserRight(RightConsts.RT_createDistList));
        Thread[] threads = new Thread[80];
        for (int i = 0; i < threads.length; i++) {
            String acctName = String.format(acctPatt, i % 10 + 1);
            Account acct = soapProv.getAccountByName(acctName);
            threads[i] = new Thread(new GetMembershipClientThread(ldapProv, acct, rights));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        ZimbraLog.test.info("ZZZ testCustomDynamicGroups %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    private GroupMembership doGetGroupMembershipWithRights(Account acct, Set<Right> rights,
            int expected, int adminOnlyExpected)
    throws ServiceException {
        String rightsDesc;
        if (rights == null) {
            rightsDesc = "rights={ALL(null)}";
        } else if (rights.isEmpty()) {
            rightsDesc = "rights={ALL(empty)}";
        } else {
            StringBuilder sb = new StringBuilder("rights=");
            for (Right right : rights) {
                if (sb.length() > 7) {
                    sb.append(',');
                }
                sb.append(right.getName());
            }
            rightsDesc = sb.toString();
        }
        long before = System.currentTimeMillis();
        GroupMembership membership = ldapProv.getGroupMembershipWithRights(acct, rights, false);
        long after = System.currentTimeMillis();
        String groupNames = groupInfo(membership.groupIds());
        ZimbraLog.test.info("YYY getGroupMembershipWithRights %s ms=%s acct=%s size=%s\n%s\ngroupNames=%s",
                rightsDesc, after - before, acct.getName(), membership.groupIds().size(), membership, groupNames);

        before = System.currentTimeMillis();
        GroupMembership adminOnlyMembership = ldapProv.getGroupMembershipWithRights(acct, rights, true);
        after = System.currentTimeMillis();
        String adminOnlyGroupNames = groupInfo(membership.groupIds());
        ZimbraLog.test.info("YYY getGroupMembershipWithRights [adminOnly] %s ms=%s acct=%s size=%s\n%s\ngroupNames=%s",
                rightsDesc, after - before, acct.getName(),
                adminOnlyMembership.groupIds().size(), adminOnlyMembership, adminOnlyGroupNames);

        assertEquals(String.format("Number of groups with %s which contain %s groups=%s",
                                    rightsDesc, acct.getName(), groupNames),
                        expected, membership.groupIds().size());
        assertEquals(String.format("Number of adminOnly groups with %s which contain %s groups=%s",
                                    rightsDesc, acct.getName(), adminOnlyGroupNames),
                        adminOnlyExpected, adminOnlyMembership.groupIds().size());
        return membership;
    }

    private String groupInfo(Iterable<String> groupIDs) {
        StringBuilder sb = new StringBuilder();
        for (String groupID : groupIDs) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            Group grp = groups.get(groupID);
            if (grp == null) {
                sb.append("UNKNOWN (not created by this test):").append(groupID);
            } else {
                sb.append(grp.getName()).append("(id=").append(groupID).append(")");
            }
        }
        return sb.toString();
    }

    private void doAnyRightsTestForAccount(String acctName, int expected, int adminOnlyExpected)
    throws ServiceException {
        Set<Right>rights = Sets.newHashSet();

        Account acct = soapProv.getAccountByName(acctName);
        doGetGroupMembershipWithRights(acct, rights, expected, adminOnlyExpected);
        doGetGroupMembershipWithRights(acct, null, expected, adminOnlyExpected);
    }

    private void doRightsTestForAccount(String acctName, int expected, int adminOnlyExpected) throws ServiceException {
        RightManager rightMgr = RightManager.getInstance();
        Set<Right>rights = Sets.newHashSet();
        rights.add(rightMgr.getUserRight(RightConsts.RT_createDistList));

        Account acct = soapProv.getAccountByName(acctName);
        doGetGroupMembershipWithRights(acct, rights, expected, adminOnlyExpected);
    }

    public void testCustomDynamicGroupsAnyRights1() throws Exception {
        long start = System.currentTimeMillis();
        // person001@cdgcache.test is not in any of the groups we've assigned rights to
        doAnyRightsTestForAccount(String.format(acctPatt, 1), 0, 0);
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsAnyRights1 %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testCustomDynamicGroupsCreateDistListRights1() throws Exception {
        long start = System.currentTimeMillis();
        doRightsTestForAccount(String.format(acctPatt, 1), 0, 0); // person001@cdgcache.test
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsCreateDistListRights1 %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testCustomDynamicGroupsAnyRights2() throws Exception {
        long start = System.currentTimeMillis();
        // person002@cdgcache.test is in all of the normaldl*@cdgcache.test DLs but no others
        // normaldl001@cdgcache.test DL has been assigned createDistList and sentToDistList rights
        doAnyRightsTestForAccount(String.format(acctPatt, 2), 1, 0);
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsAnyRights2 %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testCustomDynamicGroupsCreateDistListRights2() throws Exception {
        long start = System.currentTimeMillis();
        doRightsTestForAccount(String.format(acctPatt, 2), 1, 0); // person002@cdgcache.test
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsCreateDistListRights2 %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testCustomDynamicGroupsAnyRights3() throws Exception {
        long start = System.currentTimeMillis();
        // person003@cdgcache.test is in all of the dynamicdl*@cdgcache.test DLs but no others
        // dynamicdl001@cdgcache.test DL has been assigned createDistList rights
        // dynamicdl002@cdgcache.test DL has been assigned sendToDistList rights
        doAnyRightsTestForAccount(String.format(acctPatt, 3), 2, 0);
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsCreateDistListRights3 %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testCustomDynamicGroupsCreateDistListRights3() throws Exception {
        long start = System.currentTimeMillis();
        doRightsTestForAccount(String.format(acctPatt, 3), 1, 0); // person003@cdgcache.test
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsCreateDistListRights3 %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testCustomDynamicGroupsAnyRights4() throws Exception {
        long start = System.currentTimeMillis();
        // person004@cdgcache.test is in all of the normaldl*@cdgcache.test DLs but no others
        // normaldl001@cdgcache.test DL has been assigned createDistList and sentToDistList rights
        // cosdl001@cdgcache.test DL has been assigned createDistList rights

        doAnyRightsTestForAccount(String.format(acctPatt, 4), 2, 0); // person004@cdgcache.test
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsCreateDistListRights4 %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testCustomDynamicGroupsCreateDistListRights4() throws Exception {
        long start = System.currentTimeMillis();
        doRightsTestForAccount(String.format(acctPatt, 4), 2, 0); // person004@cdgcache.test
        ZimbraLog.test.info("ZZZ testCustomDynamicGroupsCreateDistListRights4 %s",
                ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    private void doGetCustomDynamicGroupMembership(int acctNum)
    throws ServiceException {
        String acctName = String.format(acctPatt, acctNum);
        Account acct = ldapProv.getAccountByName(acctName);
        GroupMembership membership = ldapProv.getCustomDynamicGroupMembership(acct, false);
        String groupNames = groupInfo(membership.groupIds());
        assertEquals(String.format("Number of dynamic groups with custom memberURL s which contain %s groups=%s",
                                    acct.getName(), groupNames), 1, membership.groupIds().size());
        String cosName = String.format(customDLPatt, acctNum % NUM_COS + 1);
        Group grp = groups.get(membership.groupIds().get(0));
        String groupName = (grp == null) ? "UNKNOWN(not created by this test)" : grp.getName();
        assertEquals(String.format("Name of dynamic group with custom memberURL s which contains %s", acctName),
                    cosName, groupName);
    }

    public void testGetCustomDynamicGroups() throws Exception {
        long start = System.currentTimeMillis();
        doGetCustomDynamicGroupMembership(1);
        doGetCustomDynamicGroupMembership(4);
        ZimbraLog.test.info("ZZZ testGetCustomDynamicGroups %s",
                ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    public void testDLupdateGroupMembershipWithoutViaWithAliases() throws Exception {
        Account acct = ldapProv.getAccountByName(acctWithAlias);
        GroupMembership membership = new GroupMembership();
        long start = System.currentTimeMillis();
        DistributionList.updateGroupMembership(ldapProv, (ZLdapContext) null, membership, acct, null /* via */,
                false /* adminGroupsOnly */, false /* directOnly */);
        ZimbraLog.test.info("testDLupdateGroupMembershipWithoutVia %s size=%d",
                ZimbraLog.elapsedTime(start, System.currentTimeMillis()), membership.groupIds().size());
        for (int cnt = 1;cnt<= NUM_NORMAL_DL; cnt++) {
            String nam = String.format(normalDLPatt, cnt);
            DistributionList dl = ldapProv.get(DistributionListBy.name, nam);
            Assert.assertTrue(String.format("DL %s (id=%s) in membership", nam, dl.getId()),
                    membership.groupIds().contains(dl.getId()));
        }
        DistributionList dl = ldapProv.get(DistributionListBy.name, dlWithAlias);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getId(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        dl = ldapProv.get(DistributionListBy.name, dlWithAlias2);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getId(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        Assert.assertEquals(String.format("Number of DLs User %s is a member of", acctWithAlias),
                NUM_NORMAL_DL + 2, membership.memberOf().size());

        acct = ldapProv.getAccountByName(acctWithAlias2);
        membership = new GroupMembership();
        start = System.currentTimeMillis();
        DistributionList.updateGroupMembership(ldapProv, (ZLdapContext) null, membership, acct, null /* via */,
                false /* adminGroupsOnly */, false /* directOnly */);
        ZimbraLog.test.info("testDLupdateGroupMembershipWithoutVia %s size=%d",
                ZimbraLog.elapsedTime(start, System.currentTimeMillis()), membership.groupIds().size());
        dl = ldapProv.get(DistributionListBy.name, dlWithAlias);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getName(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        dl = ldapProv.get(DistributionListBy.name, dlWithAlias2);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getName(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        Assert.assertEquals(String.format("Number of DLs User %s is a member of", acctWithAlias2),
                2, membership.memberOf().size());
    }

    public void testDLupdateGroupMembershipWithViaWithAliases() throws Exception {
        Account acct = ldapProv.getAccountByName(acctWithAlias);
        GroupMembership membership = new GroupMembership();
        Map<String, String> via = Maps.newHashMap();
        long start = System.currentTimeMillis();
        DistributionList.updateGroupMembership(ldapProv, (ZLdapContext) null, membership, acct, via,
                false /* adminGroupsOnly */, false /* directOnly */);
        ZimbraLog.test.info("testDLupdateGroupMembershipWithVia %s size=%d via size=%d via=%s",
                ZimbraLog.elapsedTime(start, System.currentTimeMillis()), membership.groupIds().size(), via.size(), via);
        for (int cnt = 1;cnt<= NUM_NORMAL_DL; cnt++) {
            String nam = String.format(normalDLPatt, cnt);
            DistributionList dl = ldapProv.get(DistributionListBy.name, nam);
            Assert.assertTrue(String.format("DL %s (id=%s) in membership", nam, dl.getId()),
                    membership.groupIds().contains(dl.getId()));
        }
        DistributionList dl = ldapProv.get(DistributionListBy.name, dlWithAlias);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getId(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        dl = ldapProv.get(DistributionListBy.name, dlWithAlias2);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getId(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        Assert.assertEquals(String.format("Number of DLs User %s is a member of", acctWithAlias),
                NUM_NORMAL_DL + 2, membership.memberOf().size());
        Assert.assertEquals(String.format("Number of vias for User %s", acctWithAlias), 1, via.size());

        acct = ldapProv.getAccountByName(acctWithAlias2);
        membership = new GroupMembership();
        Maps.newHashMap();
        start = System.currentTimeMillis();
        DistributionList.updateGroupMembership(ldapProv, (ZLdapContext) null, membership, acct, via,
                false /* adminGroupsOnly */, false /* directOnly */);
        ZimbraLog.test.info("testDLupdateGroupMembershipWithVia %s size=%d via size=%d via=%s",
                ZimbraLog.elapsedTime(start, System.currentTimeMillis()), membership.groupIds().size(), via.size(), via);
        dl = ldapProv.get(DistributionListBy.name, dlWithAlias);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getName(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        dl = ldapProv.get(DistributionListBy.name, dlWithAlias2);
        Assert.assertTrue(String.format("DL %s (id=%s) in membership", dl.getName(), dl.getId()),
                membership.groupIds().contains(dl.getId()));
        Assert.assertEquals(String.format("Number of DLs User %s is a member of", acctWithAlias2),
                2, membership.memberOf().size());
        Assert.assertEquals(String.format("Number of vias for User %s", acctWithAlias2), 1, via.size());
    }

    public void testInACLGRoup() throws Exception {
        long start = System.currentTimeMillis();
        String acctName;
        acctName = String.format(acctPatt, 1);
        doInACLGroup(acctName, String.format(normalDLPatt, 1), false);
        doInACLGroup(acctName, String.format(dynamicDLPatt, 1), false);
        doInACLGroup(acctName, String.format(customDLPatt, 1), false);
        acctName = String.format(acctPatt, 2);
        doInACLGroup(acctName, String.format(normalDLPatt, 1), true);
        doInACLGroup(acctName, String.format(dynamicDLPatt, 1), false);
        doInACLGroup(acctName, String.format(customDLPatt, 1), false);
        acctName = String.format(acctPatt, 3);
        doInACLGroup(acctName, String.format(normalDLPatt, 1), false);
        doInACLGroup(acctName, String.format(dynamicDLPatt, 1), true);
        doInACLGroup(acctName, String.format(customDLPatt, 1), false);
        acctName = String.format(acctPatt, 4);
        doInACLGroup(acctName, String.format(normalDLPatt, 1), true);
        doInACLGroup(acctName, String.format(dynamicDLPatt, 1), false);
        doInACLGroup(acctName, String.format(customDLPatt, 1), true);
        ZimbraLog.test.info("ZZZ testInACLGRoup %s", ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
    }

    private void doInACLGroup(String acctName, String groupName, boolean expected) throws ServiceException {
        Account acct = ldapProv.getAccountByName(acctName);
        Group group = ldapProv.getGroup(DistributionListBy.name, groupName);
        assertEquals(String.format("account %s in group %s", acct.getName(), group.getName()),
                expected, ldapProv.inACLGroup(acct, group.getId()));
    }

    @Override
    public void setUp() throws Exception {
        ldapProv = (LdapProvisioning) Provisioning.getInstance();
        if (soapProv == null) {
            soapProv = TestUtil.newSoapProvisioning();
        }
        tearDown();
        domain = createDomain(domainName);
        domain1 = createDomain(domainName1);
        domain2 = createDomain(domainName2);
        for (int cnt = 1;cnt<= NUM_COS; cnt++) {
            createCos(String.format(cosPatt, cnt));
        }
        groups.clear();
        for (int cnt = 1;cnt<= NUM_CUSTOM_DL; cnt++) {
            TestUtil.deleteAccount(String.format(customDLPatt, cnt));
            Group grp = createCustomDynamicGroupWhoseMembersShareCOS(String.format(customDLPatt, cnt), String.format(cosPatt, cnt));
            groups.put(grp.getId(), grp);
        }
        List<String> staticMembers = Lists.newArrayList();
        List<String> dynamicMembers = Lists.newArrayList();
        for (int cnt = 1;cnt<= NUM_ACCOUNTS; cnt++) {
            int cosChoice = cnt % NUM_COS + 1;
            createAccountAsMemberOfCOS(String.format(acctPatt, cnt), String.format(cosPatt, cosChoice));
            if (cnt % 2 == 0) {
                staticMembers.add(String.format(acctPatt, cnt));
            }
            if (cnt % 3 == 0) {
                dynamicMembers.add(String.format(acctPatt, cnt));
            }
        }
        createAccountWithAlias(acctWithAlias, acctAlias);
        createAccountWithAlias(acctWithAlias2, acctAlias2);
        staticMembers.add(acctAlias);
        DistributionList dlalias = createDistributionList(dlWithAlias);
        dlalias.addAlias(dlAlias);
        DistributionList dlalias2 = createDistributionList(dlWithAlias2);
        dlalias2.addAlias(dlAlias2);
        String[] aliasmems = {dlAlias2};
        dlalias.addMembers(aliasmems);
        String[]aliasmems2 = {acctAlias, acctAlias2};
        dlalias2.addMembers(aliasmems2);
        for (int cnt = 1;cnt<= NUM_NORMAL_DL; cnt++) {
            DistributionList dl = createDistributionList(String.format(normalDLPatt, cnt));
            groups.put(dl.getId(), dl);
            dl.addMembers(staticMembers.toArray(new String[staticMembers.size()]));
            for (String mem :staticMembers) {
                ZimbraLog.test.info("SETUP Distribution List %s has member acct=%s", dl.getName(), mem);
            }
        }
        for (int cnt = 1;cnt<= NUM_DYNAMIC_DL; cnt++) {
            Group grp = createDynamicGroup(String.format(dynamicDLPatt, cnt));
            groups.put(grp.getId(), grp);
            ldapProv.addGroupMembers(grp, dynamicMembers.toArray(new String[dynamicMembers.size()]));
            for (String mem :dynamicMembers) {
                ZimbraLog.test.info("SETUP Dynamic Group %s has member acct=%s", grp.getName(), mem);
            }
        }
        String groupName = String.format(customDLPatt, 1);
        // account4 should match this
        // account2 should NOT match this
        // account3 should NOT match this
        ldapProv.grantRight("domain" /* targetType */, TargetBy.name /* targetBy */, domain.getName() /* target */,
                "grp" /* granteeType */, GranteeSelector.GranteeBy.name /* granteeBy */, groupName /* grantee */,
                null /* secret */, RightConsts.RT_createDistList /* right */, null /* rightModifier */);
        ZimbraLog.test.info("SETUP Granted %s to Group %s for dom=%s", RightConsts.RT_createDistList, groupName, domain.getName());
        // account4 should match this
        // account2 should match this
        // account3 should NOT match this
        groupName = String.format(normalDLPatt, 1);
        ldapProv.grantRight("domain" /* targetType */, TargetBy.name /* targetBy */, domain1.getName() /* target */,
                "grp" /* granteeType */, GranteeSelector.GranteeBy.name /* granteeBy */, groupName /* grantee */,
                null /* secret */, RightConsts.RT_createDistList /* right */, null /* rightModifier */);
        ZimbraLog.test.info("SETUP Granted %s to Group %s for dom=%s", RightConsts.RT_createDistList, groupName, domain.getName());
        ldapProv.grantRight("domain" /* targetType */, TargetBy.name /* targetBy */, domain.getName() /* target */,
                "grp" /* granteeType */, GranteeSelector.GranteeBy.name /* granteeBy */, groupName /* grantee */,
                null /* secret */, RightConsts.RT_sendToDistList /* right */, null /* rightModifier */);
        ZimbraLog.test.info("SETUP Granted %s to Group %s for dom=%s", RightConsts.RT_sendToDistList, groupName, domain.getName());
        // account4 should NOT match this
        // account2 should NOT match this
        // account3 should match this
        groupName = String.format(dynamicDLPatt, 1);
        ldapProv.grantRight("domain" /* targetType */, TargetBy.name /* targetBy */, domain2.getName() /* target */,
                "grp" /* granteeType */, GranteeSelector.GranteeBy.name /* granteeBy */, groupName /* grantee */,
                null /* secret */, RightConsts.RT_createDistList /* right */, null /* rightModifier */);
        ZimbraLog.test.info("SETUP Granted %s to Group %s for dom=%s", RightConsts.RT_createDistList, groupName, domain.getName());
        // account4 should NOT match this
        // account2 should NOT match this
        // account3 should match this
        groupName = String.format(dynamicDLPatt, 2);
        ldapProv.grantRight("domain" /* targetType */, TargetBy.name /* targetBy */, domain2.getName() /* target */,
                "grp" /* granteeType */, GranteeSelector.GranteeBy.name /* granteeBy */, groupName /* grantee */,
                null /* secret */, RightConsts.RT_sendToDistList /* right */, null /* rightModifier */);
        ZimbraLog.test.info("SETUP Granted %s to Group %s for dom=%s", RightConsts.RT_sendToDistList, groupName, domain.getName());
    }

    @Override
    public void tearDown() throws Exception {
        deleteGroupIfExists(dlWithAlias);
        deleteGroupIfExists(dlWithAlias2);
        for (int cnt = 1;cnt<= NUM_NORMAL_DL; cnt++) {
            deleteGroupIfExists(String.format(normalDLPatt, cnt));
        }
        for (int cnt = 1;cnt<= NUM_DYNAMIC_DL; cnt++) {
            deleteGroupIfExists(String.format(dynamicDLPatt, cnt));
        }
        for (int cnt = 1;cnt<= NUM_CUSTOM_DL; cnt++) {
            deleteGroupIfExists(String.format(customDLPatt, cnt));
        }
        TestUtil.deleteAccount(acctWithAlias);
        TestUtil.deleteAccount(acctWithAlias2);
        for (int cnt = 1;cnt<= NUM_ACCOUNTS; cnt++) {
            TestUtil.deleteAccount(String.format(acctPatt, cnt));
        }
        for (int cnt = 1;cnt<= NUM_COS; cnt++) {
            deleteCosIfExists(String.format(cosPatt, cnt));
        }
        if (domain != null) {
            soapProv.deleteDomain(domain.getId());
            domain = null;
        }
        if (domain1 != null) {
            soapProv.deleteDomain(domain1.getId());
            domain1 = null;
        }
        if (domain2 != null) {
            soapProv.deleteDomain(domain2.getId());
            domain2 = null;
        }
    }

    public Account createAccountWithAlias(String name, String alias)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.name, name);
        if (acct == null) {
            Map<String, Object> attrs = Maps.newHashMap();
            acct = prov.createAccount(name, TestUtil.DEFAULT_PASSWORD, attrs);
        } else {
            ZimbraLog.test.warn("createAccountWithAlias(%s) - already existed!!!", name);
        }
        if (acct == null) {
            ZimbraLog.test.warn("createAccountWithAliase(%s) returning null!!!", name);
        }
        prov.addAlias(acct, alias);
        return acct;
    }

    public Account createAccountAsMemberOfCOS(String name, String cosName)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.name, name);
        if (acct != null) {
            ZimbraLog.test.warn("createAccountAsMemberOfCOS(%s,%s) - already existed!!!", name, cosName);
            return acct;
        }
        Map<String, Object> attrs = Maps.newHashMap();
        Cos cos = prov.get(Key.CosBy.name, cosName);
        attrs.put(ZAttrProvisioning.A_zimbraCOSId, cos.getId());
        acct = prov.createAccount(name, TestUtil.DEFAULT_PASSWORD, attrs);
        if (acct == null) {
            ZimbraLog.test.warn("createAccountAsMemberOfCOS(%s,%s) returning null!!!", name, cosName);
        }
        return acct;
    }

    public static Domain createDomain(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Domain dom = prov.get(Key.DomainBy.name, name);
        if (dom != null) {
            ZimbraLog.test.warn("createDomain(%s) - already existed!!!", name);
            return dom;
        }
        Map<String, Object> attrs = Maps.newHashMap();
        dom = prov.createDomain(name, attrs);
        if (dom == null) {
            ZimbraLog.test.warn("createDomain returning null for '%s'", name);
        }
        return dom;
    }

    public static Cos createCos(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Cos cos = prov.get(Key.CosBy.name, name);
        if (cos != null) {
            ZimbraLog.test.warn("createCos(%s) - already existed!!!", name);
            return cos;
        }
        cos = prov.createCos(name, null);
        if (cos == null) {
            ZimbraLog.test.warn("createCos returning null for '%s'", name);
        }
        return cos;
    }

    public static Group createDynamicGroup(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Group group = prov.getGroup(Key.DistributionListBy.name, name, true, false);
        if (group != null) {
            ZimbraLog.test.warn("createDynamicGroup(%s) - already existed!!!", name);
            return group;
        }
        Map<String, Object> attrs = Maps.newHashMap();
        group = prov.createGroup(name, attrs, true);
        if (group == null) {
            ZimbraLog.test.warn("createDynamicGroup returning null for '%s'", name);
        }
        return group;
    }

    public static Group createCustomDynamicGroupWhoseMembersShareCOS(String name, String cosName)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        String dName = name.substring(0, name.indexOf('@') - 1);
        Group group = prov.getGroup(Key.DistributionListBy.name, name, true, false);
        if (group != null) {
            ZimbraLog.test.warn("createCustomDynamicGroupWhoseMembersShareCOS(%s) - already existed!!!", name);
            return group;
        }
        Map<String, Object> attrs = Maps.newHashMap();
        Cos cos = prov.get(Key.CosBy.name, cosName);
        attrs.put(ZAttrProvisioning.A_memberURL,
                String.format(
                        "ldap:///??sub?(&(objectClass=zimbraAccount)(zimbraCOSId=%s)(zimbraAccountStatus=active))",
                        cos.getId()));
        attrs.put(ZAttrProvisioning.A_zimbraIsACLGroup, "TRUE");
        attrs.put(ZAttrProvisioning.A_zimbraMailStatus, "enabled");
        attrs.put(ZAttrProvisioning.A_displayName, dName);
        group = prov.createGroup(name, attrs, true);
        if (group == null) {
            ZimbraLog.test.debug("ensureCustomDynamicGroupExists returning null for '%s'", name);
        }
        return group;
    }

    public static DistributionList createDistributionList(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        DistributionList dl = prov.get(Key.DistributionListBy.name, name);
        if (dl != null) {
            ZimbraLog.test.warn("createDistributionList(%s) - already existed!!!", name);
            return dl;
        }
        Map<String, Object> attrs = Maps.newHashMap();
        dl = prov.createDistributionList(name, attrs);
        if (dl == null) {
            ZimbraLog.test.debug("createDistributionList returning null for '%s'", name);
        }
        return dl;
    }

    public static void deleteGroupIfExists(String name) {
        try {
            Provisioning prov = Provisioning.getInstance();
            Group group = prov.getGroup(Key.DistributionListBy.name, name, true, false);
            if (group != null) {
                prov.deleteGroup(group.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting group " + name, ex);
        }
    }

    public static void deleteCosIfExists(String name) {
        try {
            Provisioning prov = Provisioning.getInstance();
            Cos res = prov.get(Key.CosBy.name, name);
            if (res != null) {
                prov.deleteCos(res.getId());
            }
        } catch (Exception ex) {
            ZimbraLog.test.error("Problem deleting Cos " + name, ex);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        TestUtil.cliSetup();
        try {
            TestUtil.runTest(TestGroups.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
