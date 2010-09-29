/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.AccountCalDataSource;
import com.zimbra.soap.account.type.AccountDataSource;
import com.zimbra.soap.account.type.AccountImapDataSource;
import com.zimbra.soap.account.type.AccountPop3DataSource;
import com.zimbra.soap.account.type.AccountRssDataSource;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.Cos;
import com.zimbra.soap.account.type.Identity;
import com.zimbra.soap.account.type.Pref;
import com.zimbra.soap.account.type.Signature;


/**
<GetInfoResponse>
   <version>{version}</version>
   <id>{account-id}</id>
   <name>{account-name}</name>
   <lifetime>...</lifetime>   
   [<rest>{account-base-REST-url}</rest>
    <used>{used}</used>
    <prevSession>{previous-SOAP-session}</prevSession>
    <accessed>{last-SOAP-access}</accessed>
    <recent>{recent-messages}</recent>
   ]
   <docSizeLimit>{document-size-limit}</docSizeLimit>
   <attSizeLimit>{attachment-size-limit}</attSizeLimit>
   <cos name="cos-name" id="cos-id"/>
   <attrs>
    <attr name="{name}">{value}</a>
     ...
    <attr name="{name}">{value}</a>
   </attrs>
   <prefs>
     <pref name="{name}">{value}</pref>
     ...
     <pref name="{name}">{value}</pref>
   </prefs>
   <props>
     <prop zimlet="{zimlet-name}" name="{name}">{value}</prop>
     ...
     <prop zimlet="{zimlet-name}" name="{name}">{value}</prop>
   </props>
   <zimlets>
     <zimlet>
       <zimletContext baseUrl="..." priority="..." presence="{zimlet-presence}"/>
       <zimlet>...</zimlet>
       <zimletConfig>...</zimletConfig>
     </zimlet>
     ...
   </zimlets>
   <soapURL>{soap-url}</soapURL>+
   <publicURL>{account-base-public-url}</publicURL>
   <identities>
     <identity name={identity-name} id="...">
       <a name="{name}">{value}</a>
       ...
       <a name="{name}">{value}</a>
     </identity>*
   </identities>
   <signatures>
     <signature name={signature-name} id="...">
       <a name="{name}">{value}</a>
       ...
       <a name="{name}">{value}</a>
     </signature>*
   </signatures>
   <dataSources>
     {data-source}
     ...
   </dataSources>*
   <childAccounts>
     <childAccount name="{child-account-name}" visible="0|1" id="{child-account-id}">
         <attrs>
            <attr name="{name}">{value}</a>*
         </attrs>
     </childAccount>*
   </childAccounts>
   [<license status="inGracePeriod|bad"/>]
</GetInfoResponse>
 */
@XmlRootElement(name="GetInfoResponse")
@XmlType(propOrder = {})
public class GetInfoResponse {
    @XmlAttribute(name=AccountConstants.A_ATTACHMENT_SIZE_LIMIT) private Long attachmentSizeLimit;
    @XmlAttribute(name=AccountConstants.A_DOCUMENT_SIZE_LIMIT) private Long documentSizeLimit;
    
    @XmlElement(required=true) private String version;
    @XmlElement(required=true, name="id") private String accountId;
    @XmlElement(required=true, name="name") private String accountName;
    @XmlElement private String crumb;
    @XmlElement(required=true) private long lifetime;
    @XmlElement(name=AccountConstants.E_REST) private String restUrl;
    @XmlElement(name=AccountConstants.E_QUOTA_USED) private Long quotaUsed;
    @XmlElement(name=AccountConstants.E_PREVIOUS_SESSION) private Long previousSessionTime;
    @XmlElement(name=AccountConstants.E_LAST_ACCESS) private Long lastWriteAccessTime;
    @XmlElement(name=AccountConstants.E_RECENT_MSGS) private Integer recentMessageCount;
    @XmlElement(name=AccountConstants.E_COS) private Cos cos;

    @XmlElementWrapper(name=AccountConstants.E_PREFS)
    @XmlElement(name=AccountConstants.E_PREF)
    private List<Pref> prefs = new ArrayList<Pref>();

    @XmlElementWrapper(name=AccountConstants.E_ATTRS)
    @XmlElement(name=AccountConstants.E_ATTR)
    private List<Attr> attrs = new ArrayList<Attr>();
    
    @XmlElement(name=AccountConstants.E_SOAP_URL) private List<String> soapURLs = new ArrayList<String>();
    @XmlElement private String publicURL;
    
    @XmlElementWrapper(name=AccountConstants.E_IDENTITIES)
    @XmlElement(name=AccountConstants.E_IDENTITY)
    private List<Identity> identities = new ArrayList<Identity>();
    
    @XmlElementWrapper(name=AccountConstants.E_SIGNATURES)
    @XmlElement(name=AccountConstants.E_SIGNATURE)
    private List<Signature> signatures = new ArrayList<Signature>();

