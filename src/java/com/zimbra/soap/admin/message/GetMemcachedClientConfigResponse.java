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

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_MEMCACHED_CLIENT_CONFIG_RESPONSE)
public class GetMemcachedClientConfigResponse {

    /**
     * @zm-api-field-tag comma-sep-server-list
     * @zm-api-field-description Comma separated list of host:port for memcached servers
     */
    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_SERVER_LIST /* serverList */, required=false)
    private String serverList;

    /**
     * @zm-api-field-tag hash-algorithm
     * @zm-api-field-description KETAMA_HASH, etc.
     */
    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_HASH_ALGORITHM /* hashAlgorithm */, required=false)
    private String hashAlgorithm;

    /**
     * @zm-api-field-tag binary-protocol-enabled
     * @zm-api-field-description Flags whether memcached binary protocol is in use or not
     */
    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_BINARY_PROTOCOL /* binaryProtocol */, required=false)
    private ZmBoolean binaryProtocolEnabled;

    /**
     * @zm-api-field-tag default-expiry-secs
     * @zm-api-field-description Default entry expiry in seconds
     */
    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_DEFAULT_EXPIRY_SECONDS /* defaultExpirySeconds */,
                required=false)
    private Integer defaultExpirySeconds;

    /**
     * @zm-api-field-tag default-timeout-millis
     * @zm-api-field-description Default timeout in milliseconds
     */
    @XmlAttribute(name=AdminConstants.A_MEMCACHED_CLIENT_CONFIG_DEFAULT_TIMEOUT_MILLIS /* defaultTimeoutMillis */,
                required=false)
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
