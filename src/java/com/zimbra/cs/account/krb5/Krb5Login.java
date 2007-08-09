package com.zimbra.cs.account.krb5;

import com.zimbra.common.util.ZimbraLog;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.callback.PasswordCallback;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class Krb5Login {

    public static void verifyPassword(String principal, String password) throws LoginException {
        LoginContext lc = null;
        try {
            lc = Krb5Login.withPassword(principal, password);
            lc.login();
        } finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch(LoginException le) {
                    ZimbraLog.account.warn("krb5 logout failed", le);
                }
            }
        }
    }

    private static String S_CONFIG_NAME = "krb5";
    /**
     * private constructor.
     */
    private Krb5Login() {

    }

    /**
     * Constructs a new Krb5Config entry with the specified
     * principal and keytab, logs in with that entry, and
     * then removes that entry and returns the new LoginContext.
     * <p>Equivalent to the following calls:
     *<pre>
     * Krb5Config kc = Krb5Config.getInstance();
     * kc.setPrincipal(principal);
     * kc.setKeyTab(keytab);
     * kc.setStoreKey(true);
     * LoginContext lc = Login.login(kc);
     *</pre>
     */
    public static LoginContext withKeyTab(String principal,
                                          String keytab)
        throws LoginException
    {
        Krb5Config kc = Krb5Config.getInstance();
        kc.setPrincipal(principal);
        kc.setKeyTab(keytab);
        kc.setStoreKey(true);
        Configuration dc = new DynamicConfiguration(S_CONFIG_NAME, new AppConfigurationEntry[] {kc});
        return new LoginContext(S_CONFIG_NAME, null, null, dc);
    }

    /**
     * Constructs a new Krb5Config entry with the specified
     * ticket cache and logs in with that entry. 
     *<p>If <code>cache</code> is <code>null</code>, then
     * <code>Krb5Config.setTicketCache</code> will be set to
     * <code>true</code> and the default ticekt cache will be used.
     * <p>Equivalent to the following calls:
     *<pre>
     * Krb5Config kc = Krb5Config.getInstance();
     * if (cache != null) 
     *    kc.setTicketCache(cache);
     * else
     *    kc.setUseTicketCache(true);
     * LoginContext lc = Login.login(kc);
     *</pre>
     */
    public static LoginContext withTicketCache(String cache)
        throws LoginException
    {
        Krb5Config kc = Krb5Config.getInstance();
        //kc.setStoreKey(true);
        if (cache != null) {
            kc.setTicketCache(cache);
        } else {
            kc.setUseTicketCache(true);
        }
        Configuration dc = new DynamicConfiguration(S_CONFIG_NAME, new AppConfigurationEntry[] {kc});
        return new LoginContext(S_CONFIG_NAME, null, null, dc);
    }

    public static LoginContext withPassword(String name, final String password)
        throws LoginException
    {
        Krb5Config kc = Krb5Config.getInstance();
        kc.setPrincipal(name);
        kc.setUseTicketCache(false);
        kc.setStoreKey(false);
        Configuration dc = new DynamicConfiguration(S_CONFIG_NAME, new AppConfigurationEntry[] {kc});
        CallbackHandler handler = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callback;
                        pc.setPassword(password.toCharArray());
                    }
                }
            }
        };
        return new LoginContext(S_CONFIG_NAME, null, handler, dc);
    }

    static class DynamicConfiguration extends Configuration
    {
        private String mName;
        private AppConfigurationEntry[] mEntry;

        DynamicConfiguration(String name, AppConfigurationEntry[] entry) {
            mName = name;
            mEntry = entry;
        }

        /**
         * Retrieve an array of AppConfigurationEntries which
         * corresponds to the configuration of LoginModules for
         * the given application.
         */
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return name.equals(mName) ? mEntry : null;
        }

        public void refresh() {}
    }

    public static class Krb5Config extends AppConfigurationEntry
    {
        private Map<String, String> mOptions;

        private Krb5Config(String loginModuleName, LoginModuleControlFlag controlFlag, Map<String, ?> options) {
            super(loginModuleName, controlFlag, options);
        }

        public static final AppConfigurationEntry.LoginModuleControlFlag
                DEFAULT_CONTROL_FLAG =
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;

        private static final String DEFAULT_LOGIN_MODULE_NAME =
                "com.sun.security.auth.module.Krb5LoginModule";

        public static Krb5Config getInstance() {
            HashMap<String, String> options = new HashMap<String, String>();
            Krb5Config kc = new Krb5Config(DEFAULT_LOGIN_MODULE_NAME, DEFAULT_CONTROL_FLAG, options);
            kc.mOptions = options;
            return kc;
        }

        public Krb5Config setDebug(boolean value) { mOptions.put("storeKey", value ? "true" : "false"); return this; }
        public Krb5Config setDoNotPrompt(boolean value)   { mOptions.put("doNotPrompt", value ? "true" : "false"); return this;}
        public Krb5Config setKeyTab(String filename)      { mOptions.put("keyTab", filename); setUseKeyTab(true); return this; }
        public Krb5Config setPrincipal(String principal)  { mOptions.put("principal", principal); return this; }
        public Krb5Config setStoreKey(boolean value)      { mOptions.put("storeKey", value ? "true" : "false"); return this; }
        public Krb5Config setTicketCache(String filename) { mOptions.put("ticketCache", filename); setUseTicketCache(true); return this; }
        public Krb5Config setUseKeyTab(boolean value)     { mOptions.put("useKeyTab", value ? "true" : "false"); return this; }
        public Krb5Config setUseTicketCache(boolean value) { mOptions.put("useTicketCache", value ? "true" : "false"); return this;}
    }

    public static void main(String arsg[]) throws LoginException {
        verifyPassword("user1@MACPRO.LOCAL", "test123");
    }
}
