package com.zimbra.cs.account.ldap;

import java.io.IOException;
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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * 
 * @author pshao
 *
 */
public class ZimbraLdapContext {

    private static String sLdapURL;
    private static String sLdapMasterURL;    
    
    private static Hashtable<String, String> sEnvMasterAuth;
    private static Hashtable<String, String> sEnvAuth;
    
    private LdapContext mDirContext;
    private StartTlsResponse mTlsResp;

    static {
        String ldapHost = LC.ldap_host.value();
        String ldapPort = LC.ldap_port.value();
        
        sLdapURL = LC.ldap_url.value().trim();
        if (sLdapURL.length() == 0) {
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
        
        // TODO: should we use mailboxd_keystore or mailboxd_truststore? 
        if (requireStartTLS())
            System.setProperty("javax.net.ssl.trustStore", LC.mailboxd_keystore.value());

    }
    
    
    public static String getLdapURL() {
        return sLdapURL;
    }
    
    /**
     * TODO:  also need to check
     * 1. ldap protocol - cannot be ldaps
     * 2. ldap port - cannot be 636
     * 3. ldap_starttls_supported
     * 4. 
     * 
     * @return
     */
    private static boolean requireStartTLS() {
        return LC.ldap_require_tls.booleanValue();
    }
    
    private static synchronized Hashtable getDefaultEnv(boolean master) {
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
        
        // connection pooling
        if (master)
            sEnv.put("com.sun.jndi.ldap.connect.pool", LC.ldap_connect_pool_master.value());
        else
            sEnv.put("com.sun.jndi.ldap.connect.pool", "true");

        // env.put("java.naming.ldap.derefAliases", "never");
        // default: env.put("java.naming.ldap.version", "3");
        
        if (!requireStartTLS()) {
            sEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            sEnv.put(Context.SECURITY_PRINCIPAL, LC.zimbra_ldap_userdn.value());
            sEnv.put(Context.SECURITY_CREDENTIALS, LC.zimbra_ldap_password.value());
        }
        
        return sEnv;
    }
    
    /*
     * TODO: handle startTLS
     */
    private static synchronized Hashtable getNonPooledEnv(boolean master) {
        Hashtable<String, String> env = new Hashtable<String, String>(); 
        
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, master ? sLdapMasterURL : sLdapURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, LC.zimbra_ldap_userdn.value());
        env.put(Context.SECURITY_CREDENTIALS, LC.zimbra_ldap_password.value());
        env.put(Context.REFERRAL, "follow");
            
        env.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        env.put("com.sun.jndi.ldap.read.timeout", LC.ldap_read_timeout.value());
        
        // enable connection pooling
        if (master)
            env.put("com.sun.jndi.ldap.connect.pool", LC.ldap_connect_pool_master.value());
        else 
            env.put("com.sun.jndi.ldap.connect.pool", "false");
        
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
    
    /*
     * Zimbra LDAP
     */
    public ZimbraLdapContext() throws ServiceException {
        this(false);
    }

    /*
     * Zimbra LDAP
     */
    public ZimbraLdapContext(boolean master) throws ServiceException {
        this(master, true);
    }
    
    /*
     * Zimbra LDAP
     */
    public ZimbraLdapContext(boolean master, boolean useConnPool) throws ServiceException {
        try {
            Hashtable env = useConnPool? getDefaultEnv(master) : getNonPooledEnv(master);
            boolean startTLS = requireStartTLS();
            
            
            long start = ZimbraPerf.STOPWATCH_LDAP_DC.start();
            
            if (ZimbraLog.ldap.isDebugEnabled())
                ZimbraLog.ldap.debug("GET DIR CTXT: " + "url=" + env.get(Context.PROVIDER_URL) + ", binddn="+ env.get(Context.SECURITY_PRINCIPAL) + ", startTLS=" + startTLS);
            mDirContext = new InitialLdapContext(env, null);
                        
            if (startTLS) {
                // start TLS
                mTlsResp = (StartTlsResponse) mDirContext.extendedOperation(new StartTlsRequest());
                
                ZimbraLog.ldap.debug("START TLS");
                mTlsResp.negotiate();

                mDirContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                mDirContext.addToEnvironment(Context.SECURITY_PRINCIPAL, LC.zimbra_ldap_userdn.value());
                mDirContext.addToEnvironment(Context.SECURITY_CREDENTIALS, LC.zimbra_ldap_password.value());
            }
            
            ZimbraPerf.STOPWATCH_LDAP_DC.stop(start);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("getDirectContext", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("getDirectContext", e);
        }
    }
    
    /*
     * External LDAP
     */
    public ZimbraLdapContext(String urls[], String bindDn, String bindPassword)  throws NamingException {
        this(urls, null, bindDn, bindPassword);
    }
    
    /*
     * External LDAP
     */
    public ZimbraLdapContext(String urls[], LdapGalCredential credential)  throws NamingException {
        this(urls, credential.getAuthMech(), credential.getBindDn(), credential.getBindPassword());
    }
    
    /*
     * External LDAP
     */
    public ZimbraLdapContext(String urls[], String authMech, String bindDn, String bindPassword)  throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, joinURLS(urls));
        
        if (authMech == null) {
            if (bindDn != null && bindPassword != null)
                authMech = Provisioning.LDAP_AM_SIMPLE;
            else
                authMech = Provisioning.LDAP_AM_NONE;
        }
        
        if (authMech.equals(Provisioning.LDAP_AM_NONE)) {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        } else if (authMech.equals(Provisioning.LDAP_AM_SIMPLE)) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, bindPassword);        
        } else if (authMech.equals(Provisioning.LDAP_AM_KERBEROS5)) {
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            env.put("javax.security.sasl.qop", "auth-conf");
        }
        
        env.put(Context.REFERRAL, "follow");
        env.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        env.put("com.sun.jndi.ldap.read.timeout", LC.ldap_read_timeout.value());
        
        String derefAliases = LC.ldap_deref_aliases.value();
        if (!StringUtil.isNullOrEmpty(derefAliases))
            env.put("java.naming.ldap.derefAliases", LC.ldap_deref_aliases.value());
        
        // enable connection pooling
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        
        // TODO: add ldap attr for external LDAP start TLS and pass the param in
        boolean startTLS = false;
        if (ZimbraLog.ldap.isDebugEnabled())
            ZimbraLog.ldap.debug("GET DIR CTXT(external): " + "url=" + env.get(Context.PROVIDER_URL) + ", binddn="+ env.get(Context.SECURITY_PRINCIPAL) + ", authMech=" + env.get(Context.SECURITY_AUTHENTICATION) + ", startTLS=" + startTLS);

        mDirContext = new InitialLdapContext(env, null);
    }
    
    /**
     * authenticate to external LDAP
     *  
     * @param urls
     * @param principal
     * @param password
     * @throws NamingException
     */
    static void ldapAuthenticate(String urls[], String principal, String password) throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        env.put("com.sun.jndi.ldap.read.timeout", LC.ldap_read_timeout.value());
        
        String derefAliases = LC.ldap_deref_aliases.value();
        if (!StringUtil.isNullOrEmpty(derefAliases))
            env.put("java.naming.ldap.derefAliases", LC.ldap_deref_aliases.value());
        
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ZimbraLdapContext.joinURLS(urls));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);
        
        // TODO: add ldap attr for external LDAP start TLS and pass the param in
        boolean startTLS = false;
        
        DirContext context = null;
        try {
            if (ZimbraLog.ldap.isDebugEnabled())
                ZimbraLog.ldap.debug("GET DIR CTXT(external): " + "url=" + env.get(Context.PROVIDER_URL) + ", binddn="+ env.get(Context.SECURITY_PRINCIPAL) + ", authMech=" + env.get(Context.SECURITY_AUTHENTICATION) + ", startTLS=" + startTLS);

            context = new InitialLdapContext(env, null);
        } finally {
            closeContext(context);
        }
    }
    
    /*
     * TODO: retire after cleanup LdapUtil
     */
    public LdapContext getLdapContext() {
        return mDirContext;
    }
    
    private static void closeContext(Context ctxt) {
        try {
            if (ctxt != null) {
                ZimbraLog.ldap.debug("CLOSE DIR CTXT");
                ctxt.close();
            }
        } catch (NamingException e) {
            // TODO log?
            //e.printStackTrace();
        }
    }
    
    public static void closeContext(ZimbraLdapContext zlc) {
        if (zlc != null)
            zlc.closeContext();
    }
    
    private void closeContext() {
        
        try {
            // stop TLS
            if (mTlsResp != null) {
                ZimbraLog.ldap.debug("STOP TLS");
                mTlsResp.close();
            }
        } catch (IOException e) {
            ZimbraLog.account.error("failed to close tls", e);
        }
        
        try {
            // close the dir context
            if (mDirContext != null) {
                ZimbraLog.ldap.debug("CLOSE DIR CTXT");
                mDirContext.close();
            }
            
        } catch (NamingException e) {
            ZimbraLog.account.error("failed to close dir context", e);
        }
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
        NamingEnumeration ne = null;        
        try {
            // find children under old DN and move them
            SearchControls sc = new SearchControls(SearchControls.ONELEVEL_SCOPE, 0, 0, null, false, false);
            String query = "(objectclass=*)";
            ne = searchDir(oldDn, query, sc);
            NameParser ldapParser = mDirContext.getNameParser("");            
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
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
        NamingEnumeration ne = null;        
        try {
            // find children under old DN and remove them
            SearchControls sc = new SearchControls(SearchControls.ONELEVEL_SCOPE, 0, 0, null, false, false);
            String query = "(objectclass=*)";
            ne = searchDir(dn, query, sc);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
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

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        
    }

}
