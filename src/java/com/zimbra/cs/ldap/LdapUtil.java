package com.zimbra.cs.ldap;

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

public class LdapUtil {
    
    public static String getAttrString(ZSearchResultEntry entry, String name) throws LdapException {
        AttributeManager attrMgr = AttributeManager.getInst();
        boolean containsBinaryData = attrMgr == null ? false : attrMgr.containsBinaryData(name);
        boolean isBinaryTransfer = attrMgr == null ? false : attrMgr.isBinaryTransfer(name);
        
        String attrName = LdapUtilCommon.attrNameToBinaryTransferAttrName(isBinaryTransfer, name);
        
        return entry.getAttrString(attrName, containsBinaryData);
    }
    
    /**
     * Enumerates over the specified attributes and populates the specified map. The key in the map is the
     * attribute ID. For attrs with a single value, the value is a String, and for attrs with multiple values
     * the value is an array of Strings.
     * 
     * @param attrs the attributes to enumerate over
     * @throws NamingException
     */
    public static Map<String, Object> getAttrs(ZSearchResultEntry entry) throws LdapException {
        return getAttrs(entry, null);
    }
    
    /**
     * 
     * @param attrs
     * @param binaryAttrs set of binary attrs, useful for searching external LDAP.
     *                    if null, only binary attrs declared in zimbra schema are recognized.  
     * @return
     * @throws NamingException
     */
    public static Map<String, Object> getAttrs(ZSearchResultEntry entry, Set<String> binaryAttrs) throws LdapException {
        return entry.getAttrs(binaryAttrs);
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
}
