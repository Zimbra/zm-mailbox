/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.testng.TestNG;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.zclient.ZContact;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMailbox.ContactSortBy;


public class TestContacts
extends TestCase {

    private static final String NAME_PREFIX = TestContacts.class.getSimpleName();
    private static final String USER_NAME = "user1";
    String mOriginalMaxContacts;
    
    @BeforeMethod
    public void setUp()
    throws Exception {
        cleanUp();
        mOriginalMaxContacts = TestUtil.getAccountAttr(USER_NAME, Provisioning.A_zimbraContactMaxNumEntries);
    }
    /**
     * Confirms that volumeId is not set for contacts.
     */
    @Test(groups = {"Server"})
    public void testVolumeId()
    throws Exception {
        Account account = Provisioning.getInstance().get(AccountBy.name, TestUtil.getAddress("user1"));
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE type = %d AND volume_id IS NOT NULL",
            DbMailItem.getMailItemTableName(mbox), MailItem.TYPE_CONTACT);
        DbResults results = DbUtil.executeQuery(sql);
        int count = results.getInt(1);
        assertEquals("Found non-null volumeId values for contacts", 0, count);
    }
    
    /**
     * Confirms that {@link Provisioning#A_zimbraContactMaxNumEntries} is enforced (bug 29627).
     */
    @Test
    public void testMaxContacts()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZContact> contacts = mbox.getAllContacts(null, ContactSortBy.nameAsc, false, null);
        int max = contacts.size() + 2;
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraContactMaxNumEntries, Integer.toString(max));
        Map<String, String> attrs = new HashMap<String, String>();
        int i;
        for (i = 1; i <= 10; i++) {
            attrs.put("fullName", NAME_PREFIX + i);
            try {
                mbox.createContact(Integer.toString(Mailbox.ID_FOLDER_CONTACTS), null, attrs);
            } catch (SoapFaultException e) {
                assertEquals(MailServiceException.TOO_MANY_CONTACTS, e.getCode());
                break;
            }
        }
        assertEquals("Unexpected contact number", 3, i);
    }
    
    @AfterMethod
    public void tearDown()
    throws Exception {
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraContactMaxNumEntries, mOriginalMaxContacts);
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
    
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestNG testng = TestUtil.newTestNG();
        testng.setExcludedGroups("Server");
        testng.setTestClasses(new Class[] { TestContacts.class });
        testng.run();
    }
}
