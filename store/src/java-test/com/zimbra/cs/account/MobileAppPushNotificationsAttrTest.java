/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@code zimbraFeatureMobileAppPushNotificationsEnabled} attribute definition and its generated
 * accessors: registered with the expected id, type and inheritance, FALSE when unset, and updated live
 * through {@code modifyAttrs} without a restart.
 */
public class MobileAppPushNotificationsAttrTest {

    private static final String USER = "push-attr-user@example.com";

    private Provisioning provisioning;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        provisioning = Provisioning.getInstance();
        provisioning.createAccount(USER, "secret", new HashMap<String, Object>());
    }

    @After
    public void tearDown() throws Exception {
        Account account = provisioning.get(AccountBy.name, USER);
        if (account != null) {
            provisioning.deleteAccount(account.getId());
        }
    }

    // ---------- attribute definition ----------

    @Test
    public void attributeIsRegisteredAsBooleanWithId4178() throws Exception {
        AttributeInfo info = AttributeManager.getInstance()
                .getAttributeInfo(Provisioning.A_zimbraFeatureMobileAppPushNotificationsEnabled);

        assertNotNull("attribute is defined in zimbra-attrs.xml", info);
        assertEquals(4178, info.getId());
        assertEquals(AttributeType.TYPE_BOOLEAN, info.getType());
    }

    @Test
    public void attributeInheritsFromCosDomainAndGlobalConfig() throws Exception {
        AttributeManager am = AttributeManager.getInstance();
        String attr = Provisioning.A_zimbraFeatureMobileAppPushNotificationsEnabled;

        assertTrue("account falls back to COS and domain", am.isAccountCosDomainInherited(attr));
        assertTrue("domain falls back to global config", am.isDomainInherited(attr));
    }

    @Test
    public void globalConfigDefaultIsFalse() throws Exception {
        AttributeInfo info = AttributeManager.getInstance()
                .getAttributeInfo(Provisioning.A_zimbraFeatureMobileAppPushNotificationsEnabled);

        assertEquals(Collections.singletonList("FALSE"), info.getGlobalConfigValues());
    }

    // ---------- generated accessors ----------

    @Test
    public void accountGetterIsFalseWhenUnset() throws Exception {
        Account account = provisioning.get(AccountBy.name, USER);

        assertFalse(account.isFeatureMobileAppPushNotificationsEnabled());
    }

    @Test
    public void accountChangeTakesEffectImmediately() throws Exception {
        Account account = provisioning.get(AccountBy.name, USER);

        account.setFeatureMobileAppPushNotificationsEnabled(true);
        assertTrue(provisioning.get(AccountBy.name, USER).isFeatureMobileAppPushNotificationsEnabled());

        account.setFeatureMobileAppPushNotificationsEnabled(false);
        assertFalse(provisioning.get(AccountBy.name, USER).isFeatureMobileAppPushNotificationsEnabled());
    }

    @Test
    public void unsetRemovesAccountValue() throws Exception {
        Account account = provisioning.get(AccountBy.name, USER);
        account.setFeatureMobileAppPushNotificationsEnabled(true);

        account.unsetFeatureMobileAppPushNotificationsEnabled();

        assertFalse(provisioning.get(AccountBy.name, USER).isFeatureMobileAppPushNotificationsEnabled());
    }

    @Test
    public void mapSetterAndUnsetterPopulateAttrMap() throws Exception {
        Account account = provisioning.get(AccountBy.name, USER);
        String attr = Provisioning.A_zimbraFeatureMobileAppPushNotificationsEnabled;

        Map<String, Object> set = account.setFeatureMobileAppPushNotificationsEnabled(true, null);
        Map<String, Object> unset = account.unsetFeatureMobileAppPushNotificationsEnabled(null);

        assertEquals("TRUE", set.get(attr));
        assertEquals("", unset.get(attr));
    }

    @Test
    public void globalConfigValueIsReadLive() throws Exception {
        Config config = provisioning.getConfig();
        try {
            config.setFeatureMobileAppPushNotificationsEnabled(true);
            assertTrue(provisioning.getConfig().isFeatureMobileAppPushNotificationsEnabled());
        } finally {
            config.unsetFeatureMobileAppPushNotificationsEnabled();
        }
        assertFalse(provisioning.getConfig().isFeatureMobileAppPushNotificationsEnabled());
    }
}
