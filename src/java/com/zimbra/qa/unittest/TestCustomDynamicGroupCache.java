/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.Map;

import junit.framework.TestCase;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class TestCustomDynamicGroupCache extends TestCase {

    final String domainName = "cdgcache.test";
    final String acctPatt = "person%03d@" + domainName;
    final String normalDLPatt = "normalDL%03d@" + domainName;
    final String dynamicDLPatt = "dynamicDL%03d@" + domainName;
    final String customDLPatt = "cosDL%03d@" + domainName;
    final String cosPatt = "cdgcacheCOS%03d";

    final int NUM_ACCOUNTS = 10;
    final int NUM_COS = NUM_ACCOUNTS;
    final int NUM_CUSTOM_DL = NUM_ACCOUNTS;
    final int NUM_NORMAL_DL = 1000;
    final int NUM_DYNAMIC_DL = 20;
    private Domain domain = null;
    private SoapProvisioning soapProv = null;
    private LdapProvisioning ldapProv = null;

    private class GetMembershipClientThread
    implements Runnable {

        private final String account;
        private final LdapProvisioning prov;

        private GetMembershipClientThread(LdapProvisioning p, String accountName) {
            prov = p;
            account = accountName;
        }

        @Override
        public void run() {
            try {
                Account acct = soapProv.getAccountByName(account);
                for (int cnt = 0; cnt < 20; cnt++) {
                    long before = System.currentTimeMillis();
                    GroupMembership membership = prov.getGroupMembership(acct, false);
                    // GroupMembership membership = prov.getCustomDynamicGroupMembership(acct, false);
                    long after = System.currentTimeMillis();
                    ZimbraLog.test.info("XXX getGroupMembership ms=%s acct=%s NUM=%s", after - before, acct.getName(),
                        membership.groupIds().size());
                }
            } catch (Exception e) {
                ZimbraLog.test.error("Unable to get membership for %s.", account, e);
            }
        }
    }

    public void testCustomDynamicGroups() throws Exception {
        Thread[] threads = new Thread[80];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new GetMembershipClientThread(ldapProv, String.format(acctPatt, i % 10 + 1)));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Override
    public void setUp() throws Exception {
        ldapProv = (LdapProvisioning) Provisioning.getInstance();
        tearDown();
        ensureDomainExists(domainName);
        for (int cnt = 1;cnt<= NUM_COS; cnt++) {
            ensureCosExists(String.format(cosPatt, cnt));
        }
        for (int cnt = 1;cnt<= NUM_CUSTOM_DL; cnt++) {
            TestUtil.deleteAccount(String.format(customDLPatt, cnt));
            ensureCustomDynamicGroupExists(String.format(customDLPatt, cnt), String.format(cosPatt, cnt));
        }
        String [] members = new String[NUM_ACCOUNTS];
        for (int cnt = 1;cnt<= NUM_ACCOUNTS; cnt++) {
            ensureAccountExistsWithCos(String.format(acctPatt, cnt), String.format(cosPatt, cnt));
            members[cnt - 1] = String.format(acctPatt, cnt);
        }
        for (int cnt = 1;cnt<= NUM_NORMAL_DL; cnt++) {
            DistributionList dl = ensureDlExists(String.format(normalDLPatt, cnt));
            dl.addMembers(members);
        }
        for (int cnt = 1;cnt<= NUM_DYNAMIC_DL; cnt++) {
            Group grp = ensureDynamicGroupExists(String.format(dynamicDLPatt, cnt));
            // soapProv.addGroupMembers(grp, members);
        }
    }

    @Override
    public void tearDown() throws Exception {
        if (soapProv == null) {
            soapProv = TestUtil.newSoapProvisioning();
        }
        for (int cnt = 1;cnt<= NUM_NORMAL_DL; cnt++) {
            deleteGroupIfExists(String.format(normalDLPatt, cnt));
        }
        for (int cnt = 1;cnt<= NUM_DYNAMIC_DL; cnt++) {
            deleteGroupIfExists(String.format(dynamicDLPatt, cnt));
        }
        for (int cnt = 1;cnt<= NUM_CUSTOM_DL; cnt++) {
            deleteGroupIfExists(String.format(customDLPatt, cnt));
        }
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
    }

    public Account ensureAccountExistsWithCos(String name, String cosName)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(AccountBy.name, name);
        if (acct == null) {
            Map<String, Object> attrs = Maps.newHashMap();
            Cos cos = prov.get(Key.CosBy.name, cosName);
            attrs.put(ZAttrProvisioning.A_zimbraCOSId, cos.getId());
            acct = prov.createAccount(name, TestUtil.DEFAULT_PASSWORD, attrs);
        }
        if (acct == null) {
            ZimbraLog.test.debug("ensureAccountExists returning null!!!");
        }
        return acct;
    }

    public static Domain ensureDomainExists(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Domain dom = prov.get(Key.DomainBy.name, name);
        if (dom == null) {
            Map<String, Object> attrs = Maps.newHashMap();
            dom = prov.createDomain(name, attrs);
        }
        if (dom == null) {
            ZimbraLog.test.debug("ensureDomainExists returning null for '%s'", name);
        }
        return dom;
    }

    public static Cos ensureCosExists(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Cos cos = prov.get(Key.CosBy.name, name);
        if (cos == null) {
            cos = prov.createCos(name, null);
        }
        if (cos == null) {
            ZimbraLog.test.debug("ensureCosExists returning null for '%s'", name);
        }
        return cos;
    }

    public static Group ensureDynamicGroupExists(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        String dName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(dName);
        Group group = prov.getGroup(Key.DistributionListBy.name, name, true);
        if (group == null) {
            Map<String, Object> attrs = Maps.newHashMap();
            group = prov.createGroup(name, attrs, true);
        }
        if (group == null) {
            ZimbraLog.test.debug("ensureDynamicGroupExists returning null for '%s'", name);
        }
        return group;
    }

    public static Group ensureCustomDynamicGroupExists(String name, String cosName)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        String dName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(dName);
        Group group = prov.getGroup(Key.DistributionListBy.name, name, true);
        if (group == null) {
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
        }
        if (group == null) {
            ZimbraLog.test.debug("ensureCustomDynamicGroupExists returning null for '%s'", name);
        }
        return group;
    }

    public static DistributionList ensureDlExists(String name)
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        String dName = name.substring(name.indexOf('@') + 1);
        ensureDomainExists(dName);
        DistributionList dl = prov.get(Key.DistributionListBy.name, name);
        if (dl == null) {
            Map<String, Object> attrs = Maps.newHashMap();
            dl = prov.createDistributionList(name, attrs);
        }
        if (dl == null) {
            ZimbraLog.test.debug("ensureDLExists returning null for '%s'", name);
        }
        return dl;
    }

    public static void deleteGroupIfExists(String name) {
        try {
            Provisioning prov = Provisioning.getInstance();
            Group group = prov.getGroup(Key.DistributionListBy.name, name, true);
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
            TestUtil.runTest(TestCustomDynamicGroupCache.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
