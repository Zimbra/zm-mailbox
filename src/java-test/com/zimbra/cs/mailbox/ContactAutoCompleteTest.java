/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.mailbox;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit test for {@link ContactAutoComplete}.
 *
 * @author ysasaki
 */
public class ContactAutoCompleteTest {

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.setInstance(new MockProvisioning());
        MailboxManager.setInstance(new MockMailboxManager());
        Provisioning.getInstance().createAccount("test@zimbra.com", "secret",
                Collections.singletonMap(Provisioning.A_zimbraId, (Object) "0"));
    }

    @Test
    public void hitContact() throws Exception {
        ContactAutoComplete.AutoCompleteResult result =
            new ContactAutoComplete.AutoCompleteResult(10);
        result.rankings = new ContactRankings("0");
        ContactAutoComplete.ContactEntry contact =
            new ContactAutoComplete.ContactEntry();
        contact.mDisplayName = "C1";
        contact.mEmail = "c1@zimbra.com";
        result.addEntry(contact);
        Assert.assertEquals(result.entries.size(), 1);

        contact = new ContactAutoComplete.ContactEntry();
        contact.mDisplayName = "C2";
        contact.mEmail = "c2@zimbra.com";
        result.addEntry(contact);
        Assert.assertEquals(result.entries.size(), 2);
    }

    @Test
    public void hitGroup() throws Exception {
        ContactAutoComplete.AutoCompleteResult result =
            new ContactAutoComplete.AutoCompleteResult(10);
        result.rankings = new ContactRankings("0");
        ContactAutoComplete.ContactEntry group =
            new ContactAutoComplete.ContactEntry();
        group.mDisplayName = "G1";
        group.mDlist = "DL1";
        result.addEntry(group);
        Assert.assertEquals(result.entries.size(), 1);

        group = new ContactAutoComplete.ContactEntry();
        group.mDisplayName = "G2";
        group.mDlist = "DL2";
        result.addEntry(group);
        Assert.assertEquals(result.entries.size(), 2);
    }

}
