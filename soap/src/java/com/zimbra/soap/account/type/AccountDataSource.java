/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"lastError", "attributes"})
@XmlRootElement
@GraphQLType(name=GqlConstants.CLASS_ACCOUNT_DATA_SOURCE, description="Account data source")
public class AccountDataSource implements DataSource {

    /**
     * @zm-api-field-tag data-source-id
     * @zm-api-field-description Unique ID for data source
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag data-source-name
     * @zm-api-field-description Name for data source
     * <br />
     * e.g. "My IMAP Account"
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag data-source-folder-id
     * @zm-api-field-description Folder ID for data source
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag data-source-is-enabled
     * @zm-api-field-description Flag whether or not the data source is enabled
     */
    @XmlAttribute(name=MailConstants.A_DS_IS_ENABLED /* isEnabled */, required=false)
    private ZmBoolean enabled;

    /**
     * @zm-api-field-tag import-only
     * @zm-api-field-description indicates that this datasource is used for one way (incoming) import versus
     * two-way sync
     */
    @XmlAttribute(name=MailConstants.A_DS_IS_IMPORTONLY /* importOnly */, required=false)
    private ZmBoolean importOnly;

    /**
     * @zm-api-field-tag data-source-server
     * @zm-api-field-description Name of server
     * <br />
     * e.g. "imap.myisp.com"
     */
    @XmlAttribute(name=MailConstants.A_DS_HOST /* host */, required=false)
    private String host;

    /**
     * @zm-api-field-tag data-source-port
     * @zm-api-field-description Port number of server
     * <br />
     * e.g. "143"
     */
    @XmlAttribute(name=MailConstants.A_DS_PORT /* port */, required=false)
    private Integer port;

    /**
     * @zm-api-field-description Which security layer to use for connection (cleartext, ssl, tls, or tls if available).
     * If not set on data source, fallback to the value on global config.
     */
    @XmlAttribute(name=MailConstants.A_DS_CONNECTION_TYPE /* connectionType */, required=false)
    private AdsConnectionType adsConnectionType;

    /**
     * @zm-api-field-description Login string on <b>{data-source-server}</b>, for example a user name
     */
    @XmlAttribute(name=MailConstants.A_DS_USERNAME /* username */, required=false)
    private String username;

    /**
     * @zm-api-field-tag data-source-password
     * @zm-api-field-description Login password for data source
     */
    @XmlAttribute(name=MailConstants.A_DS_PASSWORD /* password */, required=false)
    private String password;

    /**
     * @zm-api-field-description Polling interval.  For instance <b>"10m"</b>
     * <br />
     * The time interval between automated data imports for a data source. If unset or 0, the data source will not be
     * scheduled for automated polling. Must be in valid duration format:
     * <br />
     *     {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - * minutes, s - seconds, d - days,
     *     ms - milliseconds.
     * <br />
     * If time unit is not specified, the default is s(seconds).
     */
    @XmlAttribute(name=MailConstants.A_DS_POLLING_INTERVAL /* pollingInterval */, required=false)
    private String pollingInterval;

    /**
     * @zm-api-field-tag data-source-email-address
     * @zm-api-field-description Email address for the data-source
     */
    @XmlAttribute(name=MailConstants.A_DS_EMAIL_ADDRESS /* emailAddress */, required=false)
    private String emailAddress;

    /**
     * @zm-api-field-description When forwarding or replying to messages sent to this data source, this flags whether
     * or not to use the email address of the data source for the from address and the designated signature/replyTo
     * of the data source for the outgoing message.
     */
    @XmlAttribute(name=MailConstants.A_DS_USE_ADDRESS_FOR_FORWARD_REPLY /* useAddressForForwardReply */, required=false)
    private ZmBoolean useAddressForForwardReply;

    /**
     * @zm-api-field-tag default-signature-id
     * @zm-api-field-description ID for default signature
     */
    @XmlAttribute(name=MailConstants.A_DS_DEFAULT_SIGNATURE /* defaultSignature */, required=false)
    private String defaultSignature;

