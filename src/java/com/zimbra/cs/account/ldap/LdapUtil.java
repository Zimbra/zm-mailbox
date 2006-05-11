/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Domain.SearchGalResult;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author schemers
 */
public class LdapUtil {
        
    public final static String LDAP_TRUE  = "TRUE";
    public final static String LDAP_FALSE = "FALSE";

    final static String EARLIEST_SYNC_TOKEN = "19700101000000Z";

    private static int SALT_LEN = 4; // to match LDAP SSHA password encoding
    private static String ENCODING = "{SSHA}";

    private static String sLdapURL;
    private static String sLdapMasterURL;    
    
    private static Hashtable<String, String> sEnvMasterAnon;
    private static Hashtable<String, String> sEnvMasterAuth;
    private static Hashtable<String, String> sEnvAnon;
    private static Hashtable<String, String> sEnvAuth;
    private static String[] sEmptyMulti = new String[0];

    static final SearchControls sSubtreeSC = new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, null, false, false);
    
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
    }

    public static void closeContext(Context ctxt) {
        try {
            if (ctxt != null) {
                //ZimbraLog.account.error("closeDirContext", new RuntimeException("------------------- CLOSE"));
                ctxt.close();
            }
        } catch (NamingException e) {
            // TODO log?
            //e.printStackTrace();
        }
    }

    public static void closeEnumContext(NamingEnumeration ctxt) {
        try {
            if (ctxt != null)
                ctxt.close();
        } catch (NamingException e) {
            // TODO log?
            //e.printStackTrace();
        }
    }

