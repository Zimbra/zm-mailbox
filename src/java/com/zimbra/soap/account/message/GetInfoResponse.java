/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.collect.Multimap;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.AccountCalDataSource;
import com.zimbra.soap.account.type.AccountCaldavDataSource;
import com.zimbra.soap.account.type.AccountDataSource;
import com.zimbra.soap.account.type.AccountGalDataSource;
import com.zimbra.soap.account.type.AccountImapDataSource;
import com.zimbra.soap.account.type.AccountPop3DataSource;
import com.zimbra.soap.account.type.AccountRssDataSource;
import com.zimbra.soap.account.type.AccountUnknownDataSource;
import com.zimbra.soap.account.type.AccountYabDataSource;
import com.zimbra.soap.account.type.AccountZimletInfo;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.ChildAccount;
import com.zimbra.soap.account.type.Cos;
import com.zimbra.soap.account.type.DiscoverRightsInfo;
import com.zimbra.soap.account.type.Identity;
import com.zimbra.soap.account.type.LicenseInfo;
import com.zimbra.soap.account.type.Pref;
import com.zimbra.soap.account.type.Prop;
import com.zimbra.soap.account.type.Signature;
import com.zimbra.soap.type.ZmBoolean;

import com.zimbra.soap.json.jackson.StringListSerializer;
import com.zimbra.soap.json.jackson.WrappedAttrListSerializer;