    /**
     * @zm-api-field-tag forward-reply-signature-id
     * @zm-api-field-description Forward / Reply Signature ID for data source
     */
    @XmlAttribute(name=MailConstants.A_DS_FORWARD_REPLY_SIGNATURE /* forwardReplySignature */, required=false)
    private String forwardReplySignature;

    /**
     * @zm-api-field-tag data-source-from-display
     * @zm-api-field-description Personal part of email address to put in the from header
     */
    @XmlAttribute(name=MailConstants.A_DS_FROM_DISPLAY /* fromDisplay */, required=false)
    private String fromDisplay;

    /**
     * @zm-api-field-tag data-source-replyto-address
     * @zm-api-field-description Email address to put in the reply-to header
     */
    @XmlAttribute(name=MailConstants.A_DS_REPLYTO_ADDRESS /* replyToAddress */, required=false)
    private String replyToAddress;

    /**
     * @zm-api-field-tag data-source-replyto-display
     * @zm-api-field-description Personal part of Email address to put in the reply-to header
     */
    @XmlAttribute(name=MailConstants.A_DS_REPLYTO_DISPLAY /* replyToDisplay */, required=false)
    private String replyToDisplay;

    /**
     * @zm-api-field-tag data-import-class
     * @zm-api-field-description Data import class used bt this data source
     */
    @XmlAttribute(name=MailConstants.A_DS_IMPORT_CLASS /* importClass */, required=false)
    private String importClass;

    /**
     * @zm-api-field-tag data-source-failing-since
     * @zm-api-field-description Failing Since
     */
    @XmlAttribute(name=MailConstants.A_DS_FAILING_SINCE /* failingSince */, required=false)
    private Long failingSince;

    /**
     * @zm-api-field-tag data-source-last-error
     * @zm-api-field-description Last Error
     */
    @XmlElement(name=MailConstants.E_DS_LAST_ERROR /* lastError */, required=false)
    private String lastError;

    /**
     * @zm-api-field-tag data-source-attrs
     * @zm-api-field-description Properties for the data source
     */
    @XmlElement(name=MailConstants.E_ATTRIBUTE /* a */, required=false)
    private List<String> attributes = Lists.newArrayList();

    /**
     * @zm-api-field-tag data-source-refreshToken
     * @zm-api-field-description refresh token for refreshing data source oauth token
     */
    @XmlAttribute(name = MailConstants.A_DS_REFRESH_TOKEN /* refreshToken */, required = false)
    private String refreshToken;

    /**
     * @zm-api-field-tag data-source-refreshTokenUrl
     * @zm-api-field-description refreshTokenUrl for refreshing data source oauth token
     */
    @XmlAttribute(name = MailConstants.A_DS_REFRESH_TOKEN_URL /* refreshTokenUrl */, required = false)
    private String refreshTokenUrl;

    public AccountDataSource() {
    }

    public AccountDataSource(DataSource from) {
        copy(from);
    }

    @Override
    public void copy(DataSource from) {
        id = from.getId();
        name = from.getName();
        folderId = from.getFolderId();
        enabled = ZmBoolean.fromBool(from.isEnabled());
        importOnly = ZmBoolean.fromBool(from.isImportOnly());
        host = from.getHost();
        port = from.getPort();
        adsConnectionType = AdsConnectionType.CT_TO_ACT.apply(
                from.getConnectionType());
        username = from.getUsername();
        password = from.getPassword();
        pollingInterval = from.getPollingInterval();
        emailAddress = from.getEmailAddress();
        useAddressForForwardReply = ZmBoolean.fromBool(from.isUseAddressForForwardReply());
        defaultSignature = from.getDefaultSignature();
        forwardReplySignature = from.getForwardReplySignature();
        fromDisplay = from.getFromDisplay();
        replyToAddress = from.getReplyToAddress();
        replyToDisplay = from.getReplyToDisplay();
        importClass = from.getImportClass();
        failingSince = from.getFailingSince();
        lastError = from.getLastError();
        refreshToken = from.getRefreshToken();
        refreshTokenUrl = from.getRefreshTokenUrl();
        setAttributes(from.getAttributes());
    }

