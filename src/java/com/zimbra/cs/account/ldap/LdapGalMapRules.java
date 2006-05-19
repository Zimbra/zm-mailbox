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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.Attributes;

/*
 * maps LDAP attrs into contact attrs. 
 */
public class LdapGalMapRules {
    
    private List<LdapGalMapRule> mRules;
    private String mLdapAttrs[];

    public LdapGalMapRules(String[] rules) {
        mRules = new ArrayList<LdapGalMapRule>(rules.length);
        ArrayList<String> ldapAttrs = new ArrayList<String>();
        for (String rule: rules) {
            LdapGalMapRule lgmr = new LdapGalMapRule(rule);
            mRules.add(lgmr);
            for (String ldapattr: lgmr.getLdapAttrs()) {
                ldapAttrs.add(ldapattr);
            }
        }
        mLdapAttrs = ldapAttrs.toArray(new String[ldapAttrs.size()]);
    }
    
    public String[] getLdapAttrs() {
        return mLdapAttrs;
    }
    
    public Map<String, Object> apply(Attributes ldapAttrs) {
        HashMap<String,Object> contactAttrs = new HashMap<String, Object>();        
        for (LdapGalMapRule rule: mRules) {
            rule.apply(ldapAttrs, contactAttrs);
        }
        return contactAttrs;
    }
}
