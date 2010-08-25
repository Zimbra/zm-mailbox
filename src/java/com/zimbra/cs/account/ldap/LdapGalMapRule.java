/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.IDNUtil;

/*
 * maps LDAP attrs into contact attrs. 
 */
class LdapGalMapRule {

    private String[] mLdapAttrs;
    private String[] mContactAttrs;
    
    // parallel array with mContactAttrs
    private LdapGalValueMap[] mContactAttrsValueMaps;
   
    public LdapGalMapRule(String rule, Map<String, LdapGalValueMap> valueMaps) {
        int p = rule.indexOf('=');
        if (p != -1) {
            String ldapAttr = rule.substring(0, p);
            String contactAttr = rule.substring(p+1);

            mLdapAttrs = (ldapAttr.indexOf(',') != -1) ? ldapAttr.split(",") : new String[] { ldapAttr };
            mContactAttrs = (contactAttr.indexOf(',') != -1) ? contactAttr.split(",") : new String[] { contactAttr };    
            
            mContactAttrsValueMaps = new LdapGalValueMap[mContactAttrs.length];
            if (valueMaps != null) {
                for (int i = 0; i < mContactAttrs.length; i++) {
                    mContactAttrsValueMaps[i] = valueMaps.get(mContactAttrs[i]);
                }
            }
        }
    }
    
    public String[] getLdapAttrs() {
        return mLdapAttrs;
    }

    public String[] getContactAttrs() {
        return mContactAttrs;
    }

    // add contact attr, and also make sure its value is unique for any other 
    // contact attrs in this rule.
    private int addToContactAttrs(Map<String,Object> contactAttrs, Object value, int index) {
        if (index >= mContactAttrs.length) return index;
        for (int i=0; i < index; i++) {
            Object v = contactAttrs.get(mContactAttrs[i]);
                    
            if (v != null) {
                value = mapValue(i, value);
                if (v.equals(value))
                    return index;
            }
        }
        
        contactAttrs.put(mContactAttrs[index], mapValue(index, value));
        index++;
        return index;
    }
    
    private Object mapValue(int index, Object value) {
        LdapGalValueMap valueMap = mContactAttrsValueMaps[index];
        if (valueMap != null)
            return valueMap.apply(value);
        else
            return value;
    }
    
    void apply(Attributes ldapAttrs, Map<String,Object> contactAttrs) {
        AttributeManager attrMgr = null;
        try {
            attrMgr = AttributeManager.getInstance();
        } catch (ServiceException se) {
            ZimbraLog.account.warn("failed to get AttributeManager instance", se);
        }
        
        int index = 0; // index into mContactAttrs
        for (String ldapAttr: mLdapAttrs) {
            if (index >= mContactAttrs.length) return;
            String val[];
            try { val = LdapUtil.getMultiAttrString(ldapAttrs, ldapAttr); } 
            catch (NamingException e) { return; }
            
            IDNType idnType = AttributeManager.idnType(attrMgr, ldapAttr);
            
            if (val.length == 1) {
                index = addToContactAttrs(contactAttrs, IDNUtil.toUnicode(val[0], idnType), index);
            } else if (val.length > 1) {
                if (mContactAttrs.length == 1) {
                    index = addToContactAttrs(contactAttrs, val, index);
                    return;
                } else {
                    for (int i=0; i < val.length; i++) {
                        if (index >= mContactAttrs.length) return;
                        index = addToContactAttrs(contactAttrs, IDNUtil.toUnicode(val[i], idnType), index);                        
                    }
                }
            }
        }
    }
    
}
