/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.type.Channel;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ChannelProvider}. The EMAIL provider is auto-registered
 * in the class's static initializer; tests assert the registry lookup behaviour, the
 * register guard rails (null/duplicate), and the JWE-backed recovery-code map accessors
 * against a real {@link Account} from the in-memory {@link MockProvisioning} harness.
 */
public class ChannelProviderTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        // Overwrite-on-duplicate contract: safe to recreate the fixture per-method.
        prov.createAccount("chan@example.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void getProviderForChannelEmailStringReturnsRegisteredEmailProvider() {
        // Act — EMAIL is registered by the static initializer.
        ChannelProvider provider = ChannelProvider.getProviderForChannel(Channel.EMAIL.toString());

        // Assert
        assertNotNull("EMAIL provider must be auto-registered", provider);
        assertTrue("EMAIL channel should resolve to an EmailChannel",
                provider instanceof EmailChannel);
    }

    @Test
    public void getProviderForChannelEmailEnumReturnsSameAsStringLookup() {
        // Act — enum and string overloads must resolve to the same instance.
        ChannelProvider byEnum = ChannelProvider.getProviderForChannel(Channel.EMAIL);
        ChannelProvider byString = ChannelProvider.getProviderForChannel(Channel.EMAIL.toString());

        // Assert
        assertNotNull(byEnum);
        assertSame("enum and string lookup share one provider instance", byString, byEnum);
    }

    @Test
    public void getProviderForChannelNullStringReturnsNull() {
        // Act / Assert — null/empty channel short-circuits to null.
        assertNull(ChannelProvider.getProviderForChannel((String) null));
        assertNull(ChannelProvider.getProviderForChannel(""));
    }

    @Test
    public void getProviderForChannelNullEnumReturnsNull() {
        // Act / Assert
        assertNull(ChannelProvider.getProviderForChannel((Channel) null));
    }

    @Test
    public void getProviderForChannelUnknownChannelReturnsNull() {
        // Act / Assert — nothing registered under this key.
        assertNull(ChannelProvider.getProviderForChannel("sms-not-registered"));
    }

    @Test
    public void registerChannelProviderNewChannelIsRetrievable() throws Exception {
        // Arrange
        EmailChannel custom = new EmailChannel();
        String channelKey = "test-channel-" + System.nanoTime();

        // Act
        ChannelProvider.registerChannelProvider(channelKey, custom);

        // Assert — the freshly registered provider is the exact instance we registered.
        assertSame(custom, ChannelProvider.getProviderForChannel(channelKey));
    }

    @Test
    public void registerChannelProviderDuplicateChannelKeepsOriginalProvider() throws Exception {
        // Arrange — register once, then attempt to overwrite.
        EmailChannel first = new EmailChannel();
        EmailChannel second = new EmailChannel();
        String channelKey = "dup-channel-" + System.nanoTime();
        ChannelProvider.registerChannelProvider(channelKey, first);

        // Act — duplicate registration is ignored (logged-and-skip branch).
        ChannelProvider.registerChannelProvider(channelKey, second);

        // Assert — the original provider survives.
        assertSame("duplicate registration must not replace the original",
                first, ChannelProvider.getProviderForChannel(channelKey));
    }

    @Test
    public void registerChannelProviderNullChannelThrowsFailure() {
        // Act / Assert — invalid channel name is rejected.
        try {
            ChannelProvider.registerChannelProvider(null, new EmailChannel());
            fail("expected ServiceException for null channel");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("channel"));
        }
    }

    @Test
    public void registerChannelProviderNullProviderThrowsFailure() {
        // Act / Assert — missing provider is rejected.
        try {
            ChannelProvider.registerChannelProvider("some-channel", null);
            fail("expected ServiceException for null provider");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("provider"));
        }
    }

    @Test
    public void getResetPasswordRecoveryCodeMapNoEncodedDataReturnsNull() throws Exception {
        // Arrange — account with no reset-password recovery code set.
        Account account = prov.get(AccountBy.name, "chan@example.com");
        ChannelProvider provider = ChannelProvider.getProviderForChannel(Channel.EMAIL);

        // Act
        Map<String, String> map = provider.getResetPasswordRecoveryCodeMap(account);

        // Assert — empty encoded data decodes to null.
        assertNull(map);
    }

    @Test
    public void getSetRecoveryAccountCodeMapNoEncodedDataReturnsNull() throws Exception {
        // Arrange — account with no recovery-account verification data.
        Account account = prov.get(AccountBy.name, "chan@example.com");
        ChannelProvider provider = ChannelProvider.getProviderForChannel(Channel.EMAIL);

        // Act
        Map<String, String> map = provider.getSetRecoveryAccountCodeMap(account);

        // Assert
        assertNull(map);
    }

    @Test
    public void getRecoveryAccountRecoveryAddressSetReturnsConfiguredAddress() throws Exception {
        // Arrange — set the recovery address, then read it back via the EMAIL provider.
        Account account = prov.get(AccountBy.name, "chan@example.com");
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, "rescue@example.com");
        prov.modifyAttrs(account, changes);
        ChannelProvider provider = ChannelProvider.getProviderForChannel(Channel.EMAIL);

        // Act
        String recovery = provider.getRecoveryAccount(account);

        // Assert
        assertEquals("rescue@example.com", recovery);
    }
}
