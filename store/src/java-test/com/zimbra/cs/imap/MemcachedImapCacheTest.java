/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

package com.zimbra.cs.imap;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ObjectOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.util.ZTestWatchman;

/**
 * @author zimbra
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ImapFolder.class, MemcachedImapCache.class, MemcachedConnector.class, ZimbraMemcachedClient.class})
public class MemcachedImapCacheTest {

    @Rule public TestName testName = new TestName();
    @Rule public MethodRule watchman = new ZTestWatchman();
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initProvisioning(MailboxTestUtil.getZimbraServerDir(null));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInvalidObject() {
        try {

            PowerMockito.mockStatic(MemcachedConnector.class);
            ZimbraMemcachedClient  memcachedClient = new MockZimbraMemcachedClient();
            PowerMockito.when(MemcachedConnector.getClient()).thenReturn(memcachedClient);
            ImapFolder folder = PowerMockito.mock(ImapFolder.class);
            MemcachedImapCache imapCache = new MemcachedImapCache();
            imapCache.put("trash", folder);
            ImapFolder folderDeserz =  imapCache.get("trash");
            assertNull(folderDeserz);
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    public class MockZimbraMemcachedClient extends ZimbraMemcachedClient {

        @Override
        public Object get(String key) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                if (key.equals("zmImap:trash")) {
                    ObjectOutputStream oout = new ObjectOutputStream(bout);
                    oout.writeObject(new Hacker("hacked"));
                    oout.close();
                }
            } catch (Exception e) {
                return bout.toByteArray();
            }
           return bout.toByteArray();
        }

        @Override
        public boolean put(String key, Object value, boolean waitForAck) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (ObjectOutputStream oout = new ObjectOutputStream(bout)) {
                oout.writeObject(new Hacker("hacked"));
            } catch (Exception e) {
                return false;
            }
            return true;
        }
    }
}
