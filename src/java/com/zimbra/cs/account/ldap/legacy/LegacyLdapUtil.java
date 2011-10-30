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

package com.zimbra.cs.account.ldap.legacy;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.krb5.Krb5Login;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.account.ldap.LdapGalCredential;
import com.zimbra.cs.account.ldap.LdapGalMapRules;
import com.zimbra.cs.account.ldap.legacy.LegacyJNDIAttributes;
import com.zimbra.cs.account.ldap.legacy.entry.LdapConfig;
import com.zimbra.cs.gal.GalSearchConfig;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.LdapTODO.*;

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

/**
 * @author schemers
 */
public class LegacyLdapUtil {
        
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
    
    @SDKDONE
    public static String getAttrString(Attributes attrs, String name) throws NamingException {
        AttributeManager attrMgr = AttributeManager.getInst();
        boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(name);
        boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(name);
        
        String attrName = LdapUtil.attrNameToBinaryTransferAttrName(isBinaryTransfer, name);
        
        Attribute attr = attrs.get(attrName);
        if (attr != null) {
            Object o = attr.get();
            if (o instanceof String) {
                return (String) o;
            } else if (containsBinaryData) {
                return ByteUtil.encodeLDAPBase64((byte[])o);
            } else {
                return new String((byte[])o);
            }
        } else {
            return null;
        }
    }

    public static String[] getMultiAttrString(Attributes attrs, String name) 
    throws NamingException {
        AttributeManager attrMgr = AttributeManager.getInst();
        boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(name);
        boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(name);
        return getMultiAttrString(attrs, name, containsBinaryData, isBinaryTransfer);
    }
    
