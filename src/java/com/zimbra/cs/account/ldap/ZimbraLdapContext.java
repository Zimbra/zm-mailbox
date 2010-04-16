/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.InvalidAttributesException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SSLSocketFactoryWrapper;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * 
 * @author pshao
 *
 */
public class ZimbraLdapContext {

    private static String sLdapURL;
    private static String sLdapMasterURL;    
    private static ConnType sConnType;
    private static ConnType sMasterConnType;
    private static String sStartTLSDebugText;
    
    private static Hashtable<String, String> sEnvMasterAuth;
    private static Hashtable<String, String> sEnvAuth;
    
    private static SSLSocketFactory sDummySSLSocketFactory;
    
    private LdapContext mDirContext;
    private StartTlsResponse mTlsResp;
    
    private static final int CHECK_LDAP_SLEEP_MILLIS = 5000;

    private static enum ConnType {
        PLAIN,
        LDAPS,
        STARTTLS;
        
        private static ConnType getConnType(String urls) {
            if (urls.toLowerCase().contains("ldaps://"))
                return LDAPS;
            
            boolean ldap_starttls_supported = "1".equals(LC.ldap_starttls_supported.value());
            boolean zimbra_require_interprocess_security = "1".equals(LC.zimbra_require_interprocess_security.value());
            
            if (ldap_starttls_supported && zimbra_require_interprocess_security)
                return STARTTLS;
            
            return PLAIN;
        }
        
        private static boolean isLDAPS(boolean master) {
            if (master)
                return (sMasterConnType == ConnType.LDAPS);
            else
                return (sConnType == ConnType.LDAPS);
        }
        
        private static boolean isSTARTTLS(boolean master) {
            if (master)
                return (sMasterConnType == ConnType.STARTTLS);
            else
                return (sConnType == ConnType.STARTTLS);
        }
    }
    
    /*
     * For specifying a custom LDAP env that uses provided pool pref, connection timeout and read timeout settings 
     */
    public static class LdapConfig {
        public static final Integer NO_TIMEOUT = 0; // wait infinitely
        
        private Boolean mUseConnPool;
        private Integer mConnTimeout;
        private Integer mReadTimeout;
        
        public LdapConfig(Boolean useConnPool, Integer connTimeout, Integer readTimeout) {
            mUseConnPool = useConnPool;
            mConnTimeout = connTimeout;
            mReadTimeout = readTimeout;
        }
        
        private String useConnPool() {
            return (mUseConnPool == null) ? "true" : mUseConnPool.toString();
        }
        
        private String connTimeout() {
            return (mConnTimeout == null) ? LC.ldap_connect_timeout.value() : String.valueOf(mConnTimeout);
        }
        
        private String readTimeout() {
            return (mReadTimeout == null) ? LC.ldap_read_timeout.value() : String.valueOf(mReadTimeout);
        }
    }
    
    static {
        
        sLdapURL = LC.ldap_url.value().trim();
        if (sLdapURL.length() == 0) {
            String ldapHost = LC.ldap_host.value();
            String ldapPort = LC.ldap_port.value();
            sLdapURL = "ldap://" + ldapHost + ":" + ldapPort + "/";
        }
        sLdapMasterURL = LC.ldap_master_url.value().trim();
        if (sLdapMasterURL.length() == 0) sLdapMasterURL = sLdapURL;

        /* See http://java.sun.com/products/jndi/tutorial/ldap/connect/config.html */
        System.setProperty("com.sun.jndi.ldap.connect.pool.debug", LC.ldap_connect_pool_debug.value());
        System.setProperty("com.sun.jndi.ldap.connect.pool.initsize", LC.ldap_connect_pool_initsize.value());
        System.setProperty("com.sun.jndi.ldap.connect.pool.maxsize", LC.ldap_connect_pool_maxsize.value());
        System.setProperty("com.sun.jndi.ldap.connect.pool.prefsize", LC.ldap_connect_pool_prefsize.value());
        System.setProperty("com.sun.jndi.ldap.connect.pool.timeout", LC.ldap_connect_pool_timeout.value());
        System.setProperty("com.sun.jndi.ldap.connect.pool.protocol", "plain ssl");
        
        sConnType = ConnType.getConnType(sLdapURL);
        sMasterConnType = ConnType.getConnType(sLdapMasterURL);
        
        /* crt should be imported to the default truststore JSSE looks for at 
         * {java.home}/lib/security/cacerts  (${zimbra_java_home}/lib/security/cacerts)
         * Thus we do not need to set javax.net.ssl.trustStore here.
         * Maybe this is why it is not set in FirstServlet.
         */
        /*
        if (ConnType.isSTARTTLS(true) || ConnType.isSTARTTLS(false))
            System.setProperty("javax.net.ssl.trustStore", LC.mailboxd_truststore.value());
        */    
        
        sDummySSLSocketFactory = SocketFactories.dummySSLSocketFactory();
        
        // setup debug text
        StringBuffer startTLSDebugText = new StringBuffer("START TLS");
        sStartTLSDebugText = "START TLS";
        if (LC.ssl_allow_mismatched_certs.booleanValue())
            startTLSDebugText.append(", allow mismatched certs");
        if (LC.ssl_allow_untrusted_certs.booleanValue())
            startTLSDebugText.append(", allow untrusted certs");
        sStartTLSDebugText = startTLSDebugText.toString();
    }

