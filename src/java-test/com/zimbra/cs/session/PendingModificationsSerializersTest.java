/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import java.io.FileInputStream;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit test for {@link PendingModifications$JavaSerializer}.
 */
public final class PendingModificationsSerializersTest {
    private static final String TESTDATA_BASE_DIR = "./data/unittest/session/";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    /** Change 264 in the test data encapsulates the toggling of a flagged flag */
    @Test
    public void testChange264JavaObjectSerializer() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        byte[] data = IOUtils.toByteArray(new FileInputStream(TESTDATA_BASE_DIR + "change264.javaobject"));
        PendingModifications pm = PendingModifications.JavaObjectSerializer.deserialize(mbox, data);
        Assert.assertNotNull(pm);

        Assert.assertEquals(3, pm.changedTypes.size());
        Assert.assertTrue(pm.changedTypes.contains(MailItem.Type.FOLDER));
        Assert.assertTrue(pm.changedTypes.contains(MailItem.Type.MESSAGE));
        Assert.assertTrue(pm.changedTypes.contains(MailItem.Type.VIRTUAL_CONVERSATION));

        Assert.assertEquals(null, pm.created);
        Assert.assertEquals(null, pm.deleted);

        Assert.assertEquals(3, pm.modified.size());

        PendingModifications.Change change = pm.modified.get(new PendingModifications.ModificationKey("708a3836-6ad7-44a2-b0d7-85a5590b8a7c", 2));
        Assert.assertTrue(change.what instanceof Folder);
        Assert.assertEquals(PendingModifications.Change.SIZE, change.why);

        byte[] data_ = PendingModifications.JavaObjectSerializer.serialize(pm);
        Assert.assertArrayEquals(data, data_);
    }
}
