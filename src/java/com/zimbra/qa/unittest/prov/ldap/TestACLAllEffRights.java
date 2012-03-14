package com.zimbra.qa.unittest.prov.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.accesscontrol.CollectAllEffectiveRights;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.CollectAllEffectiveRights.AllGroupMembers;
import com.zimbra.cs.account.accesscontrol.CollectAllEffectiveRights.GroupShape;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.qa.unittest.prov.Verify;
import com.zimbra.soap.type.TargetBy;

public class TestACLAllEffRights extends LdapTest {
    private static LdapProvTestUtil provUtil;
    private static LdapProv prov;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new LdapProvTestUtil();
        prov = provUtil.getProv();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private AllGroupMembers allGroupMembers(DistributionList dl) throws ServiceException {
        CollectAllEffectiveRights caer = new CollectAllEffectiveRights(null, false, false, null);
        return caer.getAllGroupMembers(dl);
    }
    
    @Test
    public void shapeTest1() throws Exception {
        /*
         * setup
         */
        String domainName = genDomainName(baseDomainName());
        
        Domain domain = provUtil.createDomain(domainName);
        
        DistributionList groupA = provUtil.createDistributionList("groupA", domain);
        DistributionList groupB = provUtil.createDistributionList("groupB", domain);
        DistributionList groupC = provUtil.createDistributionList("groupC", domain);
        DistributionList groupD = provUtil.createDistributionList("groupD", domain);
        
        Account A = provUtil.createAccount("A", domain);
        Account B = provUtil.createAccount("B", domain);
        Account C = provUtil.createAccount("C", domain);
        Account D = provUtil.createAccount("D", domain);
        Account AB = provUtil.createAccount("AB", domain);
        Account AC = provUtil.createAccount("AC", domain);
        Account AD = provUtil.createAccount("AD", domain);
        Account BC = provUtil.createAccount("BC", domain);
        Account BD = provUtil.createAccount("BD", domain);
        Account CD = provUtil.createAccount("CD", domain);
        Account ABC = provUtil.createAccount("ABC", domain);
        Account ABD = provUtil.createAccount("ABD", domain);
        Account ACD = provUtil.createAccount("ACD", domain);
        Account BCD = provUtil.createAccount("BCD", domain);
        Account ABCD = provUtil.createAccount("ABCD", domain);
        
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
        
        
        /*
         * test
         */
        Set<DistributionList> groupsWithGrants = new HashSet<DistributionList>();
        groupsWithGrants.add(groupA);
        groupsWithGrants.add(groupB);
        groupsWithGrants.add(groupC);
        groupsWithGrants.add(groupD);
        
        Set<GroupShape> accountShapes = new HashSet<GroupShape>();
        Set<GroupShape> calendarResourceShapes = new HashSet<GroupShape>();
        Set<GroupShape> distributionListShapes = new HashSet<GroupShape>();
        
        for (DistributionList group : groupsWithGrants) {
            DistributionList dl = prov.get(DistributionListBy.id, group.getId());
            AllGroupMembers allMembers = allGroupMembers(dl);
            GroupShape.shapeMembers(TargetType.account, accountShapes, allMembers);
            GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembers);
            GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembers);
        }
        
        
        /*
         * verify
         */
        Set<String> result = new HashSet<String>();
        int count = 1;
        for (GroupShape shape : accountShapes) {
            List<String> elements = new ArrayList<String>();
            
            System.out.println("\n" + count++);
            for (String group : shape.getGroups()) {
                System.out.println("group " + group);
                elements.add("group " + group);
            }
            for (String member : shape.getMembers()) {
                System.out.println("    member" + member);
                elements.add("member " + member);
            }
            Collections.sort(elements);
            
            // only verifying shapes have members
            // there could be empty shapes spawned than necessary (bug?),
            // but it does not affect functionality
            if (shape.getMembers().size() > 0) {
                result.add(Verify.makeResultStr(elements));
            }
        }
        
        Set<String> expected = new HashSet<String>();
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "member " + A.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupB.getName(),
                "member " + B.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupC.getName(),
                "member " + C.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupD.getName(),
                "member " + D.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupB.getName(),
                "member " + AB.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupC.getName(),
                "member " + AC.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupD.getName(),
                "member " + AD.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupB.getName(),
                "group " + groupC.getName(),
                "member " + BC.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupB.getName(),
                "group " + groupD.getName(),
                "member " + BD.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupC.getName(),
                "group " + groupD.getName(),
                "member " + CD.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupB.getName(),
                "group " + groupC.getName(),
                "member " + ABC.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupB.getName(),
                "group " + groupD.getName(),
                "member " + ABD.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupC.getName(),
                "group " + groupD.getName(),
                "member " + ACD.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupB.getName(),
                "group " + groupC.getName(),
                "group " + groupD.getName(),
                "member " + BCD.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupB.getName(),
                "group " + groupC.getName(),
                "group " + groupD.getName(),
                "member " + ABCD.getName())));
        
        Verify.verifyEquals(expected, result);
    }
    
    @Test
    public void shapeTest2() throws Exception {
        /*
         * setup
         */
        String domainName = genDomainName(baseDomainName());
        
        Domain domain = provUtil.createDomain(domainName);
        
        DistributionList groupA = provUtil.createDistributionList("groupA", domain);
        DistributionList groupB = provUtil.createDistributionList("groupB", domain);
        DistributionList groupC = provUtil.createDistributionList("groupC", domain);
        DistributionList groupD = provUtil.createDistributionList("groupD", domain);
        
        Account A = provUtil.createAccount("A", domain);
        Account B = provUtil.createAccount("B", domain);
        Account C = provUtil.createAccount("C", domain);
        Account D = provUtil.createAccount("D", domain);
        
        groupA.addMembers(new String[]{A.getName(), groupB.getName()});
        groupB.addMembers(new String[]{B.getName(), groupC.getName()});
        groupC.addMembers(new String[]{C.getName(), groupD.getName()});
        groupD.addMembers(new String[]{D.getName()});
        
        /*
         * test
         */
        Set<DistributionList> groupsWithGrants = new HashSet<DistributionList>();
        groupsWithGrants.add(groupA);
        groupsWithGrants.add(groupB);
        groupsWithGrants.add(groupC);
        groupsWithGrants.add(groupD);
        
        Set<GroupShape> accountShapes = new HashSet<GroupShape>();
        Set<GroupShape> calendarResourceShapes = new HashSet<GroupShape>();
        Set<GroupShape> distributionListShapes = new HashSet<GroupShape>();
        
        for (DistributionList group : groupsWithGrants) {
            DistributionList dl = prov.get(DistributionListBy.id, group.getId());
            AllGroupMembers allMembers = allGroupMembers(dl);
            GroupShape.shapeMembers(TargetType.account, accountShapes, allMembers);
            GroupShape.shapeMembers(TargetType.calresource, calendarResourceShapes, allMembers);
            GroupShape.shapeMembers(TargetType.dl, distributionListShapes, allMembers);
        }
        
        /*
         * verify
         */
        Set<String> result = new HashSet<String>();
        int count = 1;
        for (GroupShape shape : accountShapes) {
            List<String> elements = new ArrayList<String>();
            
            System.out.println("\n" + count++);
            for (String group : shape.getGroups()) {
                System.out.println("group " + group);
                elements.add("group " + group);
            }
            for (String member : shape.getMembers()) {
                System.out.println("    " + member);
                elements.add("member " + member);
            }
            Collections.sort(elements);
            
            // only verifying shapes have members
            // there could be empty shapes spawned than necessary (bug?),
            // but it does not affect functionality
            if (shape.getMembers().size() > 0) {
                result.add(Verify.makeResultStr(elements));
            }
        }
        
        Set<String> expected = new HashSet<String>();
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "member " + A.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupB.getName(),
                "member " + B.getName()))); 
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupB.getName(),
                "group " + groupC.getName(),
                "member " + C.getName())));
        expected.add(Verify.makeResultStr(Lists.newArrayList(
                "group " + groupA.getName(),
                "group " + groupB.getName(),
                "group " + groupC.getName(),
                "group " + groupD.getName(),
                "member " + D.getName()))); 
        
        Verify.verifyEquals(expected, result);
    }
    
    /*
    zmprov cdl dl@test.com
    zmprov cdl subdl@test.com
    zmprov cdl subsubdl@test.com
    
    zmprov ca da1@test.com test123 zimbraIsDelegatedAdminAccount TRUE
    zmprov ca da2@test.com test123 zimbraIsDelegatedAdminAccount TRUE
    
    zmprov ca a_dl@test.com test123
    zmprov ca a_subdl@test.com test123
    zmprov ca a_subsubdl@test.com test123
    
    zmprov adlm dl@test.com subdl@test.com a_dl@test.com
    zmprov adlm subdl@test.com subsubdl@test.com a_subdl@test.com
    zmprov adlm subsubdl@test.com a_subsubdl@test.com
    
    zmprov grr dl dl@test.com usr da1@test.com addDistributionListMember
    zmprov grr dl dl@test.com usr da1@test.com modifyDistributionList
    zmprov grr dl dl@test.com usr da1@test.com modifyAccount
    zmprov grr dl dl@test.com usr da1@test.com listAccount
    
    zmprov grr dl dl@test.com usr da2@test.com ^addDistributionListMember
    zmprov grr dl dl@test.com usr da2@test.com ^modifyDistributionList
    zmprov grr dl dl@test.com usr da2@test.com ^modifyAccount
    zmprov grr dl dl@test.com usr da2@test.com ^listAccount
     */
    @Test
    public void disinheritSubGroupModifier() throws Exception {
        /*
         * setup
         */
        
        /*
         * dl has members:
         *    subdl
         *    a_dl
         *  
         * subdl has members:
         *    subsubdl
         *    a_subdl
         *    
         * subsubdl has members:
         *    a_subsubdl      
         */
        String domainName = genDomainName(baseDomainName());
        
        Domain domain = provUtil.createDomain(domainName);
        
        // groups
        DistributionList dl = provUtil.createDistributionList("dl", domain);
        DistributionList subdl = provUtil.createDistributionList("subdl", domain);
        DistributionList subsubdl = provUtil.createDistributionList("subsubdl", domain);

        // users
        Account a_dl = provUtil.createAccount("a_dl", domain);
        Account a_subdl = provUtil.createAccount("a_subdl", domain);
        Account a_subsubdl = provUtil.createAccount("a_subsubdl", domain);

        // delegated admins
        Account da1 = provUtil.createDelegatedAdmin("da1", domain);
        Account da2 = provUtil.createDelegatedAdmin("da2", domain);

        dl.addMembers(new String[]{subdl.getName(), a_dl.getName()});
        subdl.addMembers(new String[]{subsubdl.getName(), a_subdl.getName()});
        subsubdl.addMembers(new String[]{a_subsubdl.getName()});
        
        Right DL_RESET_RIGHT = Admin.R_addDistributionListMember;
        Right DL_ATTR_RIGHT = Admin.R_modifyDistributionList;
        Right ACCT_PRESET_RIGHT = Admin.R_listAccount;
        Right ACCT_ATTR_RIGHT = Admin.R_modifyAccount;
        
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da1.getName(), null,
                DL_RESET_RIGHT.getName(), null);
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da1.getName(), null,
                DL_ATTR_RIGHT.getName(), null);
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da1.getName(), null,
                ACCT_PRESET_RIGHT.getName(), null);
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da1.getName(), null,
                ACCT_ATTR_RIGHT.getName(), null);
        
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da2.getName(), null,
                DL_RESET_RIGHT.getName(), RightModifier.RM_DENY);
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da2.getName(), null,
                DL_ATTR_RIGHT.getName(), RightModifier.RM_DENY);
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da2.getName(), null,
                ACCT_PRESET_RIGHT.getName(), RightModifier.RM_DENY);
        RightCommand.grantRight(prov, null,
                TargetType.dl.getCode(), TargetBy.name, dl.getName(),
                GranteeType.GT_USER.getCode(), Key.GranteeBy.name, da2.getName(), null,
                ACCT_ATTR_RIGHT.getName(), RightModifier.RM_DENY);

    }
    
}
