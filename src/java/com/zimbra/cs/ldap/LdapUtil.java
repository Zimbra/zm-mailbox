package com.zimbra.cs.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.ldap.LdapGalMapRules;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.gal.GalSearchConfig;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class LdapUtil {
    
    //
    // Escape rdn value defined in:
    // http://www.ietf.org/rfc/rfc2253.txt?number=2253
    //
    @TODO   // see impl in LegacyLdapUtil, do we need to for unboundid?
    public static String escapeRDNValue(String rdn) {
        LdapTODO.TODO();
        return null;
    }
    
    @TODO
    public static String unescapeRDNValue(String rdn) {
        LdapTODO.TODO();
        return null;
    }
    
    public static String formatMultipleMatchedEntries(ZSearchResultEntry first, ZSearchResultEnumeration rest) 
    throws LdapException {
        StringBuffer dups = new StringBuffer();
        dups.append("[" + first.getDN() + "] ");
        while (rest.hasMore()) {
            ZSearchResultEntry dup = rest.next();
            dups.append("[" + dup.getDN() + "] ");
        }
        
        return new String(dups);
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
    public static void modifyAttrs(ZLdapContext zlc, String dn, Map<String, ? extends Object> attrs, Entry entry) 
    throws ServiceException {
        ZModificationList modList = zlc.createModiftcationList();
        
        AttributeManager attrMgr = AttributeManager.getInst();
        
        for (Map.Entry<String, ? extends Object> attr : attrs.entrySet()) {    
            Object v= attr.getValue();
            String key = attr.getKey();
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
                        modList.removeAttr(key);
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
                        modList.addAttr(key, sa, entry, containsBinaryData, isBinaryTransfer);
                    } else if (doRemove) {
                        modList.removeAttr(key, sa, entry, containsBinaryData, isBinaryTransfer);
                    } else {
                        modList.modifyAttr(key, sa, containsBinaryData, isBinaryTransfer);
                    }
                }
            } else if (v instanceof Map) {
                throw ServiceException.FAILURE("Map is not a supported value type", null);
            } else {
                String s = (v == null ? null : v.toString());
                if (doAdd) {
                    modList.addAttr(key, s, entry, containsBinaryData, isBinaryTransfer);
                }
                else if (doRemove) {
                    modList.removeAttr(key, s, entry, containsBinaryData, isBinaryTransfer);
                }
                else {
                    modList.modifyAttr(key, s, entry, containsBinaryData, isBinaryTransfer);
                }
            }
        }
        zlc.modifyAttributes(dn, modList);
    }
    
    public static void searchLdapOnMaster(String base, String query, String[] returnAttrs, 
            SearchLdapVisitor visitor) throws ServiceException {
        searchZimbraLdap(base, query, returnAttrs, true, visitor);
    }

    public static void searchLdapOnReplica(String base, String query, String[] returnAttrs, 
            SearchLdapVisitor visitor) throws ServiceException {
        searchZimbraLdap(base, query, returnAttrs, false, visitor);
    }
      
    private static void searchZimbraLdap(String base, String query, String[] returnAttrs, 
            boolean useMaster, SearchLdapVisitor visitor) throws ServiceException {
        
        SearchLdapOptions searchOptions = new SearchLdapOptions(base, query, returnAttrs, null, visitor);
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.get(useMaster));
            zlc.searchPaged(searchOptions);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @TODO // see impl in LegacyLdapUtil
    public static void searchGal(ZLdapContext zlc,
            GalSearchConfig.GalType galType,
            int pageSize,
            String base, 
            String query, 
            int maxResults,
            LdapGalMapRules rules,
            String token,
            SearchGalResult result) throws ServiceException {
        LdapTODO.TODO();
    }
    
    @TODO // see impl in LegacyLdapUtil
    public static SearchGalResult searchLdapGal(
            GalParams.ExternalGalParams galParams, 
            GalOp galOp,
            String n,
            int maxResults,
            LdapGalMapRules rules,
            String token,
            GalContact.Visitor visitor) throws ServiceException {
        LdapTODO.TODO();
        return null;
    }
    
    @TODO // see impl in LegacyLdapUtil
    public static void ldapAuthenticate(String urls[], boolean requireStartTLS, String principal, String password) {
        LdapTODO.TODO();
    }
    
    @TODO // see impl in LegacyLdapUtil
    public static void ldapAuthenticate(String url[], boolean requireStartTLS, String password, String searchBase, 
            String searchFilter, String searchDn, String searchPassword) throws ServiceException {
        
    }
}
