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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/*
 * maps LDAP attrs into contact attrs. 
 */
public class LdapGalMapRule {

    private String[] mLdapAttrs;
    private String[] mContactAttrs;
   
    public LdapGalMapRule(String rule) {
        int p = rule.indexOf('=');
        if (p != -1) {
            String ldapAttr = rule.substring(0, p);
            String contactAttr = rule.substring(p+1);

            mLdapAttrs = (ldapAttr.indexOf(',') != -1) ? ldapAttr.split(",") : new String[] { ldapAttr };
            mContactAttrs = (contactAttr.indexOf(',') != -1) ? contactAttr.split(",") : new String[] { contactAttr };            
        }
    }
    
    public static List<LdapGalMapRule> parseRules(String[] rules) {
        ArrayList<LdapGalMapRule> result = new ArrayList<LdapGalMapRule>(rules.length);
        for (String rule: rules) {
            result.add(new LdapGalMapRule(rule));
        }
        return result;
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
            if (v != null && v.equals(value)) return index;
        }
        contactAttrs.put(mContactAttrs[index++], value);
        return index;
    }
    
    void apply(Attributes ldapAttrs, Map<String,Object> contactAttrs) {
        int index = 0; // index into mContactAttrs
        for (String ldapAttr: mLdapAttrs) {
            if (index >= mContactAttrs.length) return;
            String val[];
            try { val = LdapUtil.getMultiAttrString(ldapAttrs, ldapAttr); } 
            catch (NamingException e) { return; }
            
            if (val.length == 1) {
                index = addToContactAttrs(contactAttrs, val[0], index);
            } else if (val.length > 1) {
                if (mContactAttrs.length == 1) {
                    index = addToContactAttrs(contactAttrs, val, index);
                    return;
                } else {
                    for (int i=0; i < val.length; i++) {
                        if (index >= mContactAttrs.length) return;
                        index = addToContactAttrs(contactAttrs, val[i], index);                        
                    }
                }
            }
        }
    }
    
}
