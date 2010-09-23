/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.krb5.Krb5Login;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.gal.GalSearchConfig;
import com.zimbra.cs.gal.GalSearchParams;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Rdn;
import javax.security.auth.login.LoginException;

import java.io.IOException;

import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

/**
 * @author schemers
 */
public class LdapUtil {
        
    public final static String LDAP_TRUE  = "TRUE";
    public final static String LDAP_FALSE = "FALSE";

    final static String EARLIEST_SYNC_TOKEN = "19700101000000Z";

    private static String[] sEmptyMulti = new String[0];

    static final SearchControls sSubtreeSC = new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, null, false, false);

    public static void closeEnumContext(NamingEnumeration ctxt) {
        try {
            if (ctxt != null)
                ctxt.close();
        } catch (NamingException e) {
            // TODO log?
            //e.printStackTrace();
        }
    }
    
    public static void ldapAuthenticate(String urls[], boolean requireStartTLS, String principal, String password) throws NamingException, IOException {
        if (password == null || password.equals("")) 
            throw new AuthenticationException("empty password");
        
        ZimbraLdapContext.ldapAuthenticate(urls, requireStartTLS, principal, password, "external LDAP auth");
    }

    public static void ldapAuthenticate(String url[], boolean requireStartTLS, String password, String searchBase, String searchFilter, String searchDn, String searchPassword) 
        throws NamingException, IOException {
        
        if (password == null || password.equals("")) 
            throw new AuthenticationException("empty password");

        ZimbraLdapContext zlc = null;
        String resultDn = null;;
        String tooMany = null;
        NamingEnumeration ne = null;
        try {
            zlc = new ZimbraLdapContext(url, requireStartTLS, searchDn, searchPassword, "external LDAP auth");
            ne = zlc.searchDir(searchBase, searchFilter, sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                if (resultDn == null) {
                    resultDn = sr.getNameInNamespace();
                } else {
                    tooMany = sr.getNameInNamespace();
                    break;
                }
            }
        } finally {
            ZimbraLdapContext.closeContext(zlc);
            closeEnumContext(ne);
        }
        
        if (tooMany != null) {
            ZimbraLog.account.warn(String.format("ldapAuthenticate searchFilter returned more then one result: (dn1=%s, dn2=%s, filter=%s)", resultDn, tooMany, searchFilter));
            throw new AuthenticationException("too many results from search filter!");
        } else if (resultDn == null) {
            throw new AuthenticationException("empty search");
        }
        if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("search filter matched: "+resultDn);
        ldapAuthenticate(url, requireStartTLS, resultDn, password); 
    }
    
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /*
     * we want to throw the IllegalArgumentException instead of catching it so the cause
     * can be logged with the callers catcher.
     */
    public static boolean isValidUUID(String strRep) throws IllegalArgumentException {
        /*
        if (strRep.length() > 36)
            throw new IllegalArgumentException("uuid must be no longer than 36 characters");
        
        UUID uuid = UUID.fromString(strRep);
        return (uuid != null);   
        */
        
        if (strRep.length() > Provisioning.MAX_ZIMBRA_ID_LEN)
            throw new IllegalArgumentException("uuid must be no longer than " + Provisioning.MAX_ZIMBRA_ID_LEN + " characters");
        
        if (strRep.contains(":"))
            throw new IllegalArgumentException("uuid must not contain ':'");
        
        return true;
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
     * @throws NamingException
     */
    public static Map<String, Object> getAttrs(Attributes attrs) throws NamingException  {
        Map<String,Object> map = new HashMap<String,Object>();        
        for (NamingEnumeration ne = attrs.getAll(); ne.hasMore(); ) {
            Attribute attr = (Attribute) ne.next();
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
        return map;
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
    
    public static Attribute addAttr(Attributes attrs, String name, Set<String> values) {
        Attribute a = attrs.get(name);
        if (a == null) {
            a = new BasicAttribute(name);
            attrs.put(a);
        }
        
        for (String value : values)
            a.add(value);
        
        return a;
    }

    /**
     * "modify" the entry. If value is null or "", then remove attribute, otherwise replace/add it.
     */
    private static void modifyAttr(ArrayList<ModificationItem> modList, String name, String value, com.zimbra.cs.account.Entry entry) {
        int mod_op = (value == null || value.equals("")) ? DirContext.REMOVE_ATTRIBUTE : DirContext.REPLACE_ATTRIBUTE;
        if (mod_op == DirContext.REMOVE_ATTRIBUTE) {
            // make sure it exists
            if (entry.getAttr(name, false) == null)
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
    private static void removeAttr(ArrayList<ModificationItem> modList, String name, String value, com.zimbra.cs.account.Entry entry) {
        if (!contains(entry.getMultiAttr(name, false), value)) return;
        
        BasicAttribute ba = new BasicAttribute(name);
        ba.add(value);
        modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));
    }

    private static boolean contains(String[] values, String val) {
        if (values == null) return false;
        for (String s : values) {
            if (s.compareToIgnoreCase(val) == 0) return true;
        }
        return false;
    }
    
    /**
     * remove the attr with the specified value
     */
    private static void removeAttr(ArrayList<ModificationItem> modList, String name, String value[], com.zimbra.cs.account.Entry entry) {
        String[] currentValues = entry.getMultiAttr(name, false);
        if (currentValues == null || currentValues.length == 0) return;

        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (!contains(currentValues, value[i])) continue;
            if (ba == null) ba = new BasicAttribute(name);
            ba.add(value[i]);
        }
        if (ba != null) modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));

    }

    /**
     * add an additional attr with the specified value
     */
    private static void addAttr(ArrayList<ModificationItem> modList, String name, String value, com.zimbra.cs.account.Entry entry) {
        if (contains(entry.getMultiAttr(name, false), value)) return;        
        
        BasicAttribute ba = new BasicAttribute(name);
        ba.add(value);
        modList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, ba));
    }

    
    /**
     * add an additional attr with the specified value
     */
    private static void addAttr(ArrayList<ModificationItem> modList, String name, String value[], com.zimbra.cs.account.Entry entry) {
        String[] currentValues = entry.getMultiAttr(name, false);
        
        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (contains(currentValues, value[i])) continue;
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
    public static void modifyAttrs(ZimbraLdapContext zlc, String dn, Map attrs, com.zimbra.cs.account.Entry entry) throws NamingException, ServiceException {
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
                    if (entry.getAttr(key, false) != null) {
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
                    if (doAdd) addAttr(modlist, key, sa, entry);
                    else if (doRemove) removeAttr(modlist, key, sa, entry);
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
                if (doAdd) addAttr(modlist, key, s, entry);
                else if (doRemove) removeAttr(modlist, key, s, entry);
                else modifyAttr(modlist, key, s, entry);
            }
        }
        ModificationItem[] mods = new ModificationItem[modlist.size()];
        modlist.toArray(mods);
        zlc.modifyAttributes(dn, mods);
    }

    /**
     * take a map (key = String, value = String | String[]) and populate Attributes.
     * 
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
            } else if (v instanceof Collection) {
                Collection c = (Collection) v;
                BasicAttribute a = new BasicAttribute((String)me.getKey());
                for (Object o : c) {
                	a.add(o.toString());
                }
                attrs.put(a);
            }
                
        }
    }

    public static String domainToDN(String parts[], int offset) {
        StringBuffer sb = new StringBuffer(128);
        for (int i=offset; i < parts.length; i++) {
            if (i-offset > 0) sb.append(",");
            sb.append("dc=").append(escapeRDNValue(parts[i]));
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
     * Given an email like blah@foo.com, return the domain dn: dc=foo,dc=com
     * @return the dn
     * @throws ServiceException 
     */
    public static String emailToDomainDN(String email) throws ServiceException {
        int index = email.indexOf('@');
        if (index == -1) 
            throw ServiceException.INVALID_REQUEST("must be an email address: "+email, null);
        String domain = email.substring(index+1);
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
        	        sb.append(unescapeRDNValue(parts[i].substring(3)));
        	    }
        	}
        return sb.toString();
    }
    
    /**
     * given a dn like "uid=foo,ou=people,dc=widgets,dc=com", return the String[]
     * [0] = uid=foo
     * [1] = ou=people,dc=widgets,dc=com
     * 
     * if the dn cannot be split into rdn and dn:
     * [0] = the input dn
     * [1] = the input dn
     * 
     * @param dn
     * @return
     */
    public static String[] dnToRdnAndBaseDn(String dn) {
        String[] values = new String[2];
        int baseDnIdx = dn.indexOf(",");
        
        if (baseDnIdx!=-1 && dn.length()>baseDnIdx+1) {
            values[0] = dn.substring(0, baseDnIdx);
            values[1] = dn.substring(baseDnIdx+1);
        } else {
            values[0] = dn;
            values[1] = dn;
        }
        
        return values;
    }


    static String[] removeMultiValue(String values[], String value) {
        List<String> list = new ArrayList<String>(Arrays.asList(values));
        boolean updated = list.remove(value);
        if (updated) {
            return list.toArray(new String[list.size()]);
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
      
      public static interface SearchLdapVisitor {
          public void visit(String dn, Map<String, Object> attrs, Attributes ldapAttrs);
      }
      
      public static void searchLdapOnMaster(String base, String query, String[] returnAttrs, SearchLdapVisitor visitor) throws ServiceException {
          searchLdap(base, query, returnAttrs, true, visitor);
      }

      public static void searchLdapOnReplica(String base, String query, String[] returnAttrs, SearchLdapVisitor visitor) throws ServiceException {
          searchLdap(base, query, returnAttrs, false, visitor);
      }
              
      private static void searchLdap(String base, String query, String[] returnAttrs, boolean useMaster, SearchLdapVisitor visitor) throws ServiceException {
          
          int maxResults = 0; // no limit
          ZimbraLdapContext zlc = null; 
          int numModified = 0;
          
          try {
              zlc = new ZimbraLdapContext(useMaster);
              
              SearchControls searchControls =
                  new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, returnAttrs, false, false);

              //Set the page size and initialize the cookie that we pass back in subsequent pages
              int pageSize = LdapUtil.adjustPageSize(maxResults, 1000);
              byte[] cookie = null;

              NamingEnumeration ne = null;
              
              try {
                  do {
                      zlc.setPagedControl(pageSize, cookie, true);

                      ne = zlc.searchDir(base, query, searchControls);
                      while (ne != null && ne.hasMore()) {
                          SearchResult sr = (SearchResult) ne.nextElement();
                          String dn = sr.getNameInNamespace();
                          Attributes attrs = sr.getAttributes();
                          visitor.visit(dn, LdapUtil.getAttrs(attrs), attrs);
                      }
                      cookie = zlc.getCookie();
                  } while (cookie != null);
              } finally {
                  if (ne != null) ne.close();
              }
          } catch (NamingException e) {
              throw ServiceException.FAILURE("unable to search ldap", e);
          } catch (IOException e) {
              throw ServiceException.FAILURE("unable to search ldap", e);
          } finally {
              ZimbraLdapContext.closeContext(zlc);
          }
      }

      public static void searchGal(ZimbraLdapContext zlc,
                                   int pageSize,
                                   String base, 
                                   String query, 
                                   int maxResults,
                                   LdapGalMapRules rules,
                                   String token,
                                   SearchGalResult result) throws ServiceException {

        String tk = token != null && !token.equals("")? token : EARLIEST_SYNC_TOKEN;
        result.setToken(tk);
        
        if (pageSize > 0)
            pageSize = adjustPageSize(maxResults, pageSize);
        
        if (ZimbraLog.gal.isDebugEnabled()) {
            StringBuffer returnAttrs = new StringBuffer();
            String attrs[] = rules.getLdapAttrs();
            for (String a: attrs) {
                returnAttrs.append(a + ",");
            }
            
            String url = null;
            String binddn = null;
            try {
                Hashtable ctxtEnv = zlc.getLdapContext().getEnvironment();
                Object urlObj = ctxtEnv.get(zlc.getLdapContext().PROVIDER_URL);
                if (urlObj != null)
                    url = urlObj.toString();
                Object binddnObj = ctxtEnv.get(zlc.getLdapContext().SECURITY_PRINCIPAL);
                if (binddnObj != null)
                    binddn = binddnObj.toString();
            } catch (NamingException e) {
                ZimbraLog.gal.debug("cannot get DirContext environment for debug");
            }
            
            ZimbraLog.gal.debug("searchGal: " +
                                "url=" + url +
                                ", binddn=" + binddn + 
                                ", page size=" + pageSize + 
                                ", max results=" + maxResults + 
                                ", base=" + base + 
                                ", query=" + query +
                                ", attrs=" + returnAttrs);
        }
        
        SearchControls sc = new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, rules.getLdapAttrs(), false, false);
        NamingEnumeration ne = null;
        int total = 0;
        byte[] cookie = null;
        
        /*
         * quick way to limit the size of gal sync for testing, otherwise gal sync would retrieve all entries.
         */
        // maxResults = Integer.valueOf(LC.get("debug_max_gal_entries"));
        
        try {
            try {
                do {
                    if (pageSize > 0)
                        zlc.setPagedControl(pageSize, cookie, false);
                    
                    ne = zlc.searchDir(base, query, sc);
                    while (ne != null && ne.hasMore()) {
                        if (maxResults > 0 && total++ > maxResults) {
                            result.setHadMore(true);
                            break;
                        }
                        
                        SearchResult sr = (SearchResult) ne.next();
                        String dn = sr.getNameInNamespace();
                        
                        GalContact lgc = new GalContact(dn, rules.apply(zlc, sr));
                        String mts = (String) lgc.getAttrs().get("modifyTimeStamp");
                        result.setToken(getLaterTimestamp(result.getToken(), mts));
                        String cts = (String) lgc.getAttrs().get("createTimeStamp");
                        result.setToken(getLaterTimestamp(result.getToken(), cts));
                        result.addMatch(lgc);
                        ZimbraLog.gal.debug("dn=" + dn + ", mts=" + mts + ", cts=" + cts);
                    
                    }
                    if (pageSize > 0)
                        cookie = zlc.getCookie();
                } while (cookie != null);
            /*    
            } catch (ReferralException e) {
                ZimbraLog.gal.debug("caught ReferralException: info=" + e.getReferralInfo().toString() + ", msg=" + e.getMessage());
                
                // http://java.sun.com/products/jndi/tutorial/ldap/referral/jndi.html
                // ignore ReferralException if ldap_referral is set to "throw"
                if (!LC.ldap_referral.value().equals("throw")) 
                    throw e;
                
            } catch (PartialResultException e) {
                ZimbraLog.gal.debug("caught PartialResultException: " + e.getMessage());
                
                // http://java.sun.com/products/jndi/tutorial/ldap/referral/jndi.html
                // ignore PartialResultException if ldap_referral is set to "ignore"
                if (!LC.ldap_referral.value().equals("ignore"))
                    throw e;
            */    
            } finally {
                if (ne != null) 
                    ne.close();
            }    
        } catch (SizeLimitExceededException sle) {
            result.setHadMore(true);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to search gal", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to search gal", e);     
        } finally {
            // do it in the caller where the context was obtained
            // closeContext(ctxt);
        	
        	// moved from LdapProvisioning so it can be done for all ldap searches.
            /*
             *  LDAP doesn't have a > query, just a >= query.  
             *  This causes SyncGal returns extra entries that were updated/created on the same second 
             *  as the prev sync token.  To work around it, we add one second to the result token if the 
             *  token has changed in this sync.        
             */
            boolean gotNewToken = true;
            String newToken = result.getToken();
            if (newToken == null || (token != null && token.equals(newToken)) || newToken.equals(LdapUtil.EARLIEST_SYNC_TOKEN))
                gotNewToken = false;
            
            if (gotNewToken) {
                Date parsedToken = DateUtil.parseGeneralizedTime(newToken, false);
                if (parsedToken != null) {
                    long ts = parsedToken.getTime();
                    ts += 1000;
                    
                    // Note, this will "normalize" the token to our standard format
                    // DateUtil.ZIMBRA_LDAP_GENERALIZED_TIME_FORMAT
                    // Whenever we've got a new token, it will be returned in the
                    // normalized format.
                    result.setToken(DateUtil.toGeneralizedTime(new Date(ts)));
                }
                /*
                 * in the rare case when an LDAP implementation does not conform to generalized time and 
                 * we cannot parser the token, just leave it alone.
                 */
            }
        }
    }
  
      
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchGal(java.lang.String)
     */
    public static SearchGalResult searchLdapGal(
            GalParams.ExternalGalParams galParams, 
            GalOp galOp,
            String n,
            int maxResults,
            LdapGalMapRules rules,
            String token,
            GalContact.Visitor visitor) throws ServiceException, NamingException, IOException {
        
        String url[] = galParams.url();
        String base = galParams.searchBase();
        String filter = galParams.filter();
    
        SearchGalResult result = SearchGalResult.newSearchGalResult(visitor);
        String tokenize = GalUtil.tokenizeKey(galParams, galOp);
        result.setTokenizeKey(tokenize);
    
        if (url == null || url.length == 0 || base == null || filter == null) {
            if (url == null || url.length == 0)
                ZimbraLog.gal.warn("searchLdapGal url is null");
            if (base == null)
                ZimbraLog.gal.warn("searchLdapGal base is null");
            if (filter == null)
                ZimbraLog.gal.warn("searchLdapGal queryExpr is null");
            return result;
        }
    
        if (filter.indexOf("(") == -1) {
            String queryExpr = LdapProvisioning.getFilterDef(filter);
            if (queryExpr != null)
                filter = queryExpr;
        }
        String query = GalUtil.expandFilter(tokenize, filter, n, token, false);
        
        String authMech = galParams.credential().getAuthMech();
        if (authMech.equals(Provisioning.LDAP_AM_KERBEROS5))
            searchLdapGalKrb5(galParams, query, maxResults, rules, token, result);
        else    
            searchLdapGal(galParams, query, maxResults, rules, token, result);
        return result;
    }
    
    private static void searchLdapGal(
            GalParams.ExternalGalParams galParams,
            String query, 
            int maxResults,
            LdapGalMapRules rules,
            String token,
            SearchGalResult result) throws ServiceException, NamingException, IOException {
        
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(galParams.url(), galParams.requireStartTLS(), galParams.credential(), "external GAL");
            searchGal(zlc,
                      galParams.pageSize(),
                      galParams.searchBase(), 
                      query, 
                      maxResults,
                      rules,
                      token,
                      result);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private static void searchLdapGalKrb5(
            GalParams.ExternalGalParams galParams,
            String query, 
            int maxResults,
            LdapGalMapRules rules,
            String token,
            SearchGalResult result) throws NamingException, ServiceException {
        
        try {
            LdapGalCredential credential = galParams.credential();
            Krb5Login.performAs(credential.getKrb5Principal(), credential.getKrb5Keytab(),
                                new SearchGalAction(galParams, query, maxResults, rules, token, result));
        } catch (LoginException le) {
            throw ServiceException.FAILURE("login failed, unable to search GAL", le);
        } catch (PrivilegedActionException pae) {
            // e.getException() should be an instance of NamingException,
            // as only "checked" exceptions will be wrapped in a PrivilegedActionException.
            Exception e = pae.getException();
            if (e instanceof NamingException)
                throw (NamingException)e;
            else // huh?
                throw ServiceException.FAILURE("caught exception, unable to search GAL", e); 
        }
    }
    
    static class SearchGalAction implements PrivilegedExceptionAction {
        
        GalParams.ExternalGalParams galParams;
        String query;
        int maxResults;
        LdapGalMapRules rules;
        String token;
        SearchGalResult result;
        
        SearchGalAction(GalParams.ExternalGalParams arg_galParams,
                        String arg_query, 
                        int arg_maxResults,
                        LdapGalMapRules arg_rules,
                        String arg_token,
                        SearchGalResult arg_result) {
            galParams = arg_galParams;
            query = arg_query;
            maxResults = arg_maxResults;
            rules = arg_rules;
            token = arg_token;
            result = arg_result;
        }
            
        public Object run() throws ServiceException, NamingException, IOException {
            searchLdapGal(galParams, query, maxResults, rules, token, result);
            return null;
        }
    }
    
    
    /* =========================================
     * 
     *         Methods for the new GAL
     *   
     * =========================================
     */
    
    /**
     * 
     * @param params
     */
    public static void galSearch(GalSearchParams params) 
    throws ServiceException, NamingException, IOException{
        String authMech = params.getConfig().getAuthMech();
        if (authMech.equals(Provisioning.LDAP_AM_KERBEROS5))
            galSearchKrb5(params);
        else    
            doGalSearch(params);
    }
    
    private static void doGalSearch(GalSearchParams params) throws ServiceException, NamingException, IOException {
        
        ZimbraLdapContext zlc = null;
        try {
            GalSearchConfig cfg = params.getConfig();
            GalSearchConfig.GalType galType =  params.getConfig().getGalType();

            if (galType == GalSearchConfig.GalType.zimbra)
                zlc = new ZimbraLdapContext(false);
            else
            	zlc = new ZimbraLdapContext(cfg.getUrl(), cfg.getStartTlsEnabled(), cfg.getAuthMech(),
                        cfg.getBindDn(), cfg.getBindPassword(), "external GAL");
            
            searchGal(zlc,
                      cfg.getPageSize(),
                      cfg.getSearchBase(),
                      params.generateLdapQuery(),
                      params.getLimit(),
                      cfg.getRules(),
                      params.getSyncToken(),
                      params.getResult());
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private static void galSearchKrb5(GalSearchParams params) throws NamingException, ServiceException {
        
        try {
            String krb5Principal = params.getConfig().getKerberosPrincipal();
            String krb5Keytab = params.getConfig().getKerberosKeytab();
            Krb5Login.performAs(krb5Principal, krb5Keytab, new GalSearchAction(params));
        } catch (LoginException le) {
            throw ServiceException.FAILURE("login failed, unable to search GAL", le);
        } catch (PrivilegedActionException pae) {
            // e.getException() should be an instance of NamingException,
            // as only "checked" exceptions will be wrapped in a PrivilegedActionException.
            Exception e = pae.getException();
            if (e instanceof NamingException)
                throw (NamingException)e;
            else // huh?
                throw ServiceException.FAILURE("caught exception, unable to search GAL", e); 
        }
    }
    
    static class GalSearchAction implements PrivilegedExceptionAction {
        
        GalSearchParams mParams;
        
        GalSearchAction(GalSearchParams params) {
            mParams = params;
        }
            
        public Object run() throws ServiceException, NamingException, IOException {
            doGalSearch(mParams);
            return null;
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
    public static String getEarlierTimestamp(String timeA, String timeB) {
        if (timeA == null) {
            return timeB;
        } else if (timeB == null) {
            return timeA;
        }
        return timeA.compareTo(timeB) < 0 ? timeA : timeB;
    }

    public static void main(String args[]) throws NamingException, ServiceException {
        
/*
        Date now = new Date();
        String gts = generalizedTime(now);
        System.out.println(now);
        System.out.println(gts);
        Date pnow = generalizedTime(gts);
        System.out.println(pnow);        
        */
    }
    
    //
    // Escape rdn value defined in:
    // http://www.ietf.org/rfc/rfc2253.txt?number=2253
    //
    public static String escapeRDNValue(String rdn) {
        return (String)Rdn.escapeValue(rdn);
    }
    
    public static String unescapeRDNValue(String rdn) {
        return (String)Rdn.unescapeValue(rdn);
    }

    public static String formatMultipleMatchedEntries(SearchResult first, NamingEnumeration rest) throws NamingException {
        StringBuffer dups = new StringBuffer();
        dups.append("[" + first.getNameInNamespace() + "] ");
        while (rest.hasMore()) {
            SearchResult dup = (SearchResult) rest.next();
            dups.append("[" + dup.getNameInNamespace() + "] ");
        }
        
        return new String(dups);
    }
    
    /*
     * Likely an OpenLDAP bug that intermittently if the max requested is 
     * multiple of page size, after a paged search hits the SizeLimitExceededException, 
     * the next search also always throws a SizeLimitExceededException, even 
     * when it should not.
     * 
     * see bug 24168
     */
    public static int adjustPageSize(int maxResults, int pageSize) {
        if (pageSize < 2)
            return pageSize;
        
        if (maxResults >= pageSize && maxResults % pageSize == 0)
            return pageSize - 1;
        else
            return pageSize;
    }
    
    public static String getZimbraSearchBase(Domain domain, GalOp galOp) {
        String sb;
        if (galOp == GalOp.sync) {
            sb = domain.getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase);
            if (sb == null)
                sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
        } else {
            sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, "DOMAIN");
        }
        LdapDomain ld = (LdapDomain) domain;
        if (sb.equalsIgnoreCase("DOMAIN"))
            return ld.getDN();
            //mSearchBase = mDIT.domainDNToAccountSearchDN(ld.getDN());
        else if (sb.equalsIgnoreCase("SUBDOMAINS"))
            return ld.getDN();
        else if (sb.equalsIgnoreCase("ROOT"))
            return "";
        return "";
    }
 }
