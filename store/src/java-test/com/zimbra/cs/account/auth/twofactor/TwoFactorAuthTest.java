package com.zimbra.cs.account.auth.twofactor;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.auth.twofactor.TwoFactorOptions;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.util.ZimbraTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link TwoFactorAuth} — static factory loading, CredentialConfig,
 * and TwoFactorChangeListener registry.
 *
 * Dependencies:
 * - LC key override via ZimbraTestUtil.setLcKey() (reflection — no LDAP)
 * - No DB, no network
 */
public class TwoFactorAuthTest {

    // -------------------------------------------------------------------------
    // Helpers to reset the static factory field between tests
    // -------------------------------------------------------------------------

    private static final Field FACTORY_FIELD;

    static {
        try {
            FACTORY_FIELD = TwoFactorAuth.class.getDeclaredField("factory");
            FACTORY_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TwoFactorAuth.Factory savedFactory;

    @Before
    public void saveFactory() throws Exception {
        savedFactory = (TwoFactorAuth.Factory) FACTORY_FIELD.get(null);
    }

    @After
    public void restoreFactory() throws Exception {
        FACTORY_FIELD.set(null, savedFactory);
        ZimbraTestUtil.resetLcKey(LC.zimbra_class_two_factor_auth_factory);
    }

    // =========================================================================
    // setFactory / getFactory
    // =========================================================================

    @Test
    public void setFactory_null_clearsFactory() throws Exception {
        TwoFactorAuth.setFactory((String) null);
        assertNull(FACTORY_FIELD.get(null));
    }

    @Test
    public void setFactory_knownClass_instantiatesIt() throws Exception {
        TwoFactorAuth.setFactory(TwoFactorAuth.DefaultFactory.class.getName());
        TwoFactorAuth.Factory f = (TwoFactorAuth.Factory) FACTORY_FIELD.get(null);
        assertNotNull(f);
        assertTrue(f instanceof TwoFactorAuth.DefaultFactory);
    }

    @Test
    public void setFactory_unknownClass_fallsBackToDefault() throws Exception {
        TwoFactorAuth.setFactory("com.zimbra.nonexistent.FactoryClass");
        TwoFactorAuth.Factory f = (TwoFactorAuth.Factory) FACTORY_FIELD.get(null);
        assertNotNull(f);
        assertTrue("fallback must be DefaultFactory", f instanceof TwoFactorAuth.DefaultFactory);
    }

    @Test
    public void getFactory_whenFactoryNull_loadsFromLcKey() throws Exception {
        // Clear factory first, then set LC key to a valid class
        FACTORY_FIELD.set(null, null);
        ZimbraTestUtil.setLcKey(LC.zimbra_class_two_factor_auth_factory,
                TwoFactorAuth.DefaultFactory.class.getName());
        TwoFactorAuth.Factory f = TwoFactorAuth.getFactory();
        assertNotNull(f);
        assertTrue(f instanceof TwoFactorAuth.DefaultFactory);
    }

    @Test
    public void getFactory_whenFactoryAlreadySet_returnsSameInstance() throws Exception {
        TwoFactorAuth.setFactory(TwoFactorAuth.DefaultFactory.class.getName());
        TwoFactorAuth.Factory first  = (TwoFactorAuth.Factory) FACTORY_FIELD.get(null);
        TwoFactorAuth.Factory second = TwoFactorAuth.getFactory();
        assertSame(first, second);
    }

    // =========================================================================
    // DefaultFactory — returns TwoFactorAuthUnavailable / null
    // =========================================================================

    @Test
    public void defaultFactory_getTwoFactorAuth_returnsUnavailable() throws Exception {
        TwoFactorAuth.Factory factory = new TwoFactorAuth.DefaultFactory();
        TwoFactorAuth tfa = factory.getTwoFactorAuth((Account) null);
        assertNotNull(tfa);
        assertTrue(tfa instanceof TwoFactorAuthUnavailable);
    }

    @Test
    public void defaultFactory_getTrustedDevices_returnsNull() throws Exception {
        TwoFactorAuth.Factory factory = new TwoFactorAuth.DefaultFactory();
        assertNull(factory.getTrustedDevices((Account) null));
    }

    @Test
    public void defaultFactory_getAppSpecificPasswords_returnsNull() throws Exception {
        TwoFactorAuth.Factory factory = new TwoFactorAuth.DefaultFactory();
        assertNull(factory.getAppSpecificPasswords((Account) null));
    }

    @Test
    public void defaultFactory_getScratchCodes_returnsNull() throws Exception {
        TwoFactorAuth.Factory factory = new TwoFactorAuth.DefaultFactory();
        assertNull(factory.getScratchCodes((Account) null));
    }

    // =========================================================================
    // TwoFactorAuthUnavailable — no-op overrides
    // =========================================================================

    @Test
    public void unavailable_twoFactorAuthRequired_returnsFalse() throws Exception {
        TwoFactorAuth tfa = new TwoFactorAuthUnavailable(null);
        assertTrue(!tfa.twoFactorAuthRequired());
    }

    @Test
    public void unavailable_twoFactorAuthEnabled_returnsFalse() throws Exception {
        TwoFactorAuth tfa = new TwoFactorAuthUnavailable(null);
        assertTrue(!tfa.twoFactorAuthEnabled());
    }

    // =========================================================================
    // CredentialConfig — builder + byte calculation
    // =========================================================================

    @Test
    public void credentialConfig_settersAndGetters_roundTrip() {
        TwoFactorAuth.CredentialConfig cfg = new TwoFactorAuth.CredentialConfig()
                .setSecretLength(32)
                .setEncoding(TwoFactorOptions.Encoding.BASE32)
                .setScratchCodeLength(8)
                .setScratchCodeEncoding(TwoFactorOptions.Encoding.BASE64)
                .setNumScratchCodes(10);

        assertEquals(32, cfg.getSecretLength());
        assertEquals(TwoFactorOptions.Encoding.BASE32, cfg.getEncoding());
        assertEquals(8, cfg.getScratchCodeLength());
        assertEquals(TwoFactorOptions.Encoding.BASE64, cfg.getScratchCodeEncoding());
        assertEquals(10, cfg.getNumScratchCodes());
    }

    @Test
    public void credentialConfig_getBytesPerSecret_base32() {
        TwoFactorAuth.CredentialConfig cfg = new TwoFactorAuth.CredentialConfig()
                .setSecretLength(16)
                .setEncoding(TwoFactorOptions.Encoding.BASE32);
        // (16 / 8) * 5 = 10
        assertEquals(10, cfg.getBytesPerSecret());
    }

    @Test
    public void credentialConfig_getBytesPerSecret_base64() {
        TwoFactorAuth.CredentialConfig cfg = new TwoFactorAuth.CredentialConfig()
                .setSecretLength(16)
                .setEncoding(TwoFactorOptions.Encoding.BASE64);
        // (16 / 4) * 3 = 12
        assertEquals(12, cfg.getBytesPerSecret());
    }

    @Test
    public void credentialConfig_getBytesPerScratchCode_base32() {
        TwoFactorAuth.CredentialConfig cfg = new TwoFactorAuth.CredentialConfig()
                .setScratchCodeLength(8)
                .setScratchCodeEncoding(TwoFactorOptions.Encoding.BASE32);
        // (8 / 8) * 5 = 5
        assertEquals(5, cfg.getBytesPerScratchCode());
    }

    @Test
    public void credentialConfig_getBytesPerScratchCode_base64() {
        TwoFactorAuth.CredentialConfig cfg = new TwoFactorAuth.CredentialConfig()
                .setScratchCodeLength(8)
                .setScratchCodeEncoding(TwoFactorOptions.Encoding.BASE64);
        // (8 / 4) * 3 = 6
        assertEquals(6, cfg.getBytesPerScratchCode());
    }

    @Test
    public void credentialConfig_settersReturnSameInstance_forChaining() {
        TwoFactorAuth.CredentialConfig cfg = new TwoFactorAuth.CredentialConfig();
        assertSame(cfg, cfg.setSecretLength(10));
        assertSame(cfg, cfg.setEncoding(TwoFactorOptions.Encoding.BASE32));
        assertSame(cfg, cfg.setScratchCodeLength(6));
        assertSame(cfg, cfg.setScratchCodeEncoding(TwoFactorOptions.Encoding.BASE64));
        assertSame(cfg, cfg.setNumScratchCodes(5));
    }

    // =========================================================================
    // TwoFactorChangeListener — register + invoke
    // =========================================================================

    @Test
    public void changeListener_register_invokesOnEnable() {
        final boolean[] called = {false};
        String name = "test-listener-enable-" + System.nanoTime();

        TwoFactorAuth.TwoFactorChangeListener.register(name,
                new TwoFactorAuth.TwoFactorChangeListener() {
                    @Override public void twoFactorAuthEnabled(Account acct)  { called[0] = true; }
                    @Override public void twoFactorAuthDisabled(Account acct) {}
                    @Override public void appSpecificPasswordRevoked(Account acct, String app) {}
                });

        TwoFactorAuth.TwoFactorChangeListener.invokeEnabled(null);
        assertTrue("listener must have been invoked on invokeEnabled()", called[0]);
    }

    @Test
    public void changeListener_register_invokesOnDisable() {
        final boolean[] called = {false};
        String name = "test-listener-disable-" + System.nanoTime();

        TwoFactorAuth.TwoFactorChangeListener.register(name,
                new TwoFactorAuth.TwoFactorChangeListener() {
                    @Override public void twoFactorAuthEnabled(Account acct)  {}
                    @Override public void twoFactorAuthDisabled(Account acct) { called[0] = true; }
                    @Override public void appSpecificPasswordRevoked(Account acct, String app) {}
                });

        TwoFactorAuth.TwoFactorChangeListener.invokeDisabled(null);
        assertTrue("listener must have been invoked on invokeDisabled()", called[0]);
    }

    @Test
    public void changeListener_revokeAppPassword_invokesListener() {
        final String[] receivedApp = {null};
        String name = "test-listener-revoke-" + System.nanoTime();

        TwoFactorAuth.TwoFactorChangeListener.register(name,
                new TwoFactorAuth.TwoFactorChangeListener() {
                    @Override public void twoFactorAuthEnabled(Account acct)  {}
                    @Override public void twoFactorAuthDisabled(Account acct) {}
                    @Override public void appSpecificPasswordRevoked(Account acct, String app) {
                        receivedApp[0] = app;
                    }
                });

        TwoFactorAuth.TwoFactorChangeListener.revokeAppPassword(null, "myApp");
        assertEquals("myApp", receivedApp[0]);
    }

    @Test
    public void changeListener_duplicateRegister_doesNotThrow() {
        String name = "test-listener-dup-" + System.nanoTime();
        TwoFactorAuth.TwoFactorChangeListener stub = new TwoFactorAuth.TwoFactorChangeListener() {
            @Override public void twoFactorAuthEnabled(Account acct)  {}
            @Override public void twoFactorAuthDisabled(Account acct) {}
            @Override public void appSpecificPasswordRevoked(Account acct, String app) {}
        };
        TwoFactorAuth.TwoFactorChangeListener.register(name, stub);
        TwoFactorAuth.TwoFactorChangeListener.register(name, stub); // second call must not throw
    }
}
