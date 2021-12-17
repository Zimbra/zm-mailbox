/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.client;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.SystemUtil;
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
        data.setSignatures(ListUtil.newArrayList(sigs, SoapConverter.TO_SOAP_SIGNATURE));
    }

    public List<ZSignature> getSignatures() {
        return ListUtil.newArrayList(data.getSignatures(), SoapConverter.FROM_SOAP_SIGNATURE);
    }

    public ZSignature getSignature(String id) {
        if (id == null) return null;
        for (Signature sig : data.getSignatures()) {
            if (id.equals(sig.getId())) {
                return SoapConverter.FROM_SOAP_SIGNATURE.apply(sig);
            }
        }
        return null;
    }

    public List<ZIdentity> getIdentities() {
        return ListUtil.newArrayList(data.getIdentities(), SoapConverter.FROM_SOAP_IDENTITY);
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
            } else  {
                newList.add(new ZDataSource(ds));
            }
        }
        return newList;
    }

    public Map<String, List<String>> getAttrs() {
        return MapUtil.multimapToMapOfLists(data.getAttrsMultimap());
    }

    public Map<String, List<String>> getZimletProps() {
        return MapUtil.multimapToMapOfLists(data.getPropsMultimap(ZAttrProvisioning.A_zimbraZimletUserProperties));
    }

    /***
     *
     * @return Set of all lowercased email addresses for this account, including primary name and any aliases.
     */
    public Set<String> getEmailAddresses() {
        Multimap<String, String> attrs = data.getAttrsMultimap();
        Set<String> addresses = new HashSet<String>();
        addresses.add(getName().toLowerCase());
        for (String alias : attrs.get(ZAttrProvisioning.A_zimbraMailAlias)) {
            addresses.add(alias.toLowerCase());
        }
        for (String allowFrom : attrs.get(ZAttrProvisioning.A_zimbraAllowFromAddress)) {
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

    public long getAttachmentSizeLimit() {
        return data.getAttachmentSizeLimit();
    }

    public long getDocumentSizeLimit() {
        return data.getDocumentSizeLimit();
    }

    public Boolean getIsSpellCheckEnabled() {
        return data.getSpellCheckEnabled();
    }

    public String getRecent() {
        return SystemUtil.coalesce(data.getRecentMessageCount(), 0).toString();
    }

    public String getChangePasswordURL() {
        return data.getChangePasswordURL();
    }

    public String getPublicURL() {
        return data.getPublicURL();
    }

    /**
     * Previously, it was assumed that it was possible to get 2 "soapURL"'s in GetInfoResponse, however, they
     * are added using addAttribute with Element.Disposition.CONTENT (i.e. as an attribute in JSON but as an
     * element in XML)  This mechanism enforces that only one exists (latest add wins).
     * Retain list semantics here for backwards compatibility.
     */
    public List<String> getMailURL() {
        return Lists.newArrayList(data.getSoapURL());
    }

    public String getCrumb() {
        return data.getCrumb();
    }

    public String getName() {
        return data.getAccountName();
    }

    public Boolean getAdminDelegated() {
        return data.getAdminDelegated();
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

    public ZLicenses getLicenses() {
        return new ZLicenses(data.getLicense());
    }

    public String getRestURLBase() {
        return data.getRestUrl();
    }

    public String getPublicURLBase() {
        return data.getPublicURL();
    }

    public String getBOSHURL() {
        return data.getBOSHURL();
    }

    public Boolean getIsImapTracked() {
        return data.getIsTrackingIMAP();
    }

    @Override
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
        jo.put("boshURL", getBOSHURL());
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZGetInfoResult %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public long getMailboxQuotaUsed() {
        return SystemUtil.coalesce(data.getQuotaUsed(), -1L);
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
