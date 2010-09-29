/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.account.type.Signature;
import com.zimbra.soap.type.CalDataSource;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.ImapDataSource;
import com.zimbra.soap.type.Pop3DataSource;
import com.zimbra.soap.type.RssDataSource;

public class ZGetInfoResult implements ToZJSONObject {

    private GetInfoResponse data;
    private long expiration;
    
    static Map<String, List<String>> getMap(Element e, String root, String elName) {
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

    public ZGetInfoResult(GetInfoResponse res) {
        this.data = res;
        expiration = data.getLifetime() + System.currentTimeMillis();
    }

    void setSignatures(List<ZSignature> sigs) {
        data.setSignatures(Iterables.transform(sigs, SoapConverter.TO_SOAP_SIGNATURE));
    }

    public List<ZSignature> getSignatures() {
        return Lists.transform(data.getSignatures(), SoapConverter.FROM_SOAP_SIGNATURE);
    }

    public ZSignature getSignature(String id) {
        for (Signature sig : data.getSignatures()) {
            if (id.equals(sig.getId())) {
                return SoapConverter.FROM_SOAP_SIGNATURE.apply(sig);
            }
        }
        return null;
    }

    public List<ZIdentity> getIdentities() {
        return Lists.transform(data.getIdentities(), SoapConverter.FROM_SOAP_IDENTITY);
    }

    public List<ZDataSource> getDataSources() {
        List<ZDataSource> newList = new ArrayList<ZDataSource>();
        for (DataSource ds : data.getDataSources()) {
            if (ds instanceof Pop3DataSource) {
                newList.add(new ZPop3DataSource((Pop3DataSource) ds));
            } else if (ds instanceof ImapDataSource) {
                newList.add(new ZImapDataSource((ImapDataSource) ds));
            } else if (ds instanceof CalDataSource) {
                newList.add(new ZCalDataSource((CalDataSource) ds));
            } else if (ds instanceof RssDataSource) {
                newList.add(new ZRssDataSource((RssDataSource) ds));
            }
        }
        return newList;
    }

    public Map<String, List<String>> getAttrs() {
        return MapUtil.multimapToMapOfLists(data.getAttrsMultimap());
    }

    /***
     *
     * @return Set of all lowercased email addresses for this account, including primary name and any aliases.
     */
    public Set<String> getEmailAddresses() {
        Multimap<String, String> attrs = data.getAttrsMultimap();
        Set<String> addresses = new HashSet<String>();
        for (String alias : attrs.get(Provisioning.A_zimbraMailAlias)) {
            addresses.add(alias.toLowerCase());
        }
        for (String allowFrom : attrs.get(Provisioning.A_zimbraAllowFromAddress)) {
            addresses.add(allowFrom.toLowerCase());
        }
        return addresses;
    }

    public long getExpiration() {
        return expiration;
    }

    public long getLifetime() {
        return data.getLifetime();
    }

    public String getRecent() {
        return SystemUtil.getValue(data.getRecentMessageCount(), 0).toString();
    }

    public String getChangePasswordURL() {
        return data.getChangePasswordURL();
    }

    public String getPublicURL() {
        return data.getPublicURL();
    }

    public List<String> getMailURL() {
        return data.getSoapURLs();
    }

    public String getCrumb() {
        return data.getCrumb();
    }

    public String getName() {
        return data.getAccountName();
    }

    public Map<String, List<String>> getPrefAttrs() {
        return MapUtil.multimapToMapOfLists(data.getPrefsMultimap());
    }

    public ZPrefs getPrefs() {
        return new ZPrefs(data.getPrefsMultimap().asMap());
    }

    public ZFeatures getFeatures() {
        return new ZFeatures(data.getAttrsMultimap().asMap());
    }

    public String getRestURLBase() {
        return data.getRestUrl();
    }

    public String getPublicURLBase() {
        return data.getPublicURL();
    }
    
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("id", getId());
        jo.put("name", getName());
        jo.put("rest", getRestURLBase());
        jo.put("expiration", getExpiration());
        jo.put("lifetime", getLifetime());
        jo.put("mailboxQuotaUsed", getMailboxQuotaUsed());
        jo.put("recent", getRecent());
        jo.putMapList("attrs", getAttrs());
        jo.putMapList("prefs", getPrefAttrs());
        jo.putList("mailURLs", getMailURL());
        jo.put("publicURL", getPublicURL());
        return jo;
    }

    public String toString() {
        return String.format("[ZGetInfoResult %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public long getMailboxQuotaUsed() {
        return SystemUtil.getValue(data.getQuotaUsed(), -1L);
    }

    public String getId() {
        return data.getAccountId();
    }
    
    public String getVersion() {
    	return data.getVersion();
    }

    public Date getPrevSession() {
        Long timestamp = data.getPreviousSessionTime();
        if (timestamp == null) {
            return new Date();
        }
        return new Date(timestamp);
    }
}