    /*
     * called from ProvUtil
     */
    public static synchronized void forceMasterURL() {
        sLdapURL = sLdapMasterURL;
        sConnType = sMasterConnType;
    }
    
    public static String getLdapURL() {
        return sLdapURL;
    }
    
    /*
     * for external LDAP
     */
    public static boolean requireStartTLS(String[] urls, boolean startTLSEnabled) {
        if (startTLSEnabled) {
            for (String url : urls) {
                if (url.toLowerCase().contains("ldaps://"))
                    return false;
            }
            return true;
        }
        return false;
    }
    
    private static synchronized Hashtable<String, String> getDefaultEnv(boolean master) {
        Hashtable<String, String> sEnv = null;
        
        if (master) {
            if (sEnvMasterAuth != null) return sEnvMasterAuth;
            else sEnv = sEnvMasterAuth = new Hashtable<String, String>(); 
        } else {
            if (sEnvAuth != null) return sEnvAuth;
            else sEnv = sEnvAuth = new Hashtable<String, String>();             
        }

        sEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        sEnv.put(Context.PROVIDER_URL, master ? sLdapMasterURL : sLdapURL);
        sEnv.put(Context.REFERRAL, "follow");
        sEnv.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        sEnv.put("com.sun.jndi.ldap.read.timeout", LC.ldap_read_timeout.value());
        
        // env.put("java.naming.ldap.derefAliases", "never");
        // default: env.put("java.naming.ldap.version", "3");
        
        if (ConnType.isSTARTTLS(master)) {
            /*
             * if startTLS is required:
             *     1. cannot use connection pooling.
             *        see http://java.sun.com/products/jndi/tutorial/ldap/connect/pool.html
             * 
             *     2. do not send credentials over before TLS negotiation
             *        also note that after TLS negotiation, the credentials in env would be wiped, 
             *        so we'll have to add the credentials to the env again after TLS negotiation anyway.
             */
            sEnv.put("com.sun.jndi.ldap.connect.pool", "false");

        } else {
            if (master)
                sEnv.put("com.sun.jndi.ldap.connect.pool", LC.ldap_connect_pool_master.value());
            else
                sEnv.put("com.sun.jndi.ldap.connect.pool", "true");
            
            sEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            sEnv.put(Context.SECURITY_PRINCIPAL, LC.zimbra_ldap_userdn.value());
            sEnv.put(Context.SECURITY_CREDENTIALS, LC.zimbra_ldap_password.value());
            
            if (ConnType.isLDAPS(master)) {
                if (LC.ssl_allow_untrusted_certs.booleanValue()) {
                    sEnv.put("java.naming.ldap.factory.socket", DummySSLSocketFactory.class.getName());
                }
            }
        }

        return sEnv;
    }

