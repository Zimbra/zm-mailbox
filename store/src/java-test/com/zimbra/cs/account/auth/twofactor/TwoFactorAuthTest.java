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

package com.zimbra.cs.account.auth.twofactor;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.CredentialConfig;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.DefaultFactory;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.Factory;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth.TwoFactorChangeListener;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link TwoFactorAuth}, its {@link DefaultFactory}, the
 * {@link CredentialConfig} builder/calculations, and the static
 * {@link TwoFactorChangeListener} registry. Uses real {@link Account} objects
 * from the in-memory MockProvisioning harness and a concrete TwoFactorAuth
 * subclass to drive the enable()/disable() listener-callback workflow.
 */
public class TwoFactorAuthTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        prov.createAccount("tfa@example.com", "secret", new HashMap<String, Object>());
        // Reset to the well-known default factory before each test.
        TwoFactorAuth.setFactory("com.zimbra.cs.account.auth.twofactor.TwoFactorAuth$DefaultFactory");
    }

    private Account fixture() throws Exception {
        return prov.get(AccountBy.name, "tfa@example.com");
    }

    /** Minimal concrete subclass to exercise the non-abstract enable()/disable() flow. */
    private static class RecordingTwoFactorAuth extends TwoFactorAuth {
        private boolean enabledCalled;

        private boolean disabledCalled;

        private boolean lastDeleteCredentials;

        RecordingTwoFactorAuth(Account account) {
            super(account, account.getName());
        }

        @Override
        public boolean twoFactorAuthRequired() {
            return false;
        }

        @Override
        public boolean twoFactorAuthEnabled() {
            return false;
        }

        @Override
        public void enableTwoFactorAuth() {
            enabledCalled = true;
        }

        @Override
        public void disableTwoFactorAuth(boolean deleteCredentials) {
            disabledCalled = true;
            lastDeleteCredentials = deleteCredentials;
        }

        @Override
        public CredentialConfig getCredentialConfig() {
            return null;
        }

        @Override
        public AuthenticatorConfig getAuthenticatorConfig() {
            return null;
        }

        @Override
        public Credentials generateCredentials() {
            return null;
        }

        @Override
        public void authenticateTOTP(String code) { }

        @Override
        public void authenticate(String secondFactor) { }

        @Override
        public void clearData() { }
    }

    /** Listener that records which Account-level callbacks fired. */
    private static class RecordingListener extends TwoFactorChangeListener {
        private Account enabledAcct;

        private Account disabledAcct;

        private String revokedApp;

        @Override
        public void twoFactorAuthEnabled(Account acct) {
            enabledAcct = acct;
        }

        @Override
        public void twoFactorAuthDisabled(Account acct) {
            disabledAcct = acct;
        }

        @Override
        public void appSpecificPasswordRevoked(Account acct, String appName) {
            disabledAcct = acct;
            revokedApp = appName;
        }
    }

    // ---------- Factory ----------

    @Test
    public void getFactoryDefaultReturnsDefaultFactory() throws Exception {
        // Act
        Factory factory = TwoFactorAuth.getFactory();

        // Assert
        assertNotNull("factory must be resolved", factory);
        assertTrue("LC default resolves to DefaultFactory, got " + factory.getClass(),
                factory instanceof DefaultFactory);
    }

    @Test
    public void defaultFactoryGetTwoFactorAuthReturnsUnavailableImpl() throws Exception {
        // Arrange
        Factory factory = TwoFactorAuth.getFactory();
        Account acct = fixture();

        // Act
        TwoFactorAuth tfa = factory.getTwoFactorAuth(acct);
        TwoFactorAuth tfa2 = factory.getTwoFactorAuth(acct, acct.getName());

        // Assert
        assertTrue("default impl is TwoFactorAuthUnavailable",
                tfa instanceof TwoFactorAuthUnavailable);
        assertTrue("named overload also returns unavailable impl",
                tfa2 instanceof TwoFactorAuthUnavailable);
    }

    @Test
    public void defaultFactorySecondaryAccessorsReturnNull() throws Exception {
        // Arrange
        Factory factory = TwoFactorAuth.getFactory();
        Account acct = fixture();

        // Act / Assert
        assertNull("trusted devices null by default", factory.getTrustedDevices(acct));
        assertNull("trusted devices (named) null", factory.getTrustedDevices(acct, acct.getName()));
        assertNull("app passwords null by default", factory.getAppSpecificPasswords(acct));
        assertNull("app passwords (named) null", factory.getAppSpecificPasswords(acct, acct.getName()));
        assertNull("scratch codes null by default", factory.getScratchCodes(acct));
        assertNull("scratch codes (named) null", factory.getScratchCodes(acct, acct.getName()));
    }

    @Test
    public void setFactoryNullClearsThenLcRebuildsDefault() throws Exception {
        // Arrange
        TwoFactorAuth.setFactory(null);

        // Act: getFactory sees null and rebuilds from LC.
        Factory factory = TwoFactorAuth.getFactory();

        // Assert
        assertTrue("null clears factory; getFactory rebuilds DefaultFactory from LC",
                factory instanceof DefaultFactory);
    }

    @Test
    public void setFactoryUnknownClassNameFallsBackToDefault() throws Exception {
        // Act: a class name that exists nowhere triggers the CNFE -> default fallback.
        TwoFactorAuth.setFactory("com.zimbra.does.not.Exist");

        // Assert
        Factory factory = TwoFactorAuth.getFactory();
        assertTrue("unknown factory class falls back to DefaultFactory",
                factory instanceof DefaultFactory);
    }

    @Test
    public void setFactoryNonFactoryClassNameFallsBackToDefault() throws Exception {
        // Act: a real class that is NOT a Factory triggers the ClassCastException path.
        TwoFactorAuth.setFactory("java.lang.String");

        // Assert
        Factory factory = TwoFactorAuth.getFactory();
        assertTrue("non-Factory class falls back to DefaultFactory",
                factory instanceof DefaultFactory);
    }

    // ---------- enable()/disable() listener workflow ----------

    @Test
    public void enableFiresEnabledListenerCallback() throws Exception {
        // Arrange
        Account acct = fixture();
        RecordingTwoFactorAuth tfa = new RecordingTwoFactorAuth(acct);
        RecordingListener listener = new RecordingListener();
        TwoFactorChangeListener.register("enableTest-" + System.nanoTime(), listener);

        // Act
        tfa.enable();

        // Assert
        assertTrue("enableTwoFactorAuth was invoked", tfa.enabledCalled);
        assertSame("enabled listener received the account", acct, listener.enabledAcct);
    }

    @Test
    public void disableWithDeleteCredentialsFiresDisabledCallback() throws Exception {
        // Arrange
        Account acct = fixture();
        RecordingTwoFactorAuth tfa = new RecordingTwoFactorAuth(acct);
        RecordingListener listener = new RecordingListener();
        TwoFactorChangeListener.register("disableTest-" + System.nanoTime(), listener);

        // Act
        tfa.disable(true);

        // Assert
        assertTrue("disableTwoFactorAuth was invoked", tfa.disabledCalled);
        assertTrue("deleteCredentials flag propagated", tfa.lastDeleteCredentials);
        assertSame("disabled listener received the account", acct, listener.disabledAcct);
    }

    @Test
    public void constructorStoresAccountAndNamePassedIn() throws Exception {
        // Arrange
        Account acct = fixture();

        // Act
        RecordingTwoFactorAuth tfa = new RecordingTwoFactorAuth(acct);

        // Assert: protected fields visible within same package.
        assertSame("account stored", acct, tfa.account);
        assertEquals("name-passed-in stored", acct.getName(), tfa.acctNamePassedIn);
    }

    // ---------- TwoFactorChangeListener registry ----------

    @Test
    public void registerThenRevokeAppPasswordFiresRevokeCallback() throws Exception {
        // Arrange
        Account acct = fixture();
        RecordingListener listener = new RecordingListener();
        TwoFactorChangeListener.register("revokeTest-" + System.nanoTime(), listener);

        // Act
        TwoFactorChangeListener.revokeAppPassword(acct, "MailApp");

        // Assert
        assertEquals("revoked app name propagated", "MailApp", listener.revokedApp);
        assertSame("revoke callback received the account", acct, listener.disabledAcct);
    }

    @Test
    public void registerDuplicateNameKeepsFirstRegistration() throws Exception {
        // Arrange
        Account acct = fixture();
        String name = "dupTest-" + System.nanoTime();
        RecordingListener first = new RecordingListener();
        RecordingListener second = new RecordingListener();
        TwoFactorChangeListener.register(name, first);
        TwoFactorChangeListener.register(name, second);  // duplicate -> warned & ignored

        // Act
        TwoFactorChangeListener.invokeEnabled(acct);

        // Assert
        assertSame("first listener still registered under the name", acct, first.enabledAcct);
        assertNull("second (duplicate) listener was ignored", second.enabledAcct);
    }

    // ---------- CredentialConfig ----------

    @Test
    public void credentialConfigSettersAndGettersRoundTrip() {
        // Arrange / Act
        CredentialConfig cfg = new CredentialConfig()
                .setEncoding(TwoFactorOptions.Encoding.BASE32)
                .setSecretLength(16)
                .setScratchCodeEncoding(TwoFactorOptions.Encoding.BASE64)
                .setScratchCodeLength(8)
                .setNumScratchCodes(10);

        // Assert
        assertEquals("secret encoding round-trips", TwoFactorOptions.Encoding.BASE32, cfg.getEncoding());
        assertEquals("secret length round-trips", 16, cfg.getSecretLength());
        assertEquals("scratch encoding round-trips", TwoFactorOptions.Encoding.BASE64,
                cfg.getScratchCodeEncoding());
        assertEquals("scratch length round-trips", 8, cfg.getScratchCodeLength());
        assertEquals("num scratch codes round-trips", 10, cfg.getNumScratchCodes());
    }

    @Test
    public void getBytesPerSecretBase32ComputesFiveBytesPerEightChars() {
        // Arrange
        CredentialConfig cfg = new CredentialConfig()
                .setEncoding(TwoFactorOptions.Encoding.BASE32)
                .setSecretLength(16);

        // Act
        int bytes = cfg.getBytesPerSecret();

        // Assert: (16/8)*5 = 10
        assertEquals("BASE32 secret bytes computed", 10, bytes);
    }

    @Test
    public void getBytesPerScratchCodeBase64ComputesThreeBytesPerFourChars() {
        // Arrange
        CredentialConfig cfg = new CredentialConfig()
                .setScratchCodeEncoding(TwoFactorOptions.Encoding.BASE64)
                .setScratchCodeLength(8);

        // Act
        int bytes = cfg.getBytesPerScratchCode();

        // Assert: (8/4)*3 = 6
        assertEquals("BASE64 scratch-code bytes computed", 6, bytes);
    }
}
