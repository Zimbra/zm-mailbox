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

package com.zimbra.soap.account.type;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.DataSource;

@XmlType(propOrder = {})
@XmlRootElement
public class AccountDataSource
implements DataSource {

    @XmlAttribute private String id;
    @XmlAttribute private String name;
    @XmlAttribute private String folderId;
    @XmlAttribute private Boolean enabled = false;
    @XmlAttribute private Boolean importOnly = false;
    @XmlAttribute private String host;
    @XmlAttribute private Integer port;
    @XmlAttribute(name=MailConstants.A_DS_CONNECTION_TYPE) private AdsConnectionType adsConnectionType;
    @XmlAttribute private String username;
    @XmlAttribute private String password;
    @XmlAttribute private String pollingInterval;
    @XmlAttribute private String emailAddress;
    @XmlAttribute private Boolean useAddressForForwardReply;
    @XmlAttribute private String defaultSignature;
    @XmlAttribute private String fromDisplay;
    @XmlAttribute private String fromAddress;
    @XmlAttribute private String replyToAddress;
    @XmlAttribute private String replyToDisplay;
    @XmlAttribute private Long failingSince;
    @XmlElement String lastError;

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
        enabled = from.isEnabled();
        importOnly = from.isImportOnly();
        host = from.getHost();
        port = from.getPort();
        adsConnectionType = AdsConnectionType.CT_TO_ACT.apply(from.getConnectionType());
        username = from.getUsername();
        password = from.getPassword();
        pollingInterval = from.getPollingInterval();
        emailAddress = from.getEmailAddress();
        useAddressForForwardReply = from.isUseAddressForForwardReply();
        defaultSignature = from.getDefaultSignature();
        fromDisplay = from.getFromDisplay();
        fromAddress = from.getFromAddress();
        replyToAddress = from.getReplyToAddress();
        replyToDisplay = from.getReplyToDisplay();
        failingSince = from.getFailingSince();
        lastError = from.getLastError();
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getFolderId() {
        return folderId;
    }
    
    @Override
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
    
    @Override
    public Boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(Boolean isEnabled) {
        this.enabled = isEnabled;
    }
    
    @Override
    public Boolean isImportOnly() {
        return importOnly;
    }
    
    @Override
    public void setImportOnly(Boolean isImportOnly) {
        this.importOnly = isImportOnly;
    }
    
    @Override
    public String getHost() {
        return host;
    }
    
    @Override
    public void setHost(String host) {
        this.host = host;
    }
    
    @Override
    public Integer getPort() {
        return port;
    }
    
    @Override
    public void setPort(Integer port) {
        this.port = port;
    }

    public AdsConnectionType getAdsConnectionType() {
        return adsConnectionType;
    }
    
    public void setAdsConnectionType(AdsConnectionType ct) {
        adsConnectionType = ct;
    }
    
    @Override
    public ConnectionType getConnectionType() {
        return AdsConnectionType.ACT_TO_CT.apply(adsConnectionType);
    }
    
    @Override
    public void setConnectionType(ConnectionType connectionType) {
        this.adsConnectionType = AdsConnectionType.CT_TO_ACT.apply(connectionType);
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public void setUsername(String username) {
        this.username = username;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public String getPollingInterval() {
        return pollingInterval;
    }
    
    @Override
    public void setPollingInterval(String pollingInterval) {
        this.pollingInterval = pollingInterval;
    }
    
    @Override
    public String getEmailAddress() {
        return emailAddress;
    }
    
    @Override
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    @Override
    public Boolean isUseAddressForForwardReply() {
        return useAddressForForwardReply;
    }
    
    @Override
    public void setUseAddressForForwardReply(Boolean useAddressForForwardReply) {
        this.useAddressForForwardReply = useAddressForForwardReply;
    }
    
    @Override
    public String getDefaultSignature() {
        return defaultSignature;
    }
    
    @Override
    public void setDefaultSignature(String defaultSignature) {
        this.defaultSignature = defaultSignature;
    }
    
    @Override
    public String getFromDisplay() {
        return fromDisplay;
    }
    
    @Override
    public void setFromDisplay(String fromDisplay) {
        this.fromDisplay = fromDisplay;
    }
    
    @Override
    public String getFromAddress() {
        return fromAddress;
    }
    
    @Override
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    
    @Override
    public String getReplyToAddress() {
        return replyToAddress;
    }
    
    @Override
    public void setReplyToAddress(String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }
    
    @Override
    public String getReplyToDisplay() {
        return replyToDisplay;
    }
    
    @Override
    public void setReplyToDisplay(String replyToDisplay) {
        this.replyToDisplay = replyToDisplay;
    }
    
    @Override
    public Long getFailingSince() {
        return failingSince;
    }
    
    @Override
    public void setFailingSince(Long failingSince) {
        this.failingSince = failingSince;
    }
    
    @Override
    public String getLastError() {
        return lastError;
    }
    
    @Override
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