    public static String[] getMultiAttrString(Attributes attrs, String name, boolean containsBinaryData, boolean isBinaryTransfer) 
    throws NamingException {
        String attrName = LdapUtil.attrNameToBinaryTransferAttrName(isBinaryTransfer, name);
        
        Attribute attr = attrs.get(attrName);
        if (attr != null) {
            String result[] = new String[attr.size()];
            for (int i=0; i < attr.size(); i++) {
                Object o = attr.get(i);
                if (o instanceof String) {
                    result[i] = (String) o;
                } else if (containsBinaryData) {
                    result[i] = ByteUtil.encodeLDAPBase64((byte[])o);
                } else {
                    result[i] = new String((byte[])o);
                }
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
    @SDKDONE
    public static Map<String, Object> getAttrs(Attributes attrs) throws NamingException {
        return getAttrs(attrs, null);
    }
    
    /**
     * 
     * @param attrs
     * @param binaryAttrs set of binary attrs, useful for searching external LDAP.
     *                    if null, only binary attrs declared in zimbra schema are recognized.  
     * @return
     * @throws NamingException
     */
    @SDKDONE
    public static Map<String, Object> getAttrs(Attributes attrs, Set<String> binaryAttrs) throws NamingException {
        Map<String,Object> map = new HashMap<String,Object>();  
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (NamingEnumeration ne = attrs.getAll(); ne.hasMore(); ) {
            Attribute attr = (Attribute) ne.next();
            String transferAttrName = attr.getID();
            
            String attrName = LdapUtil.binaryTransferAttrNameToAttrName(transferAttrName);
            
            boolean containsBinaryData = 
                (attrMgr != null && attrMgr.containsBinaryData(attrName)) ||
                (binaryAttrs != null && binaryAttrs.contains(attrName));
            
            if (attr.size() == 1) {
                Object o = attr.get();
                if (o instanceof String) {
                    map.put(attrName, o);
                } else if (containsBinaryData) {
                    map.put(attrName, ByteUtil.encodeLDAPBase64((byte[])o));
                } else 
                    map.put(attrName, new String((byte[])o));
            } else {
                String result[] = new String[attr.size()];
                for (int i=0; i < attr.size(); i++) {
                    Object o = attr.get(i);
                    if (o instanceof String) {
                        result[i] = (String) o;
                    } else if (containsBinaryData) {
                        result[i] = ByteUtil.encodeLDAPBase64((byte[])o);
                    } else {
                        result[i] = new String((byte[])o);
                    }
                }
                map.put(attrName, result);
            }
        }
        return map;
    }
    
    @SDKDONE
    public static Attribute addAttr(Attributes attrs, String name, String value) {
        BasicAttribute a = new BasicAttribute(name);
        a.add(value);
        attrs.put(a);
        return a;
    }
    
    @SDKDONE
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
    @SDKDONE
    private static void modifyAttr(ArrayList<ModificationItem> modList, String name, String value, 
            com.zimbra.cs.account.Entry entry, boolean containsBinaryData, boolean isBinaryTransfer) {
        int mod_op = (value == null || value.equals("")) ? DirContext.REMOVE_ATTRIBUTE : DirContext.REPLACE_ATTRIBUTE;
        if (mod_op == DirContext.REMOVE_ATTRIBUTE) {
            // make sure it exists
            if (entry.getAttr(name, false) == null)
                return;
        }
        
        BasicAttribute ba = newAttribute(isBinaryTransfer, name);
        if (mod_op == DirContext.REPLACE_ATTRIBUTE)
            ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value));
        modList.add(new ModificationItem(mod_op, ba));
    }
    
    @SDKDONE
    private static void modifyAttr(ArrayList<ModificationItem> modList, String name, String[] value, 
            boolean containsBinaryData, boolean isBinaryTransfer) {
        BasicAttribute ba = newAttribute(isBinaryTransfer, name);
        for (int i=0; i < value.length; i++) {
            ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value[i]));
        }
        modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, ba));
    }

    /**
     * remove the attr with the specified value
     */
    @SDKDONE
    private static void removeAttr(ArrayList<ModificationItem> modList, String name, String value, 
            com.zimbra.cs.account.Entry entry, boolean containsBinaryData, boolean isBinaryTransfer) {
        if (!LdapUtil.contains(entry.getMultiAttr(name, false), value)) return;
        
        BasicAttribute ba = newAttribute(isBinaryTransfer, name);
        ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value));
        modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));
    }
    
    /**
     * remove the attr with the specified value
     */
    @SDKDONE
    private static void removeAttr(ArrayList<ModificationItem> modList, String name, String value[], 
            com.zimbra.cs.account.Entry entry, boolean containsBinaryData, boolean isBinaryTransfer) {
        String[] currentValues = entry.getMultiAttr(name, false);
        if (currentValues == null || currentValues.length == 0) return;
        
        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (!LdapUtil.contains(currentValues, value[i])) continue;
            if (ba == null) ba = newAttribute(isBinaryTransfer, name);
            ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value[i]));
        }
        if (ba != null) modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));

    }

    /**
     * add an additional attr with the specified value
     */
    @SDKDONE
    private static void addAttr(ArrayList<ModificationItem> modList, String name, String value, 
            com.zimbra.cs.account.Entry entry, boolean containsBinaryData, boolean isBinaryTransfer) {
        if (LdapUtil.contains(entry.getMultiAttr(name, false), value)) return;     
        
        BasicAttribute ba = newAttribute(isBinaryTransfer, name);
        ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value));
        modList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, ba));
    }

    
    /**
     * add an additional attr with the specified value
     */
    @SDKDONE
    private static void addAttr(ArrayList<ModificationItem> modList, String name, String value[], 
            com.zimbra.cs.account.Entry entry, boolean containsBinaryData, boolean isBinaryTransfer) {
        String[] currentValues = entry.getMultiAttr(name, false);
        
        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (LdapUtil.contains(currentValues, value[i])) continue;
            if (ba == null) ba = newAttribute(isBinaryTransfer, name);
            ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value[i]));
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
    @TODO  // make it private, should be called only from LegacyLdapHelper
    public static void modifyAttrs(LegacyZimbraLdapContext zlc, String dn, Map attrs, com.zimbra.cs.account.Entry entry) 
    throws NamingException, ServiceException {
        ArrayList<ModificationItem> modlist = new ArrayList<ModificationItem>();
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
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
             
            boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(key);
            boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(key);
            
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
                    if (doAdd) {
                        addAttr(modlist, key, sa, entry, containsBinaryData, isBinaryTransfer);
                    } else if (doRemove) {
                        removeAttr(modlist, key, sa, entry, containsBinaryData, isBinaryTransfer);
                    } else {
                        modifyAttr(modlist, key, sa, containsBinaryData, isBinaryTransfer);
                    }
                }
            } else if (v instanceof Map) {
                throw ServiceException.FAILURE("Map is not a supported value type", null);
            } else {
                String s = (v == null ? null : v.toString());
                if (doAdd) {
                    addAttr(modlist, key, s, entry, containsBinaryData, isBinaryTransfer);
                }
                else if (doRemove) {
                    removeAttr(modlist, key, s, entry, containsBinaryData, isBinaryTransfer);
                }
                else {
                    modifyAttr(modlist, key, s, entry, containsBinaryData, isBinaryTransfer);
                }
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
    @SDKDONE
    public static void mapToAttrs(Map mapAttrs, Attributes attrs) {
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (Iterator mit=mapAttrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry me = (Entry) mit.next();
            
            String attrName = (String)me.getKey();
            Object v = me.getValue();
            
            boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(attrName);
            boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(attrName);
            
            if (v instanceof String) {
                // attrs.put(name, decodeBase64IfBinary(isBinary, (String)v));
                BasicAttribute a = newAttribute(isBinaryTransfer, attrName);
                a.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, (String)v));
                attrs.put(a);
            } else if (v instanceof String[]) {
                String[] sa = (String[]) v;
                BasicAttribute a = newAttribute(isBinaryTransfer, attrName);
                for (int i=0; i < sa.length; i++) {
                    a.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, sa[i]));
                }
                attrs.put(a);
            } else if (v instanceof Collection) {
                Collection c = (Collection) v;
                BasicAttribute a = newAttribute(isBinaryTransfer, attrName);
                for (Object o : c) {
                	a.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, o.toString()));
                }
                attrs.put(a);
            }
        }
    }
    
    public static String[] removeMultiValue(String values[], String value) {
        List<String> list = new ArrayList<String>(Arrays.asList(values));
        boolean updated = list.remove(value);
        if (updated) {
            return list.toArray(new String[list.size()]);
        } else {
            return values;
        }
    }
      
    @SDKDONE
      private static void searchZimbraLdap(String base, String query, String[] returnAttrs, 
              boolean useMaster, SearchLdapOptions.SearchLdapVisitor visitor) 
      throws ServiceException {
          LegacyZimbraLdapContext zlc = null;
          try {
              zlc = new LegacyZimbraLdapContext(useMaster);
              searchLdap(zlc, base, query, returnAttrs, null, SearchControls.SUBTREE_SCOPE, visitor);
          } finally {
              LegacyZimbraLdapContext.closeContext(zlc);
          }
      }
             
      /**
       * Important Note: caller is responsible to close the ZimbraLdapContext
       */
      @SDKDONE
      public static void searchLdap(LegacyZimbraLdapContext zlc, String base, String query, String[] returnAttrs, 
              Set<String> binaryAttrs, int searchScope, SearchLdapOptions.SearchLdapVisitor visitor) 
      throws ServiceException {
          
          int maxResults = 0; // no limit
          
          try {
              SearchControls searchControls =
                  new SearchControls(searchScope, maxResults, 0, returnAttrs, false, false);

              //Set the page size and initialize the cookie that we pass back in subsequent pages
              int pageSize = LegacyLdapUtil.adjustPageSize(maxResults, 1000);
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
                          if (visitor.wantAttrMapOnVisit()) {
                              visitor.visit(dn, LegacyLdapUtil.getAttrs(attrs, binaryAttrs), 
                                      new LegacyJNDIAttributes(attrs));
                          } else {
                              visitor.visit(dn, new LegacyJNDIAttributes(attrs));
                          }
                      }
                      cookie = zlc.getCookie();
                  } while (cookie != null);
              } catch (SearchLdapOptions.StopIteratingException e) { 
                  // break out of the loop and close the ne
              } finally {
                  if (ne != null) ne.close();
              }
          } catch (NamingException e) {
              throw ServiceException.FAILURE("unable to search ldap", e);
          } catch (IOException e) {
              throw ServiceException.FAILURE("unable to search ldap", e);
          }
      }

      public static void searchGal(LegacyZimbraLdapContext zlc,
                                   GalSearchConfig.GalType galType,
                                   int pageSize,
                                   String base, 
                                   String query, 
                                   int maxResults,
                                   LdapGalMapRules rules,
                                   String token,
                                   SearchGalResult result) throws ServiceException {

        String tk = token != null && !token.equals("")? token : LdapConstants.EARLIEST_SYNC_TOKEN;
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
                        Attributes attributes = sr.getAttributes();
                        addGalResult(zlc, galType, dn, base, rules, attributes, result);
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
            if (newToken == null || (token != null && token.equals(newToken)) || newToken.equals(LdapConstants.EARLIEST_SYNC_TOKEN))
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
  
    private static void addGalResult(LegacyZimbraLdapContext zlc,
            GalSearchConfig.GalType galType, String dn, String base, LdapGalMapRules rules,
            Attributes attributes, SearchGalResult result) throws ServiceException {
        
        LegacyJNDIAttributes attrs = new LegacyJNDIAttributes(attributes);
        
        GalContact lgc = new GalContact(galType, dn, rules.apply(zlc, base, dn, attrs));
        String mts = (String) lgc.getAttrs().get("modifyTimeStamp");
        result.setToken(LdapUtil.getLaterTimestamp(result.getToken(), mts));
        String cts = (String) lgc.getAttrs().get("createTimeStamp");
        result.setToken(LdapUtil.getLaterTimestamp(result.getToken(), cts));
        result.addMatch(lgc);
        ZimbraLog.gal.debug("dn=" + dn + ", mts=" + mts + ", cts=" + cts);
    }
    
    private static void getGalEntryByDn(LegacyZimbraLdapContext zlc,
            GalSearchConfig.GalType galType,
            String dn,
            LdapGalMapRules rules,
            SearchGalResult result) throws ServiceException {
        String reqAttrs[] = rules.getLdapAttrs();
          
        if (ZimbraLog.gal.isDebugEnabled()) {
            StringBuffer returnAttrs = new StringBuffer();
            for (String a: reqAttrs) {
                returnAttrs.append(a + ",");
            }
           
            ZimbraLog.gal.debug("getGalEntryByDn: " +
                    ", dn=" + dn + 
                    ", attrs=" + returnAttrs);
        }
        
        try {
            Attributes attributes = zlc.getAttributes(dn);
            addGalResult(zlc, galType, dn, null, rules, attributes, result);
        } catch (NamingException e) {
            ZimbraLog.gal.debug("getGalEntryByDn: no such dn: " + dn, e);
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
            String queryExpr = GalSearchConfig.getFilterDef(filter);
            if (queryExpr != null)
                filter = queryExpr;
        }
        String query = GalUtil.expandFilter(tokenize, filter, n, token);
        
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
        
        LegacyZimbraLdapContext zlc = null;
        try {
            zlc = new LegacyZimbraLdapContext(galParams.url(), galParams.requireStartTLS(), 
                    galParams.credential(), rules.getBinaryLdapAttrs(), "external GAL");
            searchGal(zlc,
                      GalSearchConfig.GalType.ldap,
                      galParams.pageSize(),
                      galParams.searchBase(), 
                      query, 
                      maxResults,
                      rules,
                      token,
                      result);
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
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
        
        LegacyZimbraLdapContext zlc = null;
        try {
            GalSearchConfig cfg = params.getConfig();
            GalSearchConfig.GalType galType =  params.getConfig().getGalType();

            if (galType == GalSearchConfig.GalType.zimbra) {
                zlc = new LegacyZimbraLdapContext(false);
            } else {
            	zlc = new LegacyZimbraLdapContext(cfg.getUrl(), cfg.getStartTlsEnabled(), cfg.getAuthMech(),
                        cfg.getBindDn(), cfg.getBindPassword(), cfg.getRules().getBinaryLdapAttrs(), "external GAL");
            }
            
            String fetchEntryByDn = params.getSearchEntryByDn();
            if (fetchEntryByDn == null) {
            searchGal(zlc,
                      galType,
                      cfg.getPageSize(),
                      cfg.getSearchBase(),
                      params.generateLdapQuery(),
                      params.getLimit(),
                      cfg.getRules(),
                      params.getSyncToken(),
                      params.getResult());
            } else {
                getGalEntryByDn(zlc, galType, fetchEntryByDn, cfg.getRules(), params.getResult());
            }
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
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
    

    @SDKDONE
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
    
    @SDKDONE
    private static BasicAttribute newAttribute(boolean isBinaryTransfer, String attrName) {
        String transferAttrName = LdapUtil.attrNameToBinaryTransferAttrName(isBinaryTransfer, attrName);
        return new BasicAttribute(transferAttrName);
    }
    

    public static void main(String[] args)  {
        try {
            com.zimbra.cs.account.Config config = Provisioning.getInstance().getConfig();
            String dn = ((LdapConfig)config).getDN();
            
            LegacyZimbraLdapContext zlc = new LegacyZimbraLdapContext(true);
            
            ArrayList<ModificationItem> modlist = new ArrayList<ModificationItem>();
            modifyAttr(modlist, Provisioning.A_description, null, config, false, false);
            
            ModificationItem[] mods = new ModificationItem[modlist.size()];
            modlist.toArray(mods);
            zlc.modifyAttributes(dn, mods);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        
 }
