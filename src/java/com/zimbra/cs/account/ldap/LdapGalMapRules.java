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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.gal.GalGroupHandler;
import com.zimbra.cs.gal.ZimbraGalGroupHandler;

/*
 * maps LDAP attrs into contact attrs. 
 */
public class LdapGalMapRules {
    
    private List<LdapGalMapRule> mRules;
    private List<String> mLdapAttrs;
    private Map<String, LdapGalValueMap> mValueMaps;
    private GalGroupHandler mGroupHandler;

    public LdapGalMapRules(String[] rules, String[] valueMaps, String groupHandlerClass) {
        init(rules, valueMaps, groupHandlerClass);
    }
    
    public LdapGalMapRules(Config config) {
        init(config);
    }
    
    public LdapGalMapRules(Domain domain) {
        init(domain);
    }

    private void init(Entry entry) {
        init(entry.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap),
             entry.getMultiAttr(Provisioning.A_zimbraGalLdapValueMap), 
             entry.getAttr(Provisioning.A_zimbraGalLdapGroupHandlerClass));
    }
    
    private void init(String[] rules, String[] valueMaps, String groupHandlerClass) {
        if (valueMaps !=  null) {
            mValueMaps = new HashMap<String, LdapGalValueMap>(valueMaps.length);
            for (String valueMap : valueMaps) {
                LdapGalValueMap vMap = new LdapGalValueMap(valueMap);
                mValueMaps.put(vMap.getFieldName(), vMap);
            }
        }
        
        mRules = new ArrayList<LdapGalMapRule>(rules.length);
        mLdapAttrs = new ArrayList<String>();
        for (String rule: rules)
            add(rule);
        
        initGroupHandler(groupHandlerClass);
    }
    
    private void initGroupHandler(String className) {
        if (className != null && !className.equals("")) {
            try {
                try {
                    mGroupHandler = (GalGroupHandler)Class.forName(className).newInstance();
                } catch (ClassNotFoundException cnfe) {
                    // ignore and look in extensions
                    mGroupHandler = (GalGroupHandler)ExtensionUtil.findClass(className).newInstance();
                }
            } catch (Exception e) {
                ZimbraLog.gal.error("could not instantiate GalGroupHandler interface of class '" + className + "'; defaulting to ZimbraGalGroupHandler", e);
            }
        }
        if (mGroupHandler == null)
            mGroupHandler = new ZimbraGalGroupHandler();
    }
    
    public String[] getLdapAttrs() {
        return mLdapAttrs.toArray(new String[mLdapAttrs.size()]);
    }
    
    public Map<String, Object> apply(ZimbraLdapContext zlc, SearchResult sr) {
        String dn = sr.getNameInNamespace();
        Attributes ldapAttrs = sr.getAttributes();
        
        HashMap<String,Object> contactAttrs = new HashMap<String, Object>();        
        for (LdapGalMapRule rule: mRules) {
            rule.apply(ldapAttrs, contactAttrs);
        }
        
        if (mGroupHandler.isGroup(sr)) {
            contactAttrs.put(ContactConstants.A_type, ContactConstants.TYPE_GROUP);
            contactAttrs.put(ContactConstants.A_member, mGroupHandler.getMembers(zlc, sr));
        }
        
        return contactAttrs;
    }
    
    public void add(String rule) {
        LdapGalMapRule lgmr = new LdapGalMapRule(rule, mValueMaps);
        mRules.add(lgmr);
        for (String ldapattr: lgmr.getLdapAttrs()) {
            mLdapAttrs.add(ldapattr);
        }
    }
}