    public static class DummySSLSocketFactory extends SSLSocketFactoryWrapper {
        // Bug 46264: JNDI actually calls getDefault() rather than just
        // creating a new instance.
        public static SocketFactory getDefault() {
            return new DummySSLSocketFactory();
        }
        public DummySSLSocketFactory() {
            super(SocketFactories.dummySSLSocketFactory());
        }
    }

    public static void main(String[] args) throws Exception {
        String name = DummySSLSocketFactory.class.getName();
        System.out.println("name = " + name);
        SSLSocketFactory ssf = (SSLSocketFactory) Class.forName(name).newInstance();
        ssf.createSocket("foo", 123);
    }


    private static synchronized Hashtable<String, String> getCustomEnv(boolean master, LdapConfig ldapConfig) {
        Hashtable<String, String> env = new Hashtable<String, String>(); 
        
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, master ? sLdapMasterURL : sLdapURL);
        env.put(Context.REFERRAL, "follow");
        env.put("com.sun.jndi.ldap.connect.timeout", ldapConfig.connTimeout());
        env.put("com.sun.jndi.ldap.read.timeout", ldapConfig.readTimeout());
        
        if (ConnType.isSTARTTLS(master)) {
            env.put("com.sun.jndi.ldap.connect.pool", "false");
        } else {
            if (master)
                env.put("com.sun.jndi.ldap.connect.pool", LC.ldap_connect_pool_master.value());
            else
                env.put("com.sun.jndi.ldap.connect.pool", ldapConfig.useConnPool());
            
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, LC.zimbra_ldap_userdn.value());
            env.put(Context.SECURITY_CREDENTIALS, LC.zimbra_ldap_password.value());
            
