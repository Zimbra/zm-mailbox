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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalGroupHandler;

/*
 * maps LDAP attrs into contact attrs. 
 */
public class LdapGalMapRules {
    
    private List<LdapGalMapRule> mRules;
    private List<String> mLdapAttrs;
    private Set<String> mBinaryLdapAttrs;
    private Map<String, LdapGalValueMap> mValueMaps;
    private GalGroupHandler mGroupHandler;
    private boolean mFetchGroupMembers;
    private boolean mNeedSMIMECerts;

    public LdapGalMapRules(String[] rules, String[] valueMaps, String groupHandlerClass) {
        init(rules, valueMaps, groupHandlerClass);
    }
    
    public LdapGalMapRules(Config config, boolean isZimbraGal) {
        init(config, isZimbraGal);
    }
    
    public LdapGalMapRules(Domain domain, boolean isZimbraGal) {
        init(domain, isZimbraGal);
    }

    private void init(Entry entry, boolean isZimbraGal) {
        String groupHanlderClass = null;
        if (!isZimbraGal)
            groupHanlderClass = entry.getAttr(Provisioning.A_zimbraGalLdapGroupHandlerClass);
        
        init(entry.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap),
             entry.getMultiAttr(Provisioning.A_zimbraGalLdapValueMap), 
             groupHanlderClass);
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
        mBinaryLdapAttrs = new HashSet<String>();
        for (String rule: rules)
            add(rule);
        
        mGroupHandler = GalGroupHandler.getHandler(groupHandlerClass);
        ZimbraLog.gal.debug("groupHandlerClass=" + groupHandlerClass + ", handler instantiated=" + mGroupHandler.getClass().getCanonicalName());
    }
    
    public void setFetchGroupMembers(boolean fetchGroupMembers) {
        mFetchGroupMembers = fetchGroupMembers;
    }
    
    public void setNeedSMIMECerts(boolean needSMIMECerts) {
        mNeedSMIMECerts = needSMIMECerts;
    }
    
    public String[] getLdapAttrs() {
        return mLdapAttrs.toArray(new String[mLdapAttrs.size()]);
    }
    
    public Set<String> getBinaryLdapAttrs() {
        return mBinaryLdapAttrs;
    }
    
    public Map<String, Object> apply(ZimbraLdapContext zlc, String searchBase, SearchResult sr) {
        String dn = sr.getNameInNamespace();
        Attributes ldapAttrs = sr.getAttributes();
        
        HashMap<String,Object> contactAttrs = new HashMap<String, Object>();        
        for (LdapGalMapRule rule: mRules) {
        	if (!mNeedSMIMECerts && rule.isSMIMECertificate()) {
        		continue;
        	}
            rule.apply(ldapAttrs, contactAttrs);
        }
        
        if (mGroupHandler.isGroup(sr)) {
            contactAttrs.put(ContactConstants.A_type, ContactConstants.TYPE_GROUP);
            
            if (mFetchGroupMembers)
                contactAttrs.put(ContactConstants.A_member, mGroupHandler.getMembers(zlc, searchBase, sr));
            else {
                // for internal LDAP, all members are on the DL entry and have been fetched/mapped
                // delete it.
                contactAttrs.remove(ContactConstants.A_member);
            }
        }
        
        return contactAttrs;
    }
    
    public void add(String rule) {
        LdapGalMapRule lgmr = new LdapGalMapRule(rule, mValueMaps);
        mRules.add(lgmr);
        for (String ldapattr: lgmr.getLdapAttrs()) {
            mLdapAttrs.add(ldapattr);
            
            if (lgmr.isBinary()) {
                mBinaryLdapAttrs.add(ldapattr);
            }
        }
    }
}
