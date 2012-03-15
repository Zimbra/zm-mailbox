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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DELEGATE_AUTH_RESPONSE)
@XmlType(propOrder = {"authToken", "lifetime"})
public class DelegateAuthResponse {
    /**
     * @zm-api-field-description Auth Token
     */
    @XmlElement(name=AdminConstants.E_AUTH_TOKEN, required=true)
    private String authToken;

    /**
     * @zm-api-field-description Life time for the authorization
     */
    @XmlElement(name=AdminConstants.E_LIFETIME, required=true)
    private long lifetime;

    public DelegateAuthResponse() {
    }

    public DelegateAuthResponse(String authToken) {
        this(authToken, null);
    }

    public DelegateAuthResponse(String authToken, Long lifetime) {
        this.authToken = authToken;
        if (lifetime != null)
            this.lifetime = lifetime;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    public long getLifetime() {
        return lifetime;
    }
}
