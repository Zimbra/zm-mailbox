/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link TwoFactorAuthStatus} - the AttributeCallback that guards the
 * relationship between the two-factor "available", "required" and "enabled" flags on Accounts
 * and COSes. The two-factor extension is not deployed in the unit harness, so making 2FA
 * available always fails here - that itself exercises the extension-check branch.
 */
public class TwoFactorAuthStatusTest {

    private static final String AVAILABLE = Provisioning.A_zimbraFeatureTwoFactorAuthAvailable;

    private static final String REQUIRED = Provisioning.A_zimbraFeatureTwoFactorAuthRequired;

    private static final String ENABLED = Provisioning.A_zimbraTwoFactorAuthEnabled;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private CallbackContext modifyCtx() {
        return new CallbackContext(CallbackContext.Op.MODIFY);
    }

    private Account newAccount(String name, Map<String, Object> attrs) throws ServiceException {
        return prov.createAccount(name, "test123", attrs);
    }

    @Test
    public void preModifyMakeAvailableTrueWithoutExtensionThrowsFailure() throws Exception {
        // Arrange
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-avail@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(AVAILABLE, ProvisioningConstants.TRUE);

        // Act / Assert - extension not deployed in test harness
        try {
            callback.preModify(modifyCtx(), AVAILABLE, ProvisioningConstants.TRUE, attrsToModify, account);
            fail("expected FAILURE because the 2FA extension is not deployed");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("extension is not deployed"));
        }
        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyMakeAvailableFalseOnAccountPasses() throws Exception {
        // Arrange - setting available=false does not hit the extension check
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-availfalse@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(AVAILABLE, ProvisioningConstants.FALSE);

        // Act / Assert - 2FA not required on the account, so unavailable is allowed
        callback.preModify(modifyCtx(), AVAILABLE, ProvisioningConstants.FALSE, attrsToModify, account);
        assertTrue("making 2FA unavailable on an account where it is not required must pass", true);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyRequireTrueWhenNotAvailableOnAccountThrowsFailure() throws Exception {
        // Arrange - account does not have 2FA available; requiring it must fail
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-require@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(REQUIRED, ProvisioningConstants.TRUE);

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), REQUIRED, ProvisioningConstants.TRUE, attrsToModify, account);
            fail("expected FAILURE - cannot require 2FA when not available");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not available on this account"));
        }
        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyRequireTrueWhenAvailableNowOnAccountPasses() throws Exception {
        // Arrange - same modify also sets available=true, so is2faAvailable() is true
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-reqavail@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(AVAILABLE, ProvisioningConstants.TRUE);
        attrsToModify.put(REQUIRED, ProvisioningConstants.TRUE);

        // Act / Assert - the REQUIRED callback sees available being set now and passes
        callback.preModify(modifyCtx(), REQUIRED, ProvisioningConstants.TRUE, attrsToModify, account);
        assertTrue("requiring 2FA while making it available in the same modify must pass", true);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyEnableTrueWhenNotAvailableOnAccountThrowsFailure() throws Exception {
        // Arrange
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-enable@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ENABLED, ProvisioningConstants.TRUE);

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), ENABLED, ProvisioningConstants.TRUE, attrsToModify, account);
            fail("expected FAILURE - cannot enable 2FA when not available");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not available on this account"));
        }
        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyEnableTrueAvailableNowButNoSecretThrowsFailure() throws Exception {
        // Arrange - available is set in the same modify, but no shared secret exists
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-nosecret@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(AVAILABLE, ProvisioningConstants.TRUE);
        attrsToModify.put(ENABLED, ProvisioningConstants.TRUE);

        // Act / Assert - available check passes, secret check fails
        try {
            callback.preModify(modifyCtx(), ENABLED, ProvisioningConstants.TRUE, attrsToModify, account);
            fail("expected FAILURE - shared secret unavailable");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("shared secret is unavailable"));
        }
        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyEnableFalseOnAccountPasses() throws Exception {
        // Arrange - disabling never triggers the enable guards
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-enablefalse@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ENABLED, ProvisioningConstants.FALSE);

        // Act / Assert
        callback.preModify(modifyCtx(), ENABLED, ProvisioningConstants.FALSE, attrsToModify, account);
        assertTrue("disabling 2FA must pass", true);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyRequireTrueWhenNotAvailableOnCosThrowsFailure() throws Exception {
        // Arrange - COS branch: require with no availability fails
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Cos cos = prov.createCos("tfa-cos-require", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(REQUIRED, ProvisioningConstants.TRUE);

        // Act / Assert
        try {
            callback.preModify(modifyCtx(), REQUIRED, ProvisioningConstants.TRUE, attrsToModify, cos);
            fail("expected FAILURE - cannot require 2FA when not available on COS");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not available on this COS"));
        }
    }

    @Test
    public void preModifyMakeAvailableTrueOnCosThrowsFailureWithoutExtension() throws Exception {
        // Arrange - the extension check at the top of preModify fires for ANY entry type
        // (Account or COS) whenever AVAILABLE is being set to true and the extension is absent.
        // It runs BEFORE the COS-specific unset-last-reset branch is ever reached.
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put("zimbraTwoFactorAuthLastReset", "20200101000000Z");
        Cos cos = prov.createCos("tfa-cos-avail", cosAttrs);
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(AVAILABLE, ProvisioningConstants.TRUE);

        // Act / Assert - extension not deployed in test harness, so making it available fails
        try {
            callback.preModify(modifyCtx(), AVAILABLE, ProvisioningConstants.TRUE, attrsToModify, cos);
            fail("expected FAILURE because the 2FA extension is not deployed");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("extension is not deployed"));
        }
    }

    @Test
    public void preModifyUnrelatedAttrNameIsNoop() throws Exception {
        // Arrange - an attr the callback does not care about, on an account
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();
        Account account = newAccount("tfa-unrelated@example.com", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();

        // Act / Assert - none of the guarded attr names match, so it passes
        callback.preModify(modifyCtx(), "zimbraPrefSkin", "serenity", attrsToModify, account);
        assertTrue("unrelated attribute must be a no-op", true);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void postModifyIsNoopDoesNotThrow() throws Exception {
        // Arrange
        TwoFactorAuthStatus callback = new TwoFactorAuthStatus();

        // Act / Assert
        callback.postModify(modifyCtx(), AVAILABLE, null);
        assertNotNull(callback);
    }
}
