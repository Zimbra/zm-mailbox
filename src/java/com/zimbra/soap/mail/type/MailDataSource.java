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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"lastError", "attributes"})
abstract public class MailDataSource implements DataSource {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_DS_IS_ENABLED /* isEnabled */, required=false)
    private ZmBoolean enabled;

    @XmlAttribute(name=MailConstants.A_DS_IS_IMPORTONLY /* importOnly */, required=false)
    private ZmBoolean importOnly;

    @XmlAttribute(name=MailConstants.A_DS_HOST /* host */, required=false)
    private String host;

    @XmlAttribute(name=MailConstants.A_DS_PORT /* port */, required=false)
    private Integer port;

    @XmlAttribute(name=MailConstants.A_DS_CONNECTION_TYPE /* connectionType */, required=false)
    private MdsConnectionType mdsConnectionType;

    @XmlAttribute(name=MailConstants.A_DS_USERNAME /* username */, required=false)
    private String username;

    @XmlAttribute(name=MailConstants.A_DS_PASSWORD /* password */, required=false)
    private String password;

    @XmlAttribute(name=MailConstants.A_DS_POLLING_INTERVAL /* pollingInterval */, required=false)
    private String pollingInterval;

    @XmlAttribute(name=MailConstants.A_DS_EMAIL_ADDRESS /* emailAddress */, required=false)
    private String emailAddress;

    @XmlAttribute(name=MailConstants.A_DS_USE_ADDRESS_FOR_FORWARD_REPLY
                    /* useAddressForForwardReply */, required=false)
    private ZmBoolean useAddressForForwardReply;

    @XmlAttribute(name=MailConstants.A_DS_DEFAULT_SIGNATURE
                    /* defaultSignature */, required=false)
    private String defaultSignature;

    @XmlAttribute(name=MailConstants.A_DS_FORWARD_REPLY_SIGNATURE
                    /* forwardReplySignature */, required=false)
    private String forwardReplySignature;

    @XmlAttribute(name=MailConstants.A_DS_FROM_DISPLAY /* fromDisplay */,
                    required=false)
    private String fromDisplay;

    @XmlAttribute(name=MailConstants.A_DS_FROM_ADDRESS /* fromAddress */,
                    required=false)
    private String fromAddress;

    @XmlAttribute(name=MailConstants.A_DS_REPLYTO_ADDRESS /* replyToAddress */,
                    required=false)
    private String replyToAddress;

    @XmlAttribute(name=MailConstants.A_DS_REPLYTO_DISPLAY /* replyToDisplay */,
                    required=false)
    private String replyToDisplay;

    @XmlAttribute(name=MailConstants.A_DS_IMPORT_CLASS /* importClass */,
                    required=false)
    private String importClass;

    @XmlAttribute(name=MailConstants.A_DS_FAILING_SINCE /* failingSince */,
                    required=false)
    private Long failingSince;

    @XmlElement(name=MailConstants.E_DS_LAST_ERROR /* lastError */,
                    required=false)
    private String lastError;

    @XmlElement(name=MailConstants.E_ATTRIBUTE /* a */, required=false)
    private List<String> attributes = Lists.newArrayList();

    public MailDataSource() {
    }

    public MailDataSource(DataSource from) {
        copy(from);
    }

    public void copy(DataSource from) {
        id = from.getId();
        name = from.getName();
        folderId = from.getFolderId();
        setEnabled(from.isEnabled());
        setImportOnly(from.isImportOnly());
        host = from.getHost();
        port = from.getPort();
        mdsConnectionType = MdsConnectionType.CT_TO_MCT.apply(
                from.getConnectionType());
        username = from.getUsername();
        password = from.getPassword();
        pollingInterval = from.getPollingInterval();
        emailAddress = from.getEmailAddress();
        setUseAddressForForwardReply(from.isUseAddressForForwardReply());
        defaultSignature = from.getDefaultSignature();
        forwardReplySignature = from.getForwardReplySignature();
        fromDisplay = from.getFromDisplay();
        fromAddress = from.getFromAddress();
        replyToAddress = from.getReplyToAddress();
        replyToDisplay = from.getReplyToDisplay();
        importClass = from.getImportClass();
        failingSince = from.getFailingSince();
        lastError = from.getLastError();
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
    public void setMdsConnectionType(MdsConnectionType mdsConnectionType) {
        this.mdsConnectionType = mdsConnectionType;
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
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
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
    public String getId() { return id; }
    @Override
    public String getName() { return name; }
    @Override
    public String getFolderId() { return folderId; }
    @Override
    public Boolean isEnabled() { return ZmBoolean.toBool(enabled); }
    @Override
    public Boolean isImportOnly() { return ZmBoolean.toBool(importOnly); }
    @Override
    public String getHost() { return host; }
    @Override
    public Integer getPort() { return port; }
    public MdsConnectionType getMdsConnectionType() { return mdsConnectionType; }
    @Override
    public String getUsername() { return username; }
    @Override
    public String getPassword() { return password; }
    @Override
    public String getPollingInterval() { return pollingInterval; }
    @Override
    public String getEmailAddress() { return emailAddress; }
    @Override
    public Boolean isUseAddressForForwardReply() { return ZmBoolean.toBool(useAddressForForwardReply); }
    @Override
    public String getDefaultSignature() { return defaultSignature; }
    @Override
    public String getForwardReplySignature() { return forwardReplySignature; }
    @Override
    public String getFromDisplay() { return fromDisplay; }
    @Override
    public String getFromAddress() { return fromAddress; }
    @Override
    public String getReplyToAddress() { return replyToAddress; }
    @Override
    public String getReplyToDisplay() { return replyToDisplay; }
    @Override
    public String getImportClass() { return importClass; }
    @Override
    public Long getFailingSince() { return failingSince; }
    @Override
    public String getLastError() { return lastError; }
    @Override
    public List<String> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    @Override
    public ConnectionType getConnectionType() {
        return MdsConnectionType.MCT_TO_CT.apply(mdsConnectionType);
    }

    @Override
    public void setConnectionType(ConnectionType connectionType) {
        this.mdsConnectionType = MdsConnectionType.CT_TO_MCT.apply(connectionType);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("name", name)
            .add("folderId", folderId)
            .add("enabled", enabled)
            .add("importOnly", importOnly)
            .add("host", host)
            .add("port", port)
            .add("mdsConnectionType", mdsConnectionType)
            .add("username", username)
            .add("password", password)
            .add("pollingInterval", pollingInterval)
            .add("emailAddress", emailAddress)
            .add("useAddressForForwardReply", useAddressForForwardReply)
            .add("defaultSignature", defaultSignature)
            .add("forwardReplySignature", forwardReplySignature)
            .add("fromDisplay", fromDisplay)
            .add("fromAddress", fromAddress)
            .add("replyToAddress", replyToAddress)
            .add("replyToDisplay", replyToDisplay)
            .add("importClass", importClass)
            .add("failingSince", failingSince)
            .add("lastError", lastError)
            .add("attributes", attributes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
