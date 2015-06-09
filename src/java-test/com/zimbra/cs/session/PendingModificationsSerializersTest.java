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
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

/**
 * Unit test for {@link PendingModifications$JavaSerializer}.
 */
@Ignore
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
        PendingModifications pm = new PendingModificationsJavaSerializer().deserialize(mbox, data);
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

        byte[] data_ = new PendingModificationsJavaSerializer().serialize(pm);
        Assert.assertArrayEquals(data, data_);
    }

    /**
     * Load the PendingModification objects using the Java Object serializer, which is known to
     * work. Then serialize to JSON and back. Then serialize back to a Java Object, to see
     * whether the JSON serializer+deserializer had any losses.
     **/
    @Test
    public void testChange264through271JsonSerializer() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        for (int i = 264; i <= 271; i++) {
            String testDescr = "discrepancy after serializing + deserializing change" + i;

            // 1. From serialized Java Object to Java object
            String filename = TESTDATA_BASE_DIR + "change" + i + ".javaobject";
            byte[] data = IOUtils.toByteArray(new FileInputStream(filename));
            PendingModifications pm = new PendingModificationsJavaSerializer().deserialize(mbox, data);

            // 2. Sanity check. Go back to serialized Java Object and compare
            byte[] data_ = new PendingModificationsJavaSerializer().serialize(pm);
            Assert.assertEquals(testDescr, new String(data), new String(data_));
            Assert.assertArrayEquals(data, data_);

            // 3. From Java Object to JSON and back
            byte[] json = new PendingModificationsJsonSerializer().serialize(pm);
            PendingModifications pm_ = new PendingModificationsJsonSerializer().deserialize(mbox, json);
            byte[] json_ = new PendingModificationsJsonSerializer().serialize(pm_);
            Assert.assertEquals(testDescr, new String(json), new String(json_));

            // 4. From Java Object to serialized Java Object #2
            data_ = new PendingModificationsJavaSerializer().serialize(pm_);

            // 5. Compare with original
            Assert.assertEquals(pm.toString(), pm_.toString());
            assertEquals(pm.modified, pm_.modified);
        }
    }

    protected void assertEquals(Map<ModificationKey, Change> map1, Map<ModificationKey, Change> map2) {
        Assert.assertEquals(map1.keySet(), map2.keySet());
        for (ModificationKey key: map1.keySet()) {
            Assert.assertEquals(map1.get(key).toString(), map2.get(key).toString());
        }
    }
}
