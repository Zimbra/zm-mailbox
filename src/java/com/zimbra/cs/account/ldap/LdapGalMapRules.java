/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
class LdapGalMapRules {
    
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