    @XmlElementWrapper(name=AccountConstants.E_DATA_SOURCES)
    @XmlElements({
        @XmlElement(name=MailConstants.E_DS_POP3, type=AccountPop3DataSource.class),
        @XmlElement(name=MailConstants.E_DS_IMAP, type=AccountImapDataSource.class),
        @XmlElement(name=MailConstants.E_DS_RSS, type=AccountRssDataSource.class),
        @XmlElement(name=MailConstants.E_DS_CAL, type=AccountCalDataSource.class)
    })
    private List<AccountDataSource> dataSources = new ArrayList<AccountDataSource>();
    
    @XmlElement(name=AccountConstants.E_CHANGE_PASSWORD_URL) private String changePasswordURL;
    
    public Long getAttachmentSizeLimit() { return attachmentSizeLimit; }
    public Long getDocumentSizeLimit() { return documentSizeLimit; }
    public String getVersion() { return version; }
    public String getAccountId() { return accountId; }
    public String getAccountName() { return accountName; }
    public String getCrumb() { return crumb; }
    public long getLifetime() { return lifetime; }
    public String getRestUrl() { return restUrl; }
    public Long getQuotaUsed() { return quotaUsed; }
    public Long getPreviousSessionTime() { return previousSessionTime; }
    public Long getLastWriteAccessTime() { return lastWriteAccessTime; }
    public Integer getRecentMessageCount() { return recentMessageCount; }
    public Cos getCos() { return cos; }
    public List<Pref> getPrefs() { return Collections.unmodifiableList(prefs); }
    public List<Attr> getAttrs() { return Collections.unmodifiableList(attrs); }
    public List<String> getSoapURLs() { return soapURLs; }
    public String getPublicURL() { return publicURL; }
    public List<Identity> getIdentities() { return Collections.unmodifiableList(identities); }
    public List<Signature> getSignatures() { return Collections.unmodifiableList(signatures); }
    public String getChangePasswordURL() { return changePasswordURL; }
    public List<AccountDataSource> getDataSources() { return Collections.unmodifiableList(dataSources); }

    public GetInfoResponse setAttachmentSizeLimit(Long limit) { attachmentSizeLimit = limit; return this; }
    public GetInfoResponse setDocumentSizeLimit(Long limit) { documentSizeLimit = limit; return this; }
    public GetInfoResponse setVersion(String version) { this.version = version; return this; }
    public GetInfoResponse setAccountId(String id) { accountId = id; return this; }
    public GetInfoResponse setAccountName(String name) { accountName = name; return this; }
    public GetInfoResponse setCrumb(String crumb) { this.crumb = crumb; return this; }
    public GetInfoResponse setLifetime(long lifetime) { this.lifetime = lifetime; return this; }
    public GetInfoResponse setRestUrl(String url) { restUrl = url; return this; }
    public GetInfoResponse setQuotaUsed(Long quotaUsed) { this.quotaUsed = quotaUsed; return this; }
    public GetInfoResponse setPreviousSessionTime(Long time) { previousSessionTime = time; return this; }
    public GetInfoResponse setLastWriteAccessTime(Long time) { lastWriteAccessTime = time; return this; }
    public GetInfoResponse setRecentMessageCount(Integer count) { recentMessageCount = count; return this; }
    public GetInfoResponse setCos(Cos cos) { this.cos = cos; return this; }
    public GetInfoResponse setPublicURL(String url) { publicURL = url; return this; }
    public GetInfoResponse setChangePasswordURL(String url) { changePasswordURL = url; return this; }
    
    public GetInfoResponse setSoapURLs(Iterable<String> urls) {
        soapURLs.clear();
        if (urls != null) {
            Iterables.addAll(soapURLs, urls);
        }
        return this;
    }
    
    public GetInfoResponse setPrefs(Iterable<Pref> prefs) {
        this.prefs.clear();
        if (prefs != null) {
            Iterables.addAll(this.prefs, prefs);
        }
        return this;
    }
    
    public GetInfoResponse addPref(Pref pref) {
        prefs.add(pref);
        return this;
    }
    
    public GetInfoResponse setAttrs(Iterable<Attr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs, attrs);
        }
        return this;
    }
    
    public GetInfoResponse setIdentities(Iterable<Identity> identities) {
        this.identities.clear();
        if (identities != null) {
            Iterables.addAll(this.identities, identities);
        }
        return this;
    }

    public GetInfoResponse setSignatures(Iterable<Signature> signatures) {
        this.signatures.clear();
        if (signatures != null) {
            Iterables.addAll(this.signatures, signatures);
        }
        return this;
    }
    
    public GetInfoResponse setDataSources(Iterable<AccountDataSource> dataSources) {
        this.dataSources.clear();
        if (dataSources != null) {
            Iterables.addAll(this.dataSources, dataSources);
        }
        return this;
    }
    
    public GetInfoResponse addAttr(Attr attr) {
        attrs.add(attr);
        return this;
    }
    
    public Multimap<String, String> getPrefsMultimap() {
        return Pref.toMultimap(prefs);
    }
    
    public Multimap<String, String> getAttrsMultimap() {
        return Attr.toMultimap(attrs);
    }
    
    // TODO: zimlets, etc.
}
