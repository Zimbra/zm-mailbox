/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