/**
 * Note that LicenseAdminService and LicenseService both register a handler (the same one) which
 * extends com.zimbra.cs.service.account.GetInfo - this adds the "license" element
 *
<GetInfoResponse>
   <version>{version}</version>
   <id>{account-id}</id>
   <name>{account-name}</name>
   <lifetime>...</lifetime>
   [<adminDelegated>{admin-delegated}</adminDelegated>]
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
   <rights>
     {discovered-rights}
   </right>*
   [<license status="inGracePeriod|bad"/>]
</GetInfoResponse>
 *
 * @zm-api-response-description The response to a request for account information
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_INFO_RESPONSE)
@XmlType(propOrder = {"version", "accountId", "accountName", "crumb",
        "lifetime", "adminDelegated", "restUrl", "quotaUsed",
        "previousSessionTime", "lastWriteAccessTime", "recentMessageCount",
        "cos", "prefs", "attrs", "zimlets", "props", "identities",
        "signatures", "dataSources", "childAccounts", "discoveredRights",
        "soapURLs", "publicURL", "changePasswordURL", "license"})
public final class GetInfoResponse {

    /**
     * @zm-api-field-tag att-size-limit
     * @zm-api-field-description The size limit for attachments
     */
    @XmlAttribute(name=AccountConstants.A_ATTACHMENT_SIZE_LIMIT /* attSizeLimit */, required=false)
    private Long attachmentSizeLimit;

    /**
     * @zm-api-field-tag doc-size-limit
     * @zm-api-field-description The size limit for documents
     */
    @XmlAttribute(name=AccountConstants.A_DOCUMENT_SIZE_LIMIT /* docSizeLimit */, required=false)
    private Long documentSizeLimit;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-description Server version:
     *     &lt;major>[.&lt;minor>[.&lt;maintenance>]][build] &lt;release> &lt;date>[&lt;type>]
     */
    @XmlElement(name=AccountConstants.E_VERSION /* version */, required=true)
    private String version;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlElement(name=AccountConstants.E_ID /* id */, required=true)
    private String accountId;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Email address (user@domain)
     */
    @XmlElement(name=AccountConstants.E_NAME /* name */, required=true)
    private String accountName;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-description Crumb
     */
    @XmlElement(name=AccountConstants.E_CRUMB /* crumb */, required=false)
    private String crumb;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag lifetime
     * @zm-api-field-description Number of milliseconds until auth token expires
     */
    @XmlElement(name=AccountConstants.E_LIFETIME /* lifetime */, required=true)
    private long lifetime;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag admin-delegated
     * @zm-api-field-description 1 (true) if the auth token is a delegated auth token issued to an admin account
     */
    @XmlElement(name=AccountConstants.E_ADMIN_DELEGATED /* adminDelegated */, required=false)
    private ZmBoolean adminDelegated;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag account-base-REST-url
     * @zm-api-field-description Base REST URL for the requested account
     */
    @XmlElement(name=AccountConstants.E_REST /* rest */, required=false)
    private String restUrl;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag quota-used
     * @zm-api-field-description Mailbox quota used in bytes.
     * <br />Returned only if the command successfully executes on the target user's home mail server
     */
    @XmlElement(name=AccountConstants.E_QUOTA_USED /* used */, required=false)
    private Long quotaUsed;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag previous-SOAP-session-time
     * @zm-api-field-description Time (in millis) of last write op from this session, or from *any* SOAP session if we
     * don't have one
     * <br />Returned only if the command successfully executes on the target user's home mail server
     */
    @XmlElement(name=AccountConstants.E_PREVIOUS_SESSION /* prevSession */, required=false)
    private Long previousSessionTime;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag last-SOAP-write-access-time
     * @zm-api-field-description Time (in millis) of last write op from any SOAP session before this session was
     * initiated, or same as {previous-SOAP-session-time} if we don't have one.
     * <br />Returned only if the command successfully executes on the target user's home mail server
     */
    @XmlElement(name=AccountConstants.E_LAST_ACCESS /* accessed */, required=false)
    private Long lastWriteAccessTime;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag recent-message-count
     * @zm-api-field-description Number of messages received since the previous soap session, or since the last SOAP
     * write op if we don't have a session.
     * <br />Returned only if the command successfully executes on the target user's home mail server
     */
    @XmlElement(name=AccountConstants.E_RECENT_MSGS /* recent */, required=false)
    private Integer recentMessageCount;

    /**
     * @zm-api-field-description Class of service
     */
    @XmlElement(name=AccountConstants.E_COS /* cos */, required=false)
    private Cos cos;

    /**
     * @zm-api-field-description User-settable preferences
     */
    @XmlElementWrapper(name=AccountConstants.E_PREFS /* prefs */, required=false)
    @XmlElement(name=AccountConstants.E_PREF /* pref */, required=false)
    @JsonSerialize(using=WrappedAttrListSerializer.class)
    private List<Pref> prefs = Lists.newArrayList();

    /**
     * @zm-api-field-description Account attributes that aren't user-settable, but the front-end needs.
     * Only attributes listed in <b>zimbraAccountClientAttrs</b> will be returned.
     */
    @XmlElementWrapper(name=AccountConstants.E_ATTRS /* attrs */, required=false)
    @XmlElement(name=AccountConstants.E_ATTR /* attr */, required=false)
    @JsonSerialize(using=WrappedAttrListSerializer.class)
    private List<Attr> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-description Zimlets
     */
    @XmlElementWrapper(name=AccountConstants.E_ZIMLETS /* zimlets */, required=false)
    @XmlElement(name=AccountConstants.E_ZIMLET /* zimlet */, required=false)
    private List<AccountZimletInfo> zimlets = Lists.newArrayList();

    /**
     * @zm-api-field-description Properties
     */
    @XmlElementWrapper(name=AccountConstants.E_PROPERTIES /* props */, required=false)
    @XmlElement(name=AccountConstants.E_PROPERTY /* prop */, required=false)
    private List<Prop> props = Lists.newArrayList();

    /**
     * @zm-api-field-description Identities
     */
    @XmlElementWrapper(name=AccountConstants.E_IDENTITIES /* identities */, required=false)
    @XmlElement(name=AccountConstants.E_IDENTITY /* identity */, required=false)
    private List<Identity> identities = Lists.newArrayList();

    /**
     * @zm-api-field-description Signatures
     */
    @XmlElementWrapper(name=AccountConstants.E_SIGNATURES /* signatures */, required=false)
    @XmlElement(name=AccountConstants.E_SIGNATURE /* signature */, required=false)
    private List<Signature> signatures = Lists.newArrayList();

    /**
     * @zm-api-field-description Data sources
     */
    @XmlElementWrapper(name=AccountConstants.E_DATA_SOURCES /* dataSources */, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_DS_IMAP /* imap */, type=AccountImapDataSource.class),
        @XmlElement(name=MailConstants.E_DS_POP3 /* pop3 */, type=AccountPop3DataSource.class),
        @XmlElement(name=MailConstants.E_DS_CALDAV /* caldav */, type=AccountCaldavDataSource.class),
        @XmlElement(name=MailConstants.E_DS_YAB /* yab */, type=AccountYabDataSource.class),
        @XmlElement(name=MailConstants.E_DS_RSS /* rss */, type=AccountRssDataSource.class),
        @XmlElement(name=MailConstants.E_DS_GAL /* gal */, type=AccountGalDataSource.class),
        @XmlElement(name=MailConstants.E_DS_CAL /* cal */, type=AccountCalDataSource.class),
        @XmlElement(name=MailConstants.E_DS_UNKNOWN /* unknown */, type=AccountUnknownDataSource.class)
    })
    private List<AccountDataSource> dataSources = Lists.newArrayList();

    /**
     * @zm-api-field-description Child accounts
     */
    @XmlElementWrapper(name=AccountConstants.E_CHILD_ACCOUNTS /* childAccounts */, required=false)
    @XmlElement(name=AccountConstants.E_CHILD_ACCOUNT /* childAccount */, required=false)
    private List<ChildAccount> childAccounts = Lists.newArrayList();

    /**
     * @zm-api-field-description Discovered Rights - same as for <b>DiscoverRightsResponse</b>
     */
    @XmlElementWrapper(name=AccountConstants.E_RIGHTS /* rights */, required=false)
    @XmlElement(name=AccountConstants.E_TARGETS, required=false)
    private List<DiscoverRightsInfo> discoveredRights = Lists.newArrayList();

    // For JSON treat as Attribute
    /**
     * @zm-api-field-description URL to talk to for soap service for this account. i.e:
     * <br />
     * http://server:7070/service/soap/
     * <br />
     * <br />
     * Multiple URLs can be returned if both http and https (SSL) are enabled. If only one of the two is enabled,
     * then only one URL will be returned.
     */
    @XmlElement(name=AccountConstants.E_SOAP_URL /* soapURL */, required=false)
    @JsonSerialize(using=StringListSerializer.class)
    private List<String> soapURLs = Lists.newArrayList();

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag account-base-public-url
     * @zm-api-field-description Base public URL for the requested account
     */
    @XmlElement(name=AccountConstants.E_PUBLIC_URL /* publicURL */, required=false)
    private String publicURL;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag change-password-url
     * @zm-api-field-description URL to talk to in order to change a password.  Not returned if not configured
     * via domain attribute <b>zimbraChangePasswordURL</b>
     */
    @XmlElement(name=AccountConstants.E_CHANGE_PASSWORD_URL /* changePasswordURL */, required=false)
    private String changePasswordURL;

    /**
     * @zm-api-field-description License information.  Only present for Network Edition
     */
    @XmlElement(name=AccountConstants.E_LICENSE /* license */, required=false)
    private LicenseInfo license;

    public GetInfoResponse() {
    }

    public void setAttachmentSizeLimit(Long attachmentSizeLimit) { this.attachmentSizeLimit = attachmentSizeLimit; }
    public void setDocumentSizeLimit(Long documentSizeLimit) { this.documentSizeLimit = documentSizeLimit; }
    public void setVersion(String version) { this.version = version; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public void setCrumb(String crumb) { this.crumb = crumb; }
    public void setLifetime(long lifetime) { this.lifetime = lifetime; }
    public void setAdminDelegated(Boolean adminDelegated) { this.adminDelegated = ZmBoolean.fromBool(adminDelegated); }
    public void setRestUrl(String restUrl) { this.restUrl = restUrl; }
    public void setQuotaUsed(Long quotaUsed) { this.quotaUsed = quotaUsed; }
    public void setPreviousSessionTime(Long previousSessionTime) { this.previousSessionTime = previousSessionTime; }
    public void setLastWriteAccessTime(Long lastWriteAccessTime) { this.lastWriteAccessTime = lastWriteAccessTime; }
    public void setRecentMessageCount(Integer recentMessageCount) { this.recentMessageCount = recentMessageCount; }
    public void setCos(Cos cos) { this.cos = cos; }
    public void setPrefs(Iterable <Pref> prefs) {
        this.prefs.clear();
        if (prefs != null) {
            Iterables.addAll(this.prefs,prefs);
        }
    }

    public void addPref(Pref pref) {
        this.prefs.add(pref);
    }

    public void setAttrs(Iterable <Attr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public void addAttr(Attr attr) {
        this.attrs.add(attr);
    }

    public void setZimlets(Iterable <AccountZimletInfo> zimlets) {
        this.zimlets.clear();
        if (zimlets != null) {
            Iterables.addAll(this.zimlets,zimlets);
        }
    }

    public void addZimlet(AccountZimletInfo zimlet) {
        this.zimlets.add(zimlet);
    }

    public void setProps(Iterable <Prop> props) {
        this.props.clear();
        if (props != null) {
            Iterables.addAll(this.props,props);
        }
    }

    public void addProp(Prop prop) {
        this.props.add(prop);
    }

    public void setIdentities(Iterable <Identity> identities) {
        this.identities.clear();
        if (identities != null) {
            Iterables.addAll(this.identities,identities);
        }
    }

    public void addIdentity(Identity identity) {
        this.identities.add(identity);
    }

    public void setSignatures(Iterable <Signature> signatures) {
        this.signatures.clear();
        if (signatures != null) {
            Iterables.addAll(this.signatures,signatures);
        }
    }

    public void addSignature(Signature signature) {
        this.signatures.add(signature);
    }

    public void setDataSources(Iterable <AccountDataSource> dataSources) {
        this.dataSources.clear();
        if (dataSources != null) {
            Iterables.addAll(this.dataSources,dataSources);
        }
    }

    public void addDataSource(AccountDataSource dataSource) {
        this.dataSources.add(dataSource);
    }

    public void setChildAccounts(Iterable <ChildAccount> childAccounts) {
        this.childAccounts.clear();
        if (childAccounts != null) {
            Iterables.addAll(this.childAccounts,childAccounts);
        }
    }

    public void addChildAccount(ChildAccount childAccount) {
        this.childAccounts.add(childAccount);
    }

    public void setDiscoveredRights(Iterable<DiscoverRightsInfo> discoveredRights) {
        this.discoveredRights = Lists.newArrayList(discoveredRights);
    }

    public void addDiscoveredRight(DiscoverRightsInfo discoveredRight) {
        this.discoveredRights.add(discoveredRight);
    }

    public void setSoapURLs(Iterable <String> soapURLs) {
        this.soapURLs.clear();
        if (soapURLs != null) {
            Iterables.addAll(this.soapURLs,soapURLs);
        }
    }

    public void addSoapURL(String soapURL) {
        this.soapURLs.add(soapURL);
    }

    public void setPublicURL(String publicURL) { this.publicURL = publicURL; }
    public void setChangePasswordURL(String changePasswordURL) { this.changePasswordURL = changePasswordURL; }
    public void setLicense(LicenseInfo license) { this.license = license; }

    public Long getAttachmentSizeLimit() { return attachmentSizeLimit; }
    public Long getDocumentSizeLimit() { return documentSizeLimit; }
    public String getVersion() { return version; }
    public String getAccountId() { return accountId; }
    public String getAccountName() { return accountName; }
    public String getCrumb() { return crumb; }
    public long getLifetime() { return lifetime; }
    public Boolean getAdminDelegated() { return ZmBoolean.toBool(adminDelegated, Boolean.FALSE); }
    public String getRestUrl() { return restUrl; }
    public Long getQuotaUsed() { return quotaUsed; }
    public Long getPreviousSessionTime() { return previousSessionTime; }
    public Long getLastWriteAccessTime() { return lastWriteAccessTime; }
    public Integer getRecentMessageCount() { return recentMessageCount; }
    public Cos getCos() { return cos; }
    public List<Pref> getPrefs() {
        return Collections.unmodifiableList(prefs);
    }
    public List<Attr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }
    public List<AccountZimletInfo> getZimlets() {
        return Collections.unmodifiableList(zimlets);
    }
    public List<Prop> getProps() {
        return Collections.unmodifiableList(props);
    }
    public List<Identity> getIdentities() {
        return Collections.unmodifiableList(identities);
    }
    public List<Signature> getSignatures() {
        return Collections.unmodifiableList(signatures);
    }
    public List<AccountDataSource> getDataSources() {
        return Collections.unmodifiableList(dataSources);
    }
    public List<ChildAccount> getChildAccounts() {
        return Collections.unmodifiableList(childAccounts);
    }
    public List<DiscoverRightsInfo> getDiscoveredRights() {
        return Collections.unmodifiableList(discoveredRights);
    }
    public List<String> getSoapURLs() {
        return Collections.unmodifiableList(soapURLs);
    }
    public String getPublicURL() { return publicURL; }
    public String getChangePasswordURL() { return changePasswordURL; }
    public LicenseInfo getLicense() { return license; }

    public Multimap<String, String> getPrefsMultimap() {
        return Pref.toMultimap(prefs);
    }

    public Multimap<String, String> getAttrsMultimap() {
        return Attr.toMultimap(attrs);
    }

    public Multimap<String, String> getPropsMultimap(String userPropKey) {
        return Prop.toMultimap(props, userPropKey);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("attachmentSizeLimit", attachmentSizeLimit)
            .add("documentSizeLimit", documentSizeLimit)
            .add("version", version)
            .add("accountId", accountId)
            .add("accountName", accountName)
            .add("crumb", crumb)
            .add("lifetime", lifetime)
            .add("adminDelegated", adminDelegated)
            .add("restUrl", restUrl)
            .add("quotaUsed", quotaUsed)
            .add("previousSessionTime", previousSessionTime)
            .add("lastWriteAccessTime", lastWriteAccessTime)
            .add("recentMessageCount", recentMessageCount)
            .add("cos", cos)
            .add("prefs", prefs)
            .add("attrs", attrs)
            .add("zimlets", zimlets)
            .add("props", props)
            .add("identities", identities)
            .add("signatures", signatures)
            .add("dataSources", dataSources)
            .add("childAccounts", childAccounts)
            .add("soapURLs", soapURLs)
            .add("publicURL", publicURL)
            .add("changePasswordURL", changePasswordURL)
            .add("license", license);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