    @Override
    public void setId(String id) { this.id = id; }
    @Override
    public void setName(String name) { this.name = name; }
    @Override
    public void setFolderId(String folderId) { this.folderId = folderId; }
    @Override
    public void setEnabled(Boolean enabled) { this.enabled = ZmBoolean.fromBool(enabled); }
    @Override
    public void setImportOnly(Boolean importOnly) { this.importOnly = ZmBoolean.fromBool(importOnly); }
    @Override
    public void setHost(String host) { this.host = host; }
    @Override
    public void setPort(Integer port) { this.port = port; }
    public void setAdsConnectionType(AdsConnectionType adsConnectionType) {
        this.adsConnectionType = adsConnectionType;
    }
    @Override
    public void setUsername(String username) { this.username = username; }
    @Override
    public void setPassword(String password) { this.password = password; }
    @Override
    public void setPollingInterval(String pollingInterval) { this.pollingInterval = pollingInterval; }
    @Override
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    @Override
    public void setUseAddressForForwardReply(Boolean useAddressForForwardReply) {
        this.useAddressForForwardReply = ZmBoolean.fromBool(useAddressForForwardReply);
    }
    @Override
    public void setDefaultSignature(String defaultSignature) { this.defaultSignature = defaultSignature; }
    @Override
    public void setForwardReplySignature(String forwardReplySignature) { this.forwardReplySignature = forwardReplySignature; }
    @Override
    public void setFromDisplay(String fromDisplay) { this.fromDisplay = fromDisplay; }
    @Override
    public void setReplyToAddress(String replyToAddress) { this.replyToAddress = replyToAddress; }
    @Override
    public void setReplyToDisplay(String replyToDisplay) { this.replyToDisplay = replyToDisplay; }
    @Override
    public void setImportClass(String importClass) { this.importClass = importClass; }
    @Override
    public void setFailingSince(Long failingSince) { this.failingSince = failingSince; }
    @Override
    public void setLastError(String lastError) { this.lastError = lastError; }
    @Override
    public void setAttributes(Iterable <String> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            Iterables.addAll(this.attributes,attributes);
        }
    }

    @Override
    public void addAttribute(String attribute) {
        this.attributes.add(attribute);
    }

    @Override
    @GraphQLQuery(name=GqlConstants.ID, description="Unique ID for data source")
    public String getId() { return id; }
    @Override
    @GraphQLQuery(name=GqlConstants.NAME, description="Name for data source")
    public String getName() { return name; }
    @Override
    @GraphQLQuery(name=GqlConstants.FOLDER_ID, description="Folder ID for data source")
    public String getFolderId() { return folderId; }
    @Override
    @GraphQLQuery(name=GqlConstants.ENABLED, description="Flag whether or not the data source is enabled")
    public Boolean isEnabled() { return ZmBoolean.toBool(enabled); }
    @Override
    @GraphQLQuery(name=GqlConstants.IMPORT_ONLY,
        description="indicates that this datasource is used for one way (incoming) import versus")
    public Boolean isImportOnly() { return ZmBoolean.toBool(importOnly); }
    @Override
    @GraphQLQuery(name=GqlConstants.HOST, description="Name of server")
    public String getHost() { return host; }
    @Override
    @GraphQLQuery(name=GqlConstants.PORT, description="Port number of server")
    public Integer getPort() { return port; }
    @GraphQLQuery(name=GqlConstants.ADS_CONNECTION_TYPE,
            description="Which security layer to use for connection (cleartext, ssl, tls, or tls if available)."
                    + " If not set on data source, fallback to the value on global config.")
    public AdsConnectionType getAdsConnectionType() { return adsConnectionType; }
    @Override
    @GraphQLQuery(name=GqlConstants.USERNAME, description="Login string on {data-source-server}, for example a user name")
    public String getUsername() { return username; }
    @Override
    @GraphQLQuery(name=GqlConstants.PASSWORD, description="Login password for data source")
    public String getPassword() { return password; }
    @Override
    @GraphQLQuery(name=GqlConstants.POLLING_INTERVAL, description="The time interval between automated data imports for a data source."
            + " If unset or 0, the data source will not be scheduled for automated polling. Must be in valid duration format:\n"
            + "{digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - * minutes, s - seconds, d - days, ms - milliseconds.\n"
            + "If time unit is not specified, the default is s(seconds).")
    public String getPollingInterval() { return pollingInterval; }
    @Override
    @GraphQLQuery(name=GqlConstants.EMAIL_ADDRESS, description="Email address for the data-source")
    public String getEmailAddress() { return emailAddress; }
    @Override
    @GraphQLQuery(name=GqlConstants.USE_ADDRESS_FOR_FORWARD_REPLY, description="When forwarding or replying to messages sent to this data source,"
            + " this flags whether or not to use the email address of the data source for the from address and the designated signature/replyTo"
            + " of the data source for the outgoing message.")
    public Boolean isUseAddressForForwardReply() { return ZmBoolean.toBool(useAddressForForwardReply); }
    @Override
    @GraphQLQuery(name=GqlConstants.DEFAULT_SIGNATURE, description="ID for default signature")
    public String getDefaultSignature() { return defaultSignature; }
    @Override
    @GraphQLQuery(name=GqlConstants.FORWARD_REPLY_SIGNATURE, description="Forward / Reply Signature ID for data source")
    public String getForwardReplySignature() { return forwardReplySignature; }
    @Override
    @GraphQLQuery(name=GqlConstants.FROM_DISPLAY, description="Personal part of email address to put in the from header")
    public String getFromDisplay() { return fromDisplay; }
    @Override
    @GraphQLQuery(name=GqlConstants.REPLY_TO_ADDRESS, description="Email address to put in the reply-to header")
    public String getReplyToAddress() { return replyToAddress; }
    @Override
    @GraphQLQuery(name=GqlConstants.REPLY_TO_DISPLAY, description="Personal part of Email address to put in the reply-to header")
    public String getReplyToDisplay() { return replyToDisplay; }
    @Override
    @GraphQLQuery(name=GqlConstants.IMPORT_CLASS, description="Data import class used bt this data source")
    public String getImportClass() { return importClass; }
    @Override
    @GraphQLQuery(name=GqlConstants.FAILING_SINCE, description="Failing since")
    public Long getFailingSince() { return failingSince; }
    @Override
    @GraphQLQuery(name=GqlConstants.LAST_ERROR, description="Last error")
    public String getLastError() { return lastError; }
    @Override
    @GraphQLQuery(name=GqlConstants.ATTRIBUTES, description="Properties for the data source")
    public List<String> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }
    @Override
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    @Override
    @GraphQLQuery(name=GqlConstants.REFRESH_TOKEN, description="refresh token for refreshing data source oauth token")
    public String getRefreshToken() { return refreshToken; }
    @Override
    public void setRefreshTokenUrl(String refreshTokenUrl) { this.refreshTokenUrl = refreshTokenUrl; }
    @Override
    @GraphQLQuery(name=GqlConstants.REFRESH_TOKEN, description="refresh token for refreshing data source oauth token")
    public String getRefreshTokenUrl() { return refreshTokenUrl; }
    @Override
    @GraphQLIgnore
    public ConnectionType getConnectionType() {
        return AdsConnectionType.ACT_TO_CT.apply(adsConnectionType);
    }
    @Override
    public void setConnectionType(ConnectionType connectionType) {
        this.adsConnectionType = AdsConnectionType.CT_TO_ACT.apply(connectionType);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("name", name)
            .add("folderId", folderId)
            .add("enabled", enabled)
            .add("importOnly", importOnly)
            .add("host", host)
            .add("port", port)
            .add("mdsConnectionType", adsConnectionType)
            .add("username", username)
            .add("password", password)
            .add("pollingInterval", pollingInterval)
            .add("emailAddress", emailAddress)
            .add("useAddressForForwardReply", useAddressForForwardReply)
            .add("defaultSignature", defaultSignature)
            .add("forwardReplySignature", forwardReplySignature)
            .add("fromDisplay", fromDisplay)
            .add("replyToAddress", replyToAddress)
            .add("replyToDisplay", replyToDisplay)
            .add("importClass", importClass)
            .add("failingSince", failingSince)
            .add("lastError", lastError)
            .add("refreshToken", refreshToken)
            .add("refreshTokenUrl", refreshTokenUrl)
            .add("attributes", attributes);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
