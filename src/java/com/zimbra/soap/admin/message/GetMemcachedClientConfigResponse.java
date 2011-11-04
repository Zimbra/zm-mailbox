/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_MEMCACHED_CLIENT_CONFIG_RESPONSE)
public class GetMemcachedClientConfigResponse {

    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_SERVER_LIST, required=false)
    private String serverList;

    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_HASH_ALGORITHM, required=false)
    private String hashAlgorithm;

    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_BINARY_PROTOCOL, required=false)
    private ZmBoolean binaryProtocolEnabled;

    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_DEFAULT_EXPIRY_SECONDS, required=false)
    private Integer defaultExpirySeconds;

    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_DEFAULT_TIMEOUT_MILLIS, required=false)
    private Long defaultTimeoutMillis;

    public GetMemcachedClientConfigResponse() {
    }

    public void setServerList(String serverList) {
        this.serverList = serverList;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public void setBinaryProtocolEnabled(Boolean binaryProtocolEnabled) {
        this.binaryProtocolEnabled = ZmBoolean.fromBool(binaryProtocolEnabled);
    }

    public void setDefaultExpirySeconds(Integer defaultExpirySeconds) {
        this.defaultExpirySeconds = defaultExpirySeconds;
    }

    public void setDefaultTimeoutMillis(Long defaultTimeoutMillis) {
        this.defaultTimeoutMillis = defaultTimeoutMillis;
    }

    public String getServerList() { return serverList; }
    public String getHashAlgorithm() { return hashAlgorithm; }
    public Boolean getBinaryProtocolEnabled() { return ZmBoolean.toBool(binaryProtocolEnabled); }
    public Integer getDefaultExpirySeconds() { return defaultExpirySeconds; }
    public Long getDefaultTimeoutMillis() { return defaultTimeoutMillis; }
}
