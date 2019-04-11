/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016, 2019 Synacor, Inc.
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

package com.zimbra.cs.account.krb5;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.io.IOException;

public class Krb5Login {

    public static void verifyPassword(String principal, String password) throws LoginException {
        LoginContext lc = null;
        try {
            ZimbraLog.account.debug("Kerberos(krb5) principal login with password: %s and password: %s", principal, password);
            lc = Krb5Login.withPassword(principal, password);
            lc.login();
        } finally {
            if (lc != null) {
                try {
                    //Only log out if a login occured. If a login didn't occur, then a logout throws a NPE.
                    if (lc.getSubject() != null
                            && !lc.getSubject().getPrincipals().isEmpty()) {
                        ZimbraLog.account.debug("Kerberos(krb5) Login Context subject and principal are not null. Safely logging out.");
                    lc.logout();
                    } else {
                        ZimbraLog.account.debug("Kerberos(krb5) Login Context subject and principal are null. Cannot safely log out.");
                    }
                } catch (LoginException le) {
                    ZimbraLog.account.warn("krb5 logout failed", le);
                }
            }
        }
    }

    public static void performAs(String principal, String keytab, PrivilegedExceptionAction action) throws PrivilegedActionException, LoginException {
        LoginContext lc = null;
        try {
            // Authenticate to Kerberos.
            lc = Krb5Login.withKeyTab(principal, keytab);
            lc.login();
            
            // Assume the identity of the authenticated principal. 
            Subject.doAs(lc.getSubject(), action);
            
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
        /*
         * com.sun.security.auth.module.Krb5LoginModule required 
         * useKeyTab=true 
         * debug=true 
         * keyTab="/apps/workgroup-audit/keytab/keytab.workgroup-audit" 
         * doNotPrompt=true 
         * storeKey=true 
         * principal="service/workgroup-audit@stanford.edu" 
         * useTicketCache=true
         */
        Krb5Config kc = Krb5Config.getInstance();
        // kc.setDebug(true);
        kc.setPrincipal(principal);
        kc.setKeyTab(keytab);
        kc.setStoreKey(true);
        kc.setDoNotPrompt(true);
        kc.setUseTicketCache(true);
        Configuration dc = new DynamicConfiguration(S_CONFIG_NAME, new AppConfigurationEntry[] {kc});
        return new LoginContext(S_CONFIG_NAME, null, null, dc);
    }

    /**
     * Constructs a new Krb5Config entry with the specified
     * ticket cache and logs in with that entry. 
     *<p>If <code>cache</code> is <code>null</code>, then
     * <code>Krb5Config.setTicketCache</code> will be set to
     * <code>true</code> and the default ticket cache will be used.
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

        /**
         * Refresh method
         */
        public void refresh() {}
    }

    public static class Krb5Config extends AppConfigurationEntry
    {
        private Map<String, String> mOptions;

        public static final AppConfigurationEntry.LoginModuleControlFlag
            DEFAULT_CONTROL_FLAG =
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;

        private static final String DEFAULT_LOGIN_MODULE_NAME =
                "com.sun.security.auth.module.Krb5LoginModule";

        private Krb5Config(String loginModuleName, LoginModuleControlFlag controlFlag, Map<String, ?> options) {
            super(loginModuleName, controlFlag, options);
        }

        public static Krb5Config getInstance() {
            HashMap<String, String> options = new HashMap<String, String>();
            Krb5Config kc = new Krb5Config(DEFAULT_LOGIN_MODULE_NAME, DEFAULT_CONTROL_FLAG, options);
            kc.mOptions = options;
            return kc;
        }

        public Krb5Config setDebug(boolean value) { mOptions.put("debug", value ? "true" : "false"); return this; }
        public Krb5Config setDoNotPrompt(boolean value)   { mOptions.put("doNotPrompt", value ? "true" : "false"); return this;}
        public Krb5Config setKeyTab(String filename)      { mOptions.put("keyTab", filename); setUseKeyTab(true); return this; }
        public Krb5Config setPrincipal(String principal)  { mOptions.put("principal", principal); return this; }
        public Krb5Config setStoreKey(boolean value)      { mOptions.put("storeKey", value ? "true" : "false"); return this; }
        public Krb5Config setTicketCache(String filename) { mOptions.put("ticketCache", filename); setUseTicketCache(true); return this; }
        public Krb5Config setUseKeyTab(boolean value)     { mOptions.put("useKeyTab", value ? "true" : "false"); return this; }
        public Krb5Config setUseTicketCache(boolean value) { mOptions.put("useTicketCache", value ? "true" : "false"); return this;}
    }
    
    static class DummyAction implements PrivilegedExceptionAction {
        String mArg;
        
        DummyAction(String arg) {
            mArg = arg;
        }
        
        public Object run() throws Exception {
            System.out.println("arg is " + mArg);
            throw new Exception("exception thrown from run");
        }
    }
    
    static class SearchAction implements PrivilegedExceptionAction {

        public Object run() {

            // Set up the environment for creating the initial context
            Hashtable env = new Hashtable(11);
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

            // env.put(Context.PROVIDER_URL, "ldap://ldap.stanford.edu:389/");
            env.put(Context.PROVIDER_URL, "ldap://localhost:389/");
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            env.put("javax.security.sasl.qop", "auth-conf");

            DirContext ctx = null;
            try {
               // Create initial context
               ctx = new InitialDirContext(env);

               SearchControls ctls = new SearchControls();
               ctls.setReturningAttributes(
                     new String[] {"displayName", "mail","description"});

               NamingEnumeration answer =
                    ctx.search("", "(cn=*)", ctls);

               return answer;


            } catch (Exception e) {
                   e.printStackTrace();
            }
             // Close the context when we're done
            finally {
                if (!(ctx == null)) {
                  try {
                          ctx.close();
                  }
                  catch (Exception closeProblem) {
                           System.err.println("error closing Context - " + closeProblem.getMessage());
                  }
              }
            }
            return null;
        }
    }
    
    private static void testPerformAs() {
        try {
            // performAs("service/workgroup-audit@stanford.edu", "/apps/workgroup-audit/keytab/keytab.workgroup-audit", new Dummy("phoebe"));
            // performAs("service/stan-ldap-test@PHOEBE.LOCAL", "/etc/krb5.keytab", new DummyAction("phoebe"));
            // performAs("ldap/phoebe.local@PHOEBE.LOCAL", "/etc/krb5.keytab", new SearchAction());
            performAs("ldap/phoebe.local@PHOEBE.LOCAL", "/etc/krb5.keytab", new SearchAction());
        } catch (LoginException le) {
            le.printStackTrace();
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            System.out.println("exception msg is: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String arsg[]) throws LoginException {
        // verifyPassword("user1@MACPRO.LOCAL", "test123");
        testPerformAs();
        
        System.out.println("succeeded");
    }
}