// TODO: need options for get master or replica connections (write vs. read)
    // and maybe admin vs non-admin access 

    /**
     * 
     * @return
     * @throws NamingException
     */
    private static synchronized Hashtable getDefaultEnv(boolean master, boolean anonymous) {
        Hashtable<String, String> sEnv = null;
        
        if  (master && anonymous) {
            if (sEnvMasterAnon != null) return sEnvMasterAnon;
            else sEnv = sEnvMasterAnon = new Hashtable<String, String>();
        } else if (master && !anonymous) {
            if (sEnvMasterAuth != null) return sEnvMasterAuth;
            else sEnv = sEnvMasterAuth = new Hashtable<String, String>(); 
        } else if (!master && anonymous) {
            if (sEnvAnon != null) return sEnvAnon;
            else sEnv = sEnvAnon = new Hashtable<String, String>(); 
        } else if (!master && !anonymous) {
            if (sEnvAuth != null) return sEnvAuth;
            else sEnv = sEnvAuth = new Hashtable<String, String>();             
        }

        sEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        sEnv.put(Context.PROVIDER_URL, master ? sLdapMasterURL : sLdapURL);
        if (!anonymous) {
            sEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            sEnv.put(Context.SECURITY_PRINCIPAL, LC.zimbra_ldap_userdn.value());
            sEnv.put(Context.SECURITY_CREDENTIALS, LC.zimbra_ldap_password.value());
        } else {
            sEnv.put(Context.SECURITY_AUTHENTICATION, "none");
        }
        sEnv.put(Context.REFERRAL, "follow");
            
        // wait at most 10 seconds for a connection
        sEnv.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        // enable connection pooling
        sEnv.put("com.sun.jndi.ldap.connect.pool", "true");
        // env.put("java.naming.ldap.derefAliases", "never");
        //
        // default: env.put("java.naming.ldap.version", "3");
        return sEnv;
    }
    
    /**
     * 
     * @return
     * @throws NamingException
     */
    public static DirContext getDirContext() throws ServiceException {
        return getDirContext(false);
    }
 
    /**
     * 
     * @return
     * @throws NamingException
     */
    public static DirContext getDirContext(boolean master) throws ServiceException {
        return getDirContext(master, false);
    }

    /**
     * 
     * @return
     * @throws NamingException
     */
    public static DirContext getDirContext(boolean master, boolean anonymous) throws ServiceException {
        try {
            long start = ZimbraPerf.STOPWATCH_LDAP_DC.start();
            DirContext dirContext = new InitialLdapContext(getDefaultEnv(master, anonymous), null);
            ZimbraPerf.STOPWATCH_LDAP_DC.stop(start);
            //ZimbraLog.account.error("getDirContext", new RuntimeException("------------------- OPEN"));
            return dirContext;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("getDirectContext", e);
        }
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
    
    /**
     * 
     * @return
     * @throws NamingException
     */
    public static DirContext getDirContext(String urls[], String bindDn, String bindPassword)  throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, joinURLS(urls));
        if (bindDn == null || bindPassword == null) {
            env.put(Context.SECURITY_AUTHENTICATION, "none");
        } else {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, bindPassword);        
        }
        env.put(Context.REFERRAL, "follow");
        // wait at most 10 seconds
        env.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());
        // enable connection pooling
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        return new InitialLdapContext(env, null);
    }

    public static void ldapAuthenticate(String urls[], String principal, String password) throws NamingException {
        if (password == null || password.equals("")) 
            throw new AuthenticationException("empty password");

        Hashtable<String, String> env = new Hashtable<String, String>();
        // wait at most 10 seconds
        env.put("com.sun.jndi.ldap.connect.timeout", LC.ldap_connect_timeout.value());        
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, joinURLS(urls));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);
        DirContext context = null;
        try {
            context = new InitialLdapContext(env, null);
        } catch (NamingException e) {
            throw e;
        } finally {
            closeContext(context);
        }
    }

    public static void ldapAuthenticate(String url[], String password, String searchBase, String searchFilter, String searchDn, String searchPassword) throws NamingException {
        if (password == null || password.equals("")) 
            throw new AuthenticationException("empty password");

        DirContext ctxt = null;
        String resultDn = null;;
        boolean tooMany = false;        
        try {
            ctxt = getDirContext(url, searchDn, searchPassword);
            NamingEnumeration ne = ctxt.search(searchBase, searchFilter, sSubtreeSC);

            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                if (resultDn == null) {
                    resultDn = sr.getNameInNamespace();
                } else {
                    tooMany = true;
                    break;
                }
            }
            ne.close();
        } finally {
            closeContext(ctxt);
        }
        
        if (tooMany) {
            ZimbraLog.account.warn("ldapAuthenticate searchFilter returned more then one result: "+searchFilter);
            throw new AuthenticationException("too many results from search filter!");
        } else if (resultDn == null) {
            throw new AuthenticationException("empty search");
        }
        if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("search filter matched: "+resultDn);
        ldapAuthenticate(url, resultDn, password); 
    }

    public static boolean verifySSHA(String encodedPassword, String password) {
        if (!encodedPassword.startsWith(ENCODING))
            return false;
        byte[] encodedBuff = encodedPassword.substring(ENCODING.length()).getBytes();
        byte[] buff = Base64.decodeBase64(encodedBuff);
        if (buff.length <= SALT_LEN)
            return false;
        byte[] salt = new byte[SALT_LEN];
        System.arraycopy(buff, buff.length-SALT_LEN, salt, 0, SALT_LEN);
        String generated = generateSSHA(password, salt);
        return generated.equals(encodedPassword);
    }
    
    public static String generateSSHA(String password, byte[] salt) {
           try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            if (salt == null) {
                salt = new byte[SALT_LEN];
                SecureRandom sr = new SecureRandom();
                sr.nextBytes(salt);
            } else if (salt.length != SALT_LEN) {
                throw new RuntimeException("invalid salt length, must be 4 bytes: "+salt.length);
            }
            md.update(password.getBytes());
            md.update(salt);
            byte[] digest = md.digest();
            byte[] buff = new byte[digest.length + salt.length];
            System.arraycopy(digest, 0, buff, 0, digest.length);
            System.arraycopy(salt, 0, buff, digest.length, salt.length);
            return ENCODING + new String(Base64.encodeBase64(buff));
        } catch (NoSuchAlgorithmException e) {
            // this shouldn't happen unless JDK is foobar
            throw new RuntimeException(e);
        }
    }
    
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public static String getAttrString(Attributes attrs, String name) throws NamingException {
        Attribute attr = attrs.get(name);
        if (attr != null) {
            Object o = attr.get();
            if (o instanceof String)
                return (String) o;
            else 
                return new String((byte[])o);
        } else {
            return null;
        }
    }

    public static String[] getMultiAttrString(Attributes attrs, String name) throws NamingException {
        Attribute attr = attrs.get(name);
        if (attr != null) {
            String result[] = new String[attr.size()];
            for (int i=0; i < attr.size(); i++) {
                Object o = attr.get(i);
                if (o instanceof String)
                    result[i] = (String) o;
                else 
                    result[i] = new String((byte[])o);
            }
            return result;
        } else {
            return sEmptyMulti;
        }
    }

    /**
     * Enumerates over the specified attributes and populates the specified map. The key in the map is the
     * attribute ID. For attrs with a single value, the value is a String, and for attrs with multiple values
     * the value is an array of Strings.
     * 
     * @param attrs the attributes to enumerate over
     * @param map the map to populate
     * @param prefix returns only attrs that start with prefex
     * @throws NamingException
     */
    public static void getAttrs(Attributes attrs, Map<String, Object> map, String prefix) throws NamingException  {
        for (NamingEnumeration ne = attrs.getAll(); ne.hasMore(); ) {
            Attribute attr = (Attribute) ne.next();
            if (prefix != null && ! attr.getID().startsWith(prefix))
                continue;
            if (attr.size() == 1) {
                Object o = attr.get();
                if (o instanceof String)
                    map.put(attr.getID(), o);
                else 
                    map.put(attr.getID(), new String((byte[])o));
            } else {
                String result[] = new String[attr.size()];
                for (int i=0; i < attr.size(); i++) {
                    Object o = attr.get(i);
                    if (o instanceof String)
                        result[i] = (String) o;
                    else 
                        result[i] = new String((byte[])o);
                }
                map.put(attr.getID(), result);
            }
        }
    }
    
    /**
     * escape *()\ in specified string to make sure user-supplied string doesn't open a security hole.
     * i.e., if the format string is "(sn=*%s*)", and the user types in "a)(zimbraIsAdminAccount=TRUE)(cn=a",
     * we don't want to search for "(sn=*a)(zimbraIsAdminAccount=TRUE)(cn=a*)".
     * 
     * @param s
     * @return
     */
    public static String escapeSearchFilterArg(String s) {
        if (s == null)
            return null;
        else 
            return s.replaceAll("([\\\\\\*\\(\\)])", "\\\\$0");
    }
    
    public static Attribute addAttr(Attributes attrs, String name, String value) {
        BasicAttribute a = new BasicAttribute(name);
        a.add(value);
        attrs.put(a);
        return a;
    }

    public static void simpleCreate(DirContext ctxt, String dn, Object objectClass, String[] attrs) throws NamingException {
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
        Context newCtxt = ctxt.createSubcontext(dn, battrs);
        newCtxt.close();
    }

    /**
     * "modify" the entry. If value is null or "", then remove attribute, otherwise replace/add it.
     */
    private static void modifyAttr(ArrayList<ModificationItem> modList, String name, String value, Attributes currentAttrs) {
        int mod_op = (value == null || value.equals("")) ? DirContext.REMOVE_ATTRIBUTE : DirContext.REPLACE_ATTRIBUTE;
        if (mod_op == DirContext.REMOVE_ATTRIBUTE) {
            // make sure it exists
            if (currentAttrs.get(name) == null)
                return;
        }
        BasicAttribute ba = new BasicAttribute(name);
        if (mod_op == DirContext.REPLACE_ATTRIBUTE)
            ba.add(value);
        modList.add(new ModificationItem(mod_op, ba));
    }

    /**
     * remove the attr with the specified value
     */
    private static void removeAttr(ArrayList<ModificationItem> modList, String name, String value, Attributes currentAttrs) {
        Attribute a = currentAttrs.get(name);
        if (a == null || !a.contains(value)) return;
        
        BasicAttribute ba = new BasicAttribute(name);
        ba.add(value);
        modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));
    }

    /**
     * remove the attr with the specified value
     */
    private static void removeAttr(ArrayList<ModificationItem> modList, String name, String value[], Attributes currentAttrs) {
        Attribute a = currentAttrs.get(name);
        if (a == null) return;

        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (!a.contains(value[i])) continue;
            if (ba == null) ba = new BasicAttribute(name);
            ba.add(value[i]);
        }
        if (ba != null) modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));

    }

    /**
     * add an additional attr with the specified value
     */
    private static void addAttr(ArrayList<ModificationItem> modList, String name, String value, Attributes currentAttrs) {
        Attribute a = currentAttrs.get(name);
        if (a != null && a.contains(value)) return;
        
        BasicAttribute ba = new BasicAttribute(name);
        ba.add(value);
        modList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, ba));
    }

    
    /**
     * add an additional attr with the specified value
     */
    private static void addAttr(ArrayList<ModificationItem> modList, String name, String value[], Attributes currentAttrs) {
        Attribute a = currentAttrs.get(name);

        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (a != null && a.contains(value[i])) continue;
            if (ba == null) ba = new BasicAttribute(name);
            ba.add(value[i]);
        }
        if (ba != null) modList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, ba));
    }
    
    /**
     * Modifies the specified entry.  <code>attrs</code> is a <code>Map</code> consisting of
     * keys that are <code>String</code>s, and values that are either
     * <ul>
     *   <li><code>null</code>, in which case the attr is removed</li>
     *   <li>a single <code>Object</code>, in which case the attr is modified
     *     based on the object's <code>toString()</code> value</li>
     *   <li>an <code>Object</code> array or <code>Collection</code>,
     *     in which case a multi-valued attr is updated</li>
     * </ul>
     */
    public static void modifyAttrs(DirContext ctxt, String dn, Map attrs, Attributes currentAttrs) throws NamingException, ServiceException {
        ArrayList<ModificationItem> modlist = new ArrayList<ModificationItem>();
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry me = (Entry) mit.next();
            Object v= me.getValue();
            String key = (String) me.getKey();
            boolean doAdd = key.charAt(0) == '+';
            boolean doRemove = key.charAt(0) == '-';
            
            if (doAdd || doRemove) {
                // make sure there aren't other changes without +/- going on at the same time 
                key = key.substring(1);
                if (attrs.containsKey(key)) 
                    throw ServiceException.INVALID_REQUEST("can't mix +attrName/-attrName with attrName", null);
            }

            // Convert array to List so it can be treated as a Collection
            if (v instanceof Object[]) {
                // Note: Object[] cast is required, so that asList() knows to create a List
                // that contains the contents of the object array, as opposed to a List with one
                // element, which is the entire Object[].  Ick.
                v = Arrays.asList((Object[]) v);
            }
            
            if (v instanceof Collection) {
                Collection c = (Collection) v;
                if (c.size() == 0) {
                    // make sure it exists
                    if (currentAttrs.get(key) != null) {
                        BasicAttribute ba = new BasicAttribute(key);
                        modlist.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));
                    }
                } else {
                    // Convert values Collection to a String array
                    String[] sa = new String[c.size()];
                    int i = 0;
                    for (Object o : c) {
                        sa[i++] = (o == null ? null : o.toString());
                    }
                    
                    // Add attrs
                    if (doAdd) addAttr(modlist, key, sa, currentAttrs);
                    else if (doRemove) removeAttr(modlist, key, sa, currentAttrs);
                    else {
                        BasicAttribute ba = new BasicAttribute(key);
                        for (i=0; i < sa.length; i++)
                            ba.add(sa[i]);
                        modlist.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, ba));
                    }
                }
            } else if (v instanceof Map) {
                throw ServiceException.FAILURE("Map is not a supported value type", null);
            } else {
                String s = (v == null ? null : v.toString());
                if (doAdd) addAttr(modlist, key, s, currentAttrs);
                else if (doRemove) removeAttr(modlist, key, s, currentAttrs);
                else modifyAttr(modlist, key, s, currentAttrs);
            }
        }
        ModificationItem[] mods = new ModificationItem[modlist.size()];
        modlist.toArray(mods);
        ctxt.modifyAttributes(dn, mods);
    }

    /**
     * Adds the specified attr to the given dn.
     * 
     * @param ctxt
     * @param dn
     * @param name
     * @param value
     * @throws NamingException
     */
    public static void addAttr(DirContext ctxt, String dn, String name, String value) throws NamingException {
        BasicAttribute ba = new BasicAttribute(name);
        ba.add(value);
        ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, ba); 
        ModificationItem[] mods = new ModificationItem[] { item };
        ctxt.modifyAttributes(dn, mods);
    }

    /**
     * take a map (key = String, value = String | String[]) and populate Attributes.
     * 
     * @param map
     * @param attrs
     */
    public static void mapToAttrs(Map mapAttrs, Attributes attrs) {
        for (Iterator mit=mapAttrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry me = (Entry) mit.next();
            Object v = me.getValue();
            if (v instanceof String)
                attrs.put((String)me.getKey(), (String)v);
            else if (v instanceof String[]) {
                String[] sa = (String[]) v;
                BasicAttribute a = new BasicAttribute((String)me.getKey());
                for (int i=0; i < sa.length; i++)
                        a.add(sa[i]);
                attrs.put(a);
            }
        }
    }

    /**
     * removes the specified attributed.
     *	
     * @param ctxt
     * @param dn
     * @param name
     * @param value
     * @throws NamingException
     */
    public static void removeAttr(DirContext ctxt, String dn, String name, String value) throws NamingException {
        BasicAttribute ba = new BasicAttribute(name);
        if (value != null)
            ba.add(value);
        ModificationItem item = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba); 
        ModificationItem[] mods = new ModificationItem[] { item };
        ctxt.modifyAttributes(dn, mods);
    }

    private static String domainToDN(String parts[], int offset) {
        StringBuffer sb = new StringBuffer(128);
        for (int i=offset; i < parts.length; i++) {
            if (i-offset > 0) sb.append(",");
            sb.append("dc=").append(parts[i]);
        }
        return sb.toString();
    }

    /**
     * Given a domain like foo.com, return the dn: dc=foo,dc=com
     * @param domain
     * @return the dn
     */
    public static String domainToDN(String domain) {
        return domainToDN(domain.split("\\."), 0);
    }

    /**
     * given a dn like "uid=foo,ou=people,dc=widgets,dc=com", return the string "widgets.com".
     * 
     * @param dn
     * @return
     */
    public static String dnToDomain(String dn) {
        String[] parts = dn.split(",");
        StringBuffer sb = new StringBuffer();

        	for (int i=0; i < parts.length; i++) {
        	    if (parts[i].startsWith("dc=")) {
        	        if (sb.length() > 0)
        	            sb.append(".");
        	        sb.append(parts[i].substring(3));
        	    }
        	}
        return sb.toString();
    }

    /**
     * Given a dn like "uid=foo,ou=people,dc=widgets,dc=com", return the string "foo@widgets.com".
     * 
     * @param dn
     * @return
     */
    public static String dnToEmail(String dn) {
        String [] parts = dn.split(",");
        StringBuffer domain = new StringBuffer(dn.length());
        String uid = null;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("dc=")) {
                if (domain.length() > 0)
                    domain.append(".");
                domain.append(parts[i].substring(3));
            } else if (parts[i].startsWith("uid=")) {
                uid = parts[i].substring(4);
            }
        }
        if (uid == null)
            return null; // TODO should this be an exception
        if (domain.length() == 0)
            return uid;
        return new StringBuffer(uid).append('@').append(domain).toString();
    }
    
    /**
     * Given a dn like "uid=zimbra,cn=admins,cn=zimbra", return the string "zimbra".
     * @param dn
     * @return
     */
    public static String dnToUid(String dn) {
        String [] parts = dn.split(",");
        String uid = null;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("uid=")) {
                uid = parts[i].substring(4);
                break;
            }
        }
        return uid;
    }
    
    /**
     * Given a domain like foo.com, return an array of dns that work their way up the tree:
     *    [0] = dc=foo,dc=com
     *    [1] = dc=com
     * 
     * @param domain
     * @return the array of DNs
     */
    public static String[] domainToDNs(String[] parts) {
        String dns[] = new String[parts.length];
        for (int i=parts.length-1; i >= 0; i--) {
            dns[i] = domainToDN(parts, i);
        }
        return dns;
    }
    
    public static void main(String args[]) throws NamingException {
        ldapAuthenticate(new String[] { "ldap://exch1/"}, "schemers@liquidsys.com", "");
/*
        Date now = new Date();
        String gts = generalizedTime(now);
        System.out.println(now);
        System.out.println(gts);
        Date pnow = generalizedTime(gts);
        System.out.println(pnow);        
        */
    }

    static String[] removeMultiValue(String values[], String value) {
        ArrayList list = new ArrayList(Arrays.asList(values));
        boolean updated = list.remove(value);
        if (updated) {
            return (String[]) list.toArray(new String[list.size()]);
        } else {
            return values;
        }
    }
    
    public static String getBooleanString(boolean b) {
        if (b) {
            return LDAP_TRUE;
        }
        return LDAP_FALSE;
    }

    /*
      * expansions for bind dn string:
      * 
      * %n = username with @ (or without, if no @ was specified)
      * %u = username with @ removed
      * %d = domain as foo.com
      * %D = domain as dc=foo,dc=com
      * 
      * exchange example, where the exchange domian is different than the zimbra one
      * 
      * zimbraAuthMech      ldap
      * zimbraAuthLdapURL   ldap://exch1/
      * zimbraAuthLdapDn    %n@example.zimbra.com
      * 
      * our own LDAP example:
      * 
      * zimbraAuthMech       ldap
      * zimbraAuthLdapURL    ldap://server.example.zimbra.com/
      * zimbraAuthLdapUserDn uid=%u,ou=people,%D
      */
      public static String computeAuthDn(String name, String bindDnRule) {
         if (bindDnRule == null || bindDnRule.equals("") || bindDnRule.equals("%n"))
             return name;
    
         int at = name.indexOf("@");
    
         Map<String, String> vars = new HashMap<String, String>();
         vars.put("n", name);         
    
         if (at  == -1) {
             vars.put("u", name);
         } else {
             vars.put("u", name.substring(0, at));
             String d = name.substring(at+1);
             vars.put("d", d);
             vars.put("D", domainToDN(d));
         }
         
         return LdapProvisioning.expandStr(bindDnRule, vars);
      }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchGal(java.lang.String)
     */
    public static SearchGalResult searchLdapGal(
            String url[],
            String bindDn,
            String bindPassword,
            String base,
            String filter, 
            String n,
            int maxResults,
            String[] galAttrList,
            Map galAttrMap, String token) throws NamingException, ServiceException {
    
        SearchGalResult result = new SearchGalResult();
        result.matches = new ArrayList<GalContact>();
    
        if (url == null || url.length == 0 || base == null || filter == null) {
            if (url == null || url.length == 0)
                ZimbraLog.misc.warn("searchLdapGal url is null");
            if (base == null)
                ZimbraLog.misc.warn("searchLdapGal base is null");
            if (filter == null)
                ZimbraLog.misc.warn("searchLdapGal queryExpr is null");
            return result;
        }
    
        if (filter.indexOf("(") == -1) {
            String queryExpr = LdapDomain.getFilterDef(filter);
            if (queryExpr != null)
                filter = queryExpr;
        }
                

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("s", n);
        String query = LdapProvisioning.expandStr(filter, vars);
        if (token != null) {
            if (token.equals(""))
                query = query.replaceAll("\\*\\*", "*");
            else  {
                String arg = LdapUtil.escapeSearchFilterArg(token);                
                query = "(&(|(modifyTimeStamp>="+arg+")(createTimeStamp>="+arg+")(whenModified>="+arg+")(whenCreated>="+arg+"))"+query.replaceAll("\\*\\*", "*")+")";                
            }                
        }
        ZimbraLog.misc.debug("searchLdapGal query:"+query);
        SearchControls sc = new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, galAttrList, false, false);
        result.token = token != null ? token : EARLIEST_SYNC_TOKEN;
        DirContext ctxt = null;
        try {
            ctxt = getDirContext(url, bindDn, bindPassword);
            NamingEnumeration ne = ctxt.search(base, query, sc);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                String dn = sr.getNameInNamespace();
                LdapGalContact lgc = new LdapGalContact(dn, sr.getAttributes(), galAttrList, galAttrMap); 
                String mts = (String) lgc.getAttrs().get("modifyTimeStamp");
                result.token = getLaterTimestamp(result.token, mts);
                result.matches.add(lgc);
            }
            ne.close();
        } catch (SizeLimitExceededException sle) {
            result.hadMore = true;
        } finally {
            closeContext(ctxt);
        }
        return result;
    }

    /**
     * 
     * @param ldapAttrMap value of zimbraGalLdapAttrMap
     * @param attrsList list of ldap attributes to populate
     * @param attrMap map of ldap attr to address book field to populate
     */
    public static void initGalAttrs(String[] ldapAttrMap, List<String> attrsList, Map<String, String> attrMap) {
        for (int i = 0; i < ldapAttrMap.length; i++) {
            String val = ldapAttrMap[i];
            int p = val.indexOf('=');
            if (p != -1) {
                String ldapAttr = val.substring(0, p);
                String abookAttr = val.substring(p+1);
                if (ldapAttr.indexOf(',') != -1) {
                    String[] lattrs = ldapAttr.split(",");
                    for (int j=0; j < lattrs.length; j++) {
                        attrsList.add(lattrs[j]);
                        attrMap.put(lattrs[j], abookAttr);
                    }
                } else {
                    attrsList.add(ldapAttr);
                    attrMap.put(ldapAttr, abookAttr);
                }
            }
        }
    }

    /**
     * Return the later (more recent) of two LDAP timestamps.  Timestamp
     * format is YYYYMMDDhhmmssZ. (e.g. 20060315023000Z)
     * @param timeA
     * @param timeB
     * @return later of the two timestamps; a non-null timestamp is considered
     *         later than a null timestamp; null is returned if both timestamps
     *         are null
     */
    public static String getLaterTimestamp(String timeA, String timeB) {
        if (timeA == null) {
            return timeB;
        } else if (timeB == null) {
            return timeA;
        }
        return timeA.compareTo(timeB) > 0 ? timeA : timeB;
    }
}
