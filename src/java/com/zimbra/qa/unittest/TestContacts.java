/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

import junit.framework.TestCase;


public class TestContacts
extends TestCase {

    /**
     * Confirms that volumeId is not set for contacts.
     */
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
}