            if (ConnType.isLDAPS(master)) {
                if (LC.ssl_allow_untrusted_certs.booleanValue())
                    env.put("java.naming.ldap.factory.socket", "com.zimbra.common.util.EasySSLSocketFactory");
            }
        }
        
        return env;
    }
    
    private static String joinURLS(String urls[]) {
        if (urls.length == 1) return urls[0];
        StringBuffer url = new StringBuffer();
        for (int i=0; i < urls.length; i++) {
            if (i > 0) url.append(' ');
            url.append(urls[i]);
        }
        return url.toString();
    }
    
    private static class DummyHostVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            // System.out.println("Checking: " + hostname + " in");
            try {
                Certificate[] cert = session.getPeerCertificates();
                for (int i = 0; i < cert.length; i++) {
                    // System.out.println(cert[i]);
                }
            } catch (SSLPeerUnverifiedException e) {
                return false;
            }
    
            return true;
        }
    }
    
    private static void tlsNegotiate(StartTlsResponse tlsResp) throws IOException {
        
        ZimbraLog.ldap.debug(sStartTLSDebugText);
        
        if (LC.ssl_allow_mismatched_certs.booleanValue())
            tlsResp.setHostnameVerifier(new DummyHostVerifier());
        
        if (LC.ssl_allow_untrusted_certs.booleanValue())
            tlsResp.negotiate(sDummySSLSocketFactory);
        else
            tlsResp.negotiate();
    }
    
    /*
     * Zimbra LDAP
     */
    public ZimbraLdapContext() throws ServiceException {
        this(false, null);
    }

    /*
     * Zimbra LDAP
     */
    public ZimbraLdapContext(boolean master) throws ServiceException {
        this(master, null);
    }
    
    /*
     * Zimbra LDAP
     */
    public ZimbraLdapContext(boolean master, boolean useConnPool) throws ServiceException {
        // use custom ldap config if not using conn pool
        this(master, useConnPool? null : new LdapConfig(useConnPool, null, null));
    }
    
    /*
     * Zimbra LDAP
     * 
     * Used only for upgrade, not in server production.
     */
    public ZimbraLdapContext(boolean master, LdapConfig ldapConfig) throws ServiceException {
        try {
            Hashtable<String, String> env = (ldapConfig==null)? getDefaultEnv(master) : getCustomEnv(master, ldapConfig);
            boolean startTLS = ConnType.isSTARTTLS(master);
            
            long start = ZimbraPerf.STOPWATCH_LDAP_DC.start();
            
            if (ZimbraLog.ldap.isDebugEnabled())
                ZimbraLog.ldap.debug("GET DIR CTXT: " + "url=" + env.get(Context.PROVIDER_URL) + ", binddn="+ env.get(Context.SECURITY_PRINCIPAL) + ", startTLS=" + startTLS);
            mDirContext = new InitialLdapContext(env, null);
                        
            if (startTLS) {
                // start TLS
                mTlsResp = (StartTlsResponse) mDirContext.extendedOperation(new StartTlsRequest());
                tlsNegotiate(mTlsResp);

                mDirContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                mDirContext.addToEnvironment(Context.SECURITY_PRINCIPAL, LC.zimbra_ldap_userdn.value());
                mDirContext.addToEnvironment(Context.SECURITY_CREDENTIALS, LC.zimbra_ldap_password.value());
            }
            
            ZimbraPerf.STOPWATCH_LDAP_DC.stop(start);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("ZimbraLdapContext", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("ZimbraLdapContext", e);
        }
    }
    
    /*
     * External LDAP
     */
    public ZimbraLdapContext(String urls[], boolean requireStartTLS, String bindDn, String bindPassword, String note) throws NamingException, IOException {
        this(urls, requireStartTLS, null, bindDn, bindPassword, note);
    }
    
    /*
     * External LDAP
     */
    public ZimbraLdapContext(String urls[], boolean requireStartTLS, LdapGalCredential credential, String note)  throws NamingException, IOException {
        this(urls, requireStartTLS, credential.getAuthMech(), credential.getBindDn(), credential.getBindPassword(), note);
    }
    
    /*
     * External LDAP
     * 
     * Naming or IO exceptions are not caught then wrapped in a ServiceException like in the ZimbraLdapContext for Zimbra internal directory, 
     * because callsites of this method need to check for Naming/IOExceptions and log/handle/throw accordingly.  
     */
    public ZimbraLdapContext(String urls[], boolean requireStartTLS, String authMech, String bindDn, String bindPassword, String note)  throws NamingException, IOException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, joinURLS(urls));
        env.put(Context.REFERRAL, "follow");
        env.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        env.put("com.sun.jndi.ldap.read.timeout", LC.ldap_read_timeout.value());
        
        String derefAliases = LC.ldap_deref_aliases.value();
        if (!StringUtil.isNullOrEmpty(derefAliases))
            env.put("java.naming.ldap.derefAliases", LC.ldap_deref_aliases.value());
        
        if (authMech == null) {
            if (bindDn != null && bindPassword != null)
                authMech = Provisioning.LDAP_AM_SIMPLE;
            else
                authMech = Provisioning.LDAP_AM_NONE;
        }
        
        // do startTLS only if auth mech is simple
        boolean startTLS = (requireStartTLS && authMech.equals(Provisioning.LDAP_AM_SIMPLE));
        
        if (authMech.equals(Provisioning.LDAP_AM_NONE)) {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        } else if (authMech.equals(Provisioning.LDAP_AM_SIMPLE)) {
            if (!startTLS) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                if (bindDn != null)
                    env.put(Context.SECURITY_PRINCIPAL, bindDn);
                if (bindPassword != null)
                    env.put(Context.SECURITY_CREDENTIALS, bindPassword);  
            }
        } else if (authMech.equals(Provisioning.LDAP_AM_KERBEROS5)) {
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            env.put("javax.security.sasl.qop", "auth-conf");
        }
        
        // enable connection pooling if not doing startTLS
        if (!startTLS)
            env.put("com.sun.jndi.ldap.connect.pool", "true");
        

        try {
            if (ZimbraLog.ldap.isDebugEnabled())
                ZimbraLog.ldap.debug("GET DIR CTXT(" + note + "): " + "url=" + env.get(Context.PROVIDER_URL) + ", binddn="+ bindDn + ", authMech=" + authMech + ", startTLS=" + requireStartTLS);
            mDirContext = new InitialLdapContext(env, null);
            if (startTLS) {
                // start TLS
                mTlsResp = (StartTlsResponse) mDirContext.extendedOperation(new StartTlsRequest());
                tlsNegotiate(mTlsResp);
    
                mDirContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                if (bindDn != null)
                    mDirContext.addToEnvironment(Context.SECURITY_PRINCIPAL, bindDn);
                if (bindPassword != null)
                    mDirContext.addToEnvironment(Context.SECURITY_CREDENTIALS, bindPassword);
            }
        } catch (NamingException e) {
            ZimbraLog.ldap.debug("GET DIR CTXT(" + note + ") failed", e);
            throw e;
        } catch (IOException e) {
            ZimbraLog.ldap.debug("GET DIR CTXT(" + note + ") failed", e);
            throw e;
        }
    }
    
    /**
     * Authenticate to Zimbra LDAP.  Called when password is not SSHA encoded.
     * 
     * @param urls
     * @param requireStartTLS
     * @param principal
     * @param password
     * @throws NamingException
     */
    public static void ldapAuthenticate(String principal, String password) throws NamingException, IOException {
        String[] urls = new String[] { getLdapURL() };
        ldapAuthenticate(urls, ConnType.isSTARTTLS(false), principal, password, "Zimbra LDAP auth, password not SSHA");
    }
    
    /**
     * authenticate to external LDAP
     *  
     * @param urls
     * @param principal
     * @param password
     * @throws NamingException
     */
    static void ldapAuthenticate(String urls[], boolean requireStartTLS, String principal, String password, String note) throws NamingException, IOException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ZimbraLdapContext.joinURLS(urls));
        env.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        env.put("com.sun.jndi.ldap.read.timeout", LC.ldap_read_timeout.value());
        
        String derefAliases = LC.ldap_deref_aliases.value();
        if (!StringUtil.isNullOrEmpty(derefAliases))
            env.put("java.naming.ldap.derefAliases", LC.ldap_deref_aliases.value());
        
        // do NOT use connection pooling for LDAP auth, since each credential is different.
        
        if (!requireStartTLS) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, principal);
            env.put(Context.SECURITY_CREDENTIALS, password);
        }
        
        DirContext context = null;
        StartTlsResponse tlsResp = null;
        try {
            if (ZimbraLog.ldap.isDebugEnabled())
                ZimbraLog.ldap.debug("GET DIR CTXT(" + note + "): " + "url=" + env.get(Context.PROVIDER_URL) + ", binddn="+ principal + ", authMech=" + "simple" + ", startTLS=" + requireStartTLS);

            context = new InitialLdapContext(env, null);
            if (requireStartTLS) {
                // start TLS
                LdapContext ldapCtxt = (LdapContext)context;
                tlsResp = (StartTlsResponse) ldapCtxt.extendedOperation(new StartTlsRequest());
                tlsNegotiate(tlsResp);

                ldapCtxt.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                ldapCtxt.addToEnvironment(Context.SECURITY_PRINCIPAL, principal);
                ldapCtxt.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
                
                /*
                 * reconnect to the LDAP server using the credentials added to the env after TLS negotiation
                 * 
                 * This is not needed if the LdapContext will be used fpr operations (e.g. search, getAttributes) later, 
                 * because JNDI will do the bind when the op is executed.  reconnect is explicitly done here because the sole 
                 * purpose of this LdapContext is for authentication, and there is no op needed before closing the context.
                 * If we do not reconnect using the crendential, no auth is being done.
                 */ 
                ldapCtxt.reconnect(null);
            }
        } catch (NamingException e) {   
            ZimbraLog.ldap.debug("GET DIR CTXT(" + note + ") failed", e);
            throw e;
        } catch (IOException e) { 
            ZimbraLog.ldap.debug("GET DIR CTXT(" + note + ") failed", e);
            throw e;
        } finally {
            closeContext(context, tlsResp);
        }
    }
    
    /*
     * TODO: retire after cleanup LdapUtil
     */
    public LdapContext getLdapContext() {
        return mDirContext;
    }
    
    private static void closeContext(Context ctxt) {
        closeContext(ctxt, null);
    }
    
    private static void closeContext(Context ctxt, StartTlsResponse tlsResp) {
        try {
            // stop TLS
            if (tlsResp != null) {
                ZimbraLog.ldap.debug("STOP TLS");
                tlsResp.close();
            }
        } catch (IOException e) {
            ZimbraLog.ldap.error("failed to close tls", e);
        }
        
        try {
            // close the dir context
            if (ctxt != null) {
                ZimbraLog.ldap.debug("CLOSE DIR CTXT");
                ctxt.close();
            }
        } catch (NamingException e) {
            ZimbraLog.ldap.error("failed to close dir context", e);
        }
    }
    
    public static void closeContext(ZimbraLdapContext zlc) {
        if (zlc != null)
            zlc.closeContext();
    }
    
    private void closeContext() {
        closeContext(mDirContext, mTlsResp);
    }
    
    public DirContext getSchema() throws NamingException {
        return mDirContext.getSchema("");
    }
    
    public Attributes getAttributes(String dn) throws NamingException {
        Name cpName = new CompositeName().add(dn);
        
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("GET ATTRS: dn=" + dn);
        return mDirContext.getAttributes(cpName);
    }
    
    public void modifyAttributes(String dn, ModificationItem[] mods) throws NamingException {
        Name cpName = new CompositeName().add(dn);
        
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("MODIFY ATTRS: dn=" + dn + ", mods=" + dumpMods(mods));
        mDirContext.modifyAttributes(cpName, mods);
    }
    
    public void replaceAttributes(String dn, Attributes attrs) throws NamingException {
        Name cpName = new CompositeName().add(dn);
        
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("REPLACE ATTRS: dn=" + dn + ", mods=" + attrs.toString());
        mDirContext.modifyAttributes(cpName, DirContext.REPLACE_ATTRIBUTE, attrs);
    }
    
    public NamingEnumeration<SearchResult> searchDir(String base, String filter, SearchControls cons) throws NamingException {
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("SEARCH: base=" + base + ", filter=" + filter);
        
        if (base.length() == 0) {
            return mDirContext.search(base, filter, cons);
        } else {
            Name cpName = new CompositeName().add(base);
            return mDirContext.search(cpName, filter, cons);
        }
    }
    
    public void createEntry(String dn, Attributes attrs, String method) throws NameAlreadyBoundException, ServiceException {
        Context newCtxt = null;
        try {
            Name cpName = new CompositeName().add(dn);
            
            if (ZimbraLog.ldap.isDebugEnabled())
                ZimbraLog.ldap.debug("CREATE ENTRY: method=" + method + ", dn=" + dn + ", attrs=" + attrs.toString());
            newCtxt = mDirContext.createSubcontext(cpName, attrs);
        } catch (NameAlreadyBoundException e) {            
            throw e;
        } catch (NameNotFoundException e){
            throw ServiceException.INVALID_REQUEST(method+" dn not found: "+ LdapUtil.dnToRdnAndBaseDn(dn)[1] +e.getMessage(), e);
        } catch (InvalidAttributeIdentifierException e) {
            throw AccountServiceException.INVALID_ATTR_NAME(method+" invalid attr name: "+e.getMessage(), e);
        } catch (InvalidAttributeValueException e) {
            throw AccountServiceException.INVALID_ATTR_VALUE(method+" invalid attr value: "+e.getMessage(), e);
        } catch (InvalidAttributesException e) {
            throw ServiceException.INVALID_REQUEST(method+" invalid set of attributes: "+e.getMessage(), e);
        } catch (InvalidNameException e) {
            throw ServiceException.INVALID_REQUEST(method+" invalid name: "+e.getMessage(), e);
        } catch (SchemaViolationException e) {
            throw ServiceException.INVALID_REQUEST(method+" invalid schema change: "+e.getMessage(), e); 
        } catch (NamingException e) {
            throw ServiceException.FAILURE(method, e);
        } finally {
            closeContext(newCtxt);
        }
    }
    
    void simpleCreate(String dn, Object objectClass, String[] attrs) throws NamingException {
        Attributes battrs = new BasicAttributes(true);
        if (objectClass instanceof String) {
            battrs.put(Provisioning.A_objectClass, objectClass);
        } else if (objectClass instanceof String[]) {
            String[] oclasses = (String[]) objectClass;
            Attribute a = new BasicAttribute(Provisioning.A_objectClass);
            for (int i=0; i < oclasses.length; i++)
                    a.add(oclasses[i]);
            battrs.put(a);
        }
        for (int i=0; i < attrs.length; i += 2)
            battrs.put(attrs[i], attrs[i+1]);
        Name cpName = new CompositeName().add(dn);
        
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("CREATE ENTRY: dn=" + dn + ", attrs=" + battrs.toString());
        Context newCtxt = mDirContext.createSubcontext(cpName, battrs);
        newCtxt.close();
    }
    
    public void unbindEntry(String dn) throws NamingException {
        Name cpName = new CompositeName().add(dn);
        
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("DELETE ENTRY: dn=" + dn);
        mDirContext.unbind(cpName);
    }
    
    public void moveChildren(String oldDn, String newDn) throws ServiceException {
        NamingEnumeration<SearchResult> ne = null;        
        try {
            // find children under old DN and move them
            SearchControls sc = new SearchControls(SearchControls.ONELEVEL_SCOPE, 0, 0, null, false, false);
            String query = "(objectclass=*)";
            ne = searchDir(oldDn, query, sc);
            NameParser ldapParser = mDirContext.getNameParser("");            
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                String oldChildDn = sr.getNameInNamespace();
                Name oldChildName = ldapParser.parse(oldChildDn);
                Name newChildName = ldapParser.parse(newDn).add(oldChildName.get(oldChildName.size()-1));
                
                if (ZimbraLog.ldap.isDebugEnabled())
                    ZimbraLog.ldap.debug("RENAME ENTRY: old=" + oldChildName + ", new=" + newChildName);
                mDirContext.rename(oldChildName, newChildName);
            }
        } catch (NamingException e) {
            ZimbraLog.account.warn("unable to move children", e);            
        } finally {
            LdapUtil.closeEnumContext(ne);            
        }
    }
    
    public void deleteChildren(String dn) throws ServiceException {
        NamingEnumeration<SearchResult> ne = null;        
        try {
            // find children under old DN and remove them
            SearchControls sc = new SearchControls(SearchControls.ONELEVEL_SCOPE, 0, 0, null, false, false);
            String query = "(objectclass=*)";
            ne = searchDir(dn, query, sc);
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                unbindEntry(sr.getNameInNamespace());
            }
        } catch (NamingException e) {
            ZimbraLog.account.warn("unable to remove children", e);            
        } finally {
            LdapUtil.closeEnumContext(ne);            
        }
    }
    
    public void renameEntry(String oldDn, String newDn) throws NamingException {
        Name oldCpName = new CompositeName().add(oldDn);
        Name newCpName = new CompositeName().add(newDn);
        
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("RENAME ENTRY: old=" + oldCpName + ", new=" + newCpName);
        mDirContext.rename(oldCpName, newCpName);
    }
    
    public void setPagedControl(int pageSize, byte[] cookie, boolean critical) throws NamingException, IOException {
        mDirContext.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, critical?Control.CRITICAL:Control.NONCRITICAL)});
    }
    
    public byte[] getCookie() throws NamingException {
        Control[] controls = mDirContext.getResponseControls();
        if (controls != null) {
            for (int i = 0; i < controls.length; i++) {
                if (controls[i] instanceof PagedResultsResponseControl) {
                    PagedResultsResponseControl prrc =
                        (PagedResultsResponseControl)controls[i];
                    return prrc.getCookie();
                }
            }
        }
        return null;
    }
    
    private static String dumpMods(ModificationItem[] mods) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<mods.length; i++)
            sb.append(mods[i].toString() + ", ");
        return sb.toString();
    }

    public static void waitForServer() {
        while (true) {
            ZimbraLdapContext zlc = null;
            try {
                zlc = new ZimbraLdapContext();
                break;
            } catch (ServiceException e) {
                System.err.println(new Date() + ": error communicating with LDAP (will retry)");
                e.printStackTrace();
                try {
                    Thread.sleep(CHECK_LDAP_SLEEP_MILLIS);
                } catch (InterruptedException ie) {
                }
            } finally {
                ZimbraLdapContext.closeContext(zlc);
            }
        }
    }
    

}
