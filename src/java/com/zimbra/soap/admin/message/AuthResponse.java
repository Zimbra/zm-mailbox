/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012 Zimbra, Inc.
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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTH_RESPONSE)
public class AuthResponse {

    /**
     * @zm-api-field-tag auth-token
     * @zm-api-field-description Auth Token
     */
    @XmlElement(name=AdminConstants.E_AUTH_TOKEN /* authToken */, required=true)
    private String authToken;

    /**
     * @zm-api-field-tag auth-lifetime
     * @zm-api-field-description Life time for the authorization
     */
    @XmlElement(name=AdminConstants.E_LIFETIME /* lifetime */, required=true)
    private long lifetime;

    public AuthResponse() {
    }

    public void setAuthToken(String authToken) { this.authToken = authToken; }
    public void setLifetime(long lifetime) { this.lifetime = lifetime; }
    public String getAuthToken() { return authToken; }
    public long getLifetime() { return lifetime; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("authToken", authToken)
            .add("lifetime", lifetime);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
