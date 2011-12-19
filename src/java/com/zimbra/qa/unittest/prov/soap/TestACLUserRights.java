/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest.prov.soap;

import java.util.List;
import java.util.ArrayList;

import org.junit.*;

import static org.junit.Assert.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.client.ZAce;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGrant;
import com.zimbra.client.ZMailbox;
import com.zimbra.qa.QA.Bug;
import com.zimbra.qa.unittest.TestUtil;

public class TestACLUserRights extends SoapTest {

    private static SoapProvTestUtil provUtil;
    private static Provisioning prov;
    private static Domain domain;
    
    @BeforeClass
    public static void init() throws Exception {
        provUtil = new SoapProvTestUtil();
        prov = provUtil.getProv();
        domain = provUtil.createDomain(baseDomainName());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        Cleanup.deleteAll(baseDomainName());
    }
    
    private Account createUserAccount(String localPart) throws Exception {
        return provUtil.createAccount(localPart, domain);
    }
    
    @Test
    @Bug(bug=42146)
    public void testFallbackToFolderRight() throws Exception {
        
        // grantees
        Account allowed = createUserAccount("allowed");
        Account denied = createUserAccount("denied");
        Account noAclButHasFolderGrant = createUserAccount("noAclButHasFolderGrant");
        Account noAclAndNoFolderGrant = createUserAccount("noAclAndNoFolderGrant");
        
        // owner
        Account owner = createUserAccount("owner");
        
        ZMailbox ownerMbox = TestUtil.getZMailbox(owner.getName());
        
        // grant account right
        ZAce aceAllow = new ZAce(ZAce.GranteeType.usr, allowed.getId(), allowed.getName(), "invite", false, null);
        ownerMbox.grantRight(aceAllow);
        ZAce aceDeny = new ZAce(ZAce.GranteeType.usr, denied.getId(), denied.getName(), "invite", true, null);
        ownerMbox.grantRight(aceDeny);
        
        // grant folder right
        String folderPath = "/Calendar";
        short rights = ACL.RIGHT_READ | ACL.RIGHT_WRITE | ACL.RIGHT_INSERT | ACL.RIGHT_DELETE;
        String rightsStr = ACL.rightsToString(rights);
        ZFolder folder = ownerMbox.getFolder(folderPath);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.usr, denied.getName(), rightsStr, null);
        ownerMbox.modifyFolderGrant(folder.getId(), ZGrant.GranteeType.usr, noAclButHasFolderGrant.getName(), rightsStr, null);
        
        // check permission
        List<String> rightsToCheck = new ArrayList<String>();
        rightsToCheck.add("invite");
        boolean result;
            
        result = TestUtil.getZMailbox(allowed.getName()).checkRights(owner.getName(), rightsToCheck);
        assertTrue(result);
         
        result = TestUtil.getZMailbox(denied.getName()).checkRights(owner.getName(), rightsToCheck);
        assertTrue(result);
        
        result = TestUtil.getZMailbox(noAclButHasFolderGrant.getName()).checkRights(owner.getName(), rightsToCheck);
        assertTrue(result);
        
        result = TestUtil.getZMailbox(noAclAndNoFolderGrant.getName()).checkRights(owner.getName(), rightsToCheck);
        assertFalse(result);
    }

}
