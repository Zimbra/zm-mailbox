/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Multimap;
import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPendingAclPush;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.ScheduledTaskManager;

/**
 * @author zimbra
 *
 */
public class AclPushTest {

	private DbConnection connection;

	@BeforeClass
	public static void init() throws Exception {
		Provisioning.setInstance(new MockProvisioning());
		LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
		DbPool.startup();
		HSQLDB.createDatabase();
		MailboxTestUtil.initServer();
		Provisioning prov = Provisioning.getInstance();
		HashMap<String, Object> attrs = new HashMap<String, Object>();
		attrs.put(Provisioning.A_zimbraId,
				"17dd075e-2b47-44e6-8cb8-7fdfa18c1a9f");
		prov.createAccount("owner@zimbra.com", "secret", attrs);
		attrs = new HashMap<String, Object>();
		attrs.put(Provisioning.A_zimbraId,
				"a4e41fbe-9c3e-4ab5-8b34-c42f17e251cd");
		prov.createAccount("principal@zimbra.com", "secret", attrs);
		ScheduledTaskManager.startup();
	}

	@Before
	public void setUp() throws Exception {
		MailboxTestUtil.clearData();
		HSQLDB.clearDatabase();
		connection = DbPool.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		connection.close();
	}


	@Test
	public void getAclPushEntriesMultipleGrantForSameItem() throws Exception {

		Account owner = Provisioning.getInstance().get(Key.AccountBy.name,
				"owner@zimbra.com");
		Account grantee = Provisioning.getInstance().get(Key.AccountBy.name,
				"principal@zimbra.com");
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);

		Folder folder = mbox.createFolder(null, "shared",
				new Folder.FolderOptions()
						.setDefaultView(MailItem.Type.DOCUMENT));

		OperationContext octxt = new OperationContext(owner);
		Multimap<Integer, Integer> mboxIdToItemIds = null;
		try (final MailboxLock l = mbox.lock(true)) {
		     l.lock();
    		mbox.grantAccess(octxt, folder.getId(), grantee.getId(),
    				ACL.GRANTEE_USER, ACL.stringToRights("r"), null);
    		mbox.grantAccess(octxt, folder.getId(), grantee.getId(),
    				ACL.GRANTEE_USER, ACL.stringToRights("rw"), null);

    		mboxIdToItemIds = DbPendingAclPush
    				.getEntries(new Date());
		}
//		assertTrue(mboxIdToItemIds.size() == 1);

		Thread.sleep(1000);
		mboxIdToItemIds = DbPendingAclPush.getEntries(new Date());
		assertTrue(mboxIdToItemIds.size() == 0);
		short rights = folder.getACL().getGrantedRights(grantee);
		assertEquals(3, rights);
	}


	@Test
	public void getAclPushEntriesFolderNameWithSemiColon() throws Exception {

		try {
		Account owner = Provisioning.getInstance().get(Key.AccountBy.name,
				"owner@zimbra.com");
		Account grantee = Provisioning.getInstance().get(Key.AccountBy.name,
				"principal@zimbra.com");
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(owner);

		Folder folder = mbox.createFolder(null, "shared",
				new Folder.FolderOptions()
						.setDefaultView(MailItem.Type.DOCUMENT));
		Folder folder2 = mbox.createFolder(null, "shared; hello",
				new Folder.FolderOptions()
						.setDefaultView(MailItem.Type.DOCUMENT));

		OperationContext octxt = new OperationContext(owner);
		Multimap<Integer, Integer> mboxIdToItemIds = null;

		try (final MailboxLock l = mbox.lock(true)) {
		     l.lock();
    		mbox.grantAccess(octxt, folder.getId(), grantee.getId(),
    				ACL.GRANTEE_USER, ACL.stringToRights("r"), null);
    		mbox.grantAccess(octxt, folder2.getId(), grantee.getId(),
    				ACL.GRANTEE_USER, ACL.stringToRights("rw"), null);

    		mboxIdToItemIds = DbPendingAclPush
    				.getEntries(new Date());
		}
//		assertTrue(mboxIdToItemIds.size() == 2);

		Thread.sleep(1000);
		mboxIdToItemIds = DbPendingAclPush.getEntries(new Date());
		assertTrue(mboxIdToItemIds.size() == 0);
		} catch (Exception e) {
			fail("Should not throw an exception.");
		}
	}


}
