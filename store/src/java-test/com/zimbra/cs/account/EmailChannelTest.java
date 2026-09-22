/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018, 2023 Synacor, Inc.
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

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.util.JWEUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for the non-mail-sending surface of {@link EmailChannel}:
 * the recovery-account accessor and the early validation failure path that does
 * not require a live {@code Mailbox}. The mail-sending methods need real Mailbox
 * / MailSender infrastructure and are intentionally not exercised here.
 */
public class EmailChannelTest {

    private Account account;

    private EmailChannel channel;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("recover@example.com", "secret", new HashMap<String, Object>());
        account = prov.get(AccountBy.name, "recover@example.com");
        channel = new EmailChannel();
    }

    @Test
    public void getRecoveryAccountRecoveryAddressSetReturnsConfiguredAddress() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, "backup@example.com");
        account.modify(attrs);

        // Act
        String recovery = channel.getRecoveryAccount(account);

        // Assert
        assertEquals("backup@example.com", recovery);
    }

    @Test
    public void getRecoveryAccountNoRecoveryAddressReturnsNull() throws Exception {
        // Arrange — fresh account has no recovery address

        // Act
        String recovery = channel.getRecoveryAccount(account);

        // Assert
        assertNull(recovery);
    }

    @Test
    public void getSetRecoveryAccountCodeMapNoVerificationDataReturnsNullOrEmpty() throws Exception {
        // Arrange — no verification data stored

        // Act
        Map<String, String> map = channel.getSetRecoveryAccountCodeMap(account);

        // Assert — decoding absent data yields null or an empty map
        assertTrue("no verification data must yield null/empty map", map == null || map.isEmpty());
    }

    @Test
    public void validateSetRecoveryAccountCodeNoStoredCodeThrowsCodeNotFound() throws Exception {
        // Arrange — no recovery verification data on the account, so validation
        // fails before any Mailbox / SOAP context is touched (null is safe here).

        // Act / Assert
        try {
            channel.validateSetRecoveryAccountCode("123456", account, null, null);
            fail("expected ForgetPasswordException for missing recovery code");
        } catch (ServiceException e) {
            assertTrue("error code must be CODE_NOT_FOUND",
                    e.getCode().contains("CODE_NOT_FOUND"));
        }
    }

    /*
     * Stores a JWE-encoded recovery-verification blob on the account so that
     * {@link EmailChannel#getSetRecoveryAccountCodeMap} returns a populated map and
     * {@link EmailChannel#validateSetRecoveryAccountCode} proceeds past the
     * "code not found" guard into the code-compare / expiry branches.
     */
    private void storeVerificationData(String code, long expiryTime) throws Exception {
        Map<String, String> data = new HashMap<String, String>();
        data.put(CodeConstants.CODE.toString(), code);
        data.put(CodeConstants.EXPIRY_TIME.toString(), String.valueOf(expiryTime));
        data.put(CodeConstants.EMAIL.toString(), "backup@example.com");
        String jwe = JWEUtil.getJWE(data);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraRecoveryAccountVerificationData, jwe);
        account.modify(attrs);
    }

    @Test
    public void getSetRecoveryAccountCodeMapDataStoredReturnsDecodedMap() throws Exception {
        // Arrange
        storeVerificationData("123456", System.currentTimeMillis() + 600000L);

        // Act
        Map<String, String> map = channel.getSetRecoveryAccountCodeMap(account);

        // Assert — round-trips through JWE encoding
        assertEquals("123456", map.get(CodeConstants.CODE.toString()));
        assertEquals("backup@example.com", map.get(CodeConstants.EMAIL.toString()));
    }

    @Test
    public void validateSetRecoveryAccountCodeWrongCodeThrowsCodeMismatch() throws Exception {
        // Arrange — store a known good code, then validate a different one
        storeVerificationData("654321", System.currentTimeMillis() + 600000L);

        // Act / Assert — mismatch is detected before any Mailbox / SOAP access
        try {
            channel.validateSetRecoveryAccountCode("000000", account, null, null);
            fail("expected ForgetPasswordException for code mismatch");
        } catch (ServiceException e) {
            assertTrue("error code must be CODE_MISMATCH", e.getCode().contains("CODE_MISMATCH"));
        }
    }

    @Test
    public void validateSetRecoveryAccountCodeExpiredCodeThrowsCodeExpired() throws Exception {
        // Arrange — matching code but expiry in the past
        storeVerificationData("abc123", System.currentTimeMillis() - 600000L);

        // Act / Assert — expiry is checked before any Mailbox / SOAP access
        try {
            channel.validateSetRecoveryAccountCode("abc123", account, null, null);
            fail("expected ForgetPasswordException for expired code");
        } catch (ServiceException e) {
            assertTrue("error code must be CODE_EXPIRED", e.getCode().contains("CODE_EXPIRED"));
        }
    }

    @Test
    public void getRecoveryAccountDistinctFromVerificationDataIndependentAttrs() throws Exception {
        // Arrange — set both the recovery address and (unrelated) verification data
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, "alt@example.com");
        account.modify(attrs);
        storeVerificationData("zzz999", System.currentTimeMillis() + 600000L);

        // Act
        String recovery = channel.getRecoveryAccount(account);
        Map<String, String> map = channel.getSetRecoveryAccountCodeMap(account);

        // Assert — recovery address and verification map are independent
        assertEquals("alt@example.com", recovery);
        assertEquals("zzz999", map.get(CodeConstants.CODE.toString()));
    }

    @Test
    public void newEmailChannelIsChannelProvider() {
        // Arrange / Act
        EmailChannel ch = new EmailChannel();

        // Assert — EmailChannel is a ChannelProvider implementation
        assertTrue(ch instanceof ChannelProvider);
    }
}
