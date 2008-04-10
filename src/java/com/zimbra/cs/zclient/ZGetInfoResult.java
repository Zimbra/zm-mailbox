/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.cs.account.Provisioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZGetInfoResult {

    private String mVersion;
    private String mId;
    private String mName;
    private String mRestURLBase;
    private String mCrumb;
    private long mLifetime;
    private long mExpiration;
    private long mMailboxQuotaUsed;
    private String mRecent;
    private Map<String, List<String>> mAttrs;
    private Map<String, List<String>> mPrefAttrs;
    private ZPrefs mPrefs;
    private ZFeatures mFeatures;
    private List<ZIdentity> mIdentities;
    private List<ZDataSource> mDataSources;
    private List<ZSignature> mSignatures;
    private List<String> mMailURLs;
    private Set<String> mEmailAddresses;

    static Map<String, List<String>> getMap(Element e, String root, String elName) throws ServiceException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        Element attrsEl = e.getOptionalElement(root);
        if (attrsEl != null) {

            for (KeyValuePair pair : attrsEl.listKeyValuePairs(elName, AccountConstants.A_NAME)) {
            //StringUtil.addToMultiMap(mAttrs, pair.getKey(), pair.getValue());
                String name = pair.getKey();
                List<String> list = result.get(name);
                if (list == null) {
                    list = new ArrayList<String>();
                    result.put(name, list);
                }
                list.add(pair.getValue());
            }
        }
        return result;
    }
    
    public ZGetInfoResult(Element e) throws ServiceException {
    	mVersion = e.getAttribute(AccountConstants.E_VERSION, "unknown");
        mId = e.getAttribute(AccountConstants.E_ID, null); // TODO: ID was just added to GetInfo, remove ,null shortly...
        mName = e.getAttribute(AccountConstants.E_NAME);
        mCrumb = e.getAttribute(AccountConstants.E_CRUMB, null);
        mLifetime = e.getAttributeLong(AccountConstants.E_LIFETIME);
        mMailboxQuotaUsed = e.getAttributeLong(AccountConstants.E_QUOTA_USED, -1);
        mExpiration  = mLifetime + System.currentTimeMillis();
        mAttrs = getMap(e, AccountConstants.E_ATTRS, AccountConstants.E_ATTR);
        mPrefAttrs = getMap(e, AccountConstants.E_PREFS, AccountConstants.E_PREF);
        mPrefs = new ZPrefs(mPrefAttrs);
        mFeatures = new ZFeatures(mAttrs);
        mRecent = e.getAttribute(AccountConstants.E_RECENT_MSGS, "0");
        mRestURLBase = e.getAttribute(AccountConstants.E_REST, null);

        mMailURLs = new ArrayList<String>();
        String mailUrl = e.getAttribute(AccountConstants.E_SOAP_URL, null);
        if (mailUrl != null)
            mMailURLs.add(mailUrl);

        mIdentities = new ArrayList<ZIdentity>();
        Element identities = e.getOptionalElement(AccountConstants.E_IDENTITIES);
        if (identities != null) {
            for (Element identity: identities.listElements(AccountConstants.E_IDENTITY)) {
                mIdentities.add(new ZIdentity(identity));
            }
        }
        mDataSources = new ArrayList<ZDataSource>();
        Element sources = e.getOptionalElement(AccountConstants.E_DATA_SOURCES);
        if (sources != null) {
            for (Element source: sources.listElements()) {
                if (source.getName().equals(MailConstants.E_DS_POP3))
                    mDataSources.add(new ZPop3DataSource(source));
            }
        }
        mSignatures = new ArrayList<ZSignature>();
        Element sigs = e.getOptionalElement(AccountConstants.E_SIGNATURES);
        if (sigs != null) {
            for (Element sig: sigs.listElements()) {
                if (sig.getName().equals(MailConstants.E_SIGNATURE))
                    mSignatures.add(new ZSignature(sig));
            }
        }

    }

    void setSignatures(List<ZSignature> sigs) {
        mSignatures = sigs;
    }
    
    public List<ZSignature> getSignatures() {
        return mSignatures;
    }

    public ZSignature getSignature(String id) {
        for (ZSignature sig : getSignatures()) {
            if (sig.getId().equals(id))
                return sig;
        }
        return null;
    }
    
    public List<ZIdentity> getIdentities() {
        return mIdentities;
    }

    public List<ZDataSource> getDataSources() {
        return mDataSources;
    }

    public Map<String, List<String>> getAttrs() {
        return mAttrs;
    }

    /***
     * 
     * @return Set of all lowercased email addresses for this account, including primary name and any aliases. 
     */
    public synchronized Set<String> getEmailAddresses() {
        if (mEmailAddresses == null) {
            mEmailAddresses =  new HashSet<String>();
            mEmailAddresses.add(getName().toLowerCase());
            List<String> aliasList = getAttrs().get(Provisioning.A_zimbraMailAlias);
            if (aliasList != null)
                for (String alias: aliasList)
                    mEmailAddresses.add(alias.toLowerCase());
            List<String> allowFromList = getAttrs().get(Provisioning.A_zimbraAllowFromAddress);
            if (allowFromList != null)
                for (String allowFrom: allowFromList)
                    mEmailAddresses.add(allowFrom.toLowerCase());
        }
        return mEmailAddresses;
    }

    public long getExpiration() {
        return mExpiration;
    }

    public long getLifetime() {
        return mLifetime;
    }

    public String getRecent() {
        return mRecent;
    }

    public List<String> getMailURL() {
        return mMailURLs;
    }

    public String getCrumb() {
        return mCrumb;
    }

    public String getName() {
        return mName;
    }

    public Map<String, List<String>> getPrefAttrs() {
        return mPrefAttrs;
    }

    public ZPrefs getPrefs() {
        return mPrefs;
    }
    
    public ZFeatures getFeatures() {
        return mFeatures; 
    }

    public String getRestURLBase() {
        return mRestURLBase;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);        
        sb.add("name", mName);
        sb.add("rest", mRestURLBase);
        sb.addDate("expiration", mExpiration);
        sb.add("lifetime", mLifetime);
        sb.add("mailboxQuotaUsed", mMailboxQuotaUsed);
        sb.add("recent", mRecent);
        sb.add("attrs", mAttrs);
        sb.add("prefs", mPrefAttrs);
        sb.add("mailURLs", mMailURLs, true, true);
        sb.endStruct();
        return sb.toString();
    }

    public long getMailboxQuotaUsed() {
        return mMailboxQuotaUsed;
    }

    public String getId() {
        return mId;
    }
    
    public String getVersion() {
    	return mVersion;
    }
}

