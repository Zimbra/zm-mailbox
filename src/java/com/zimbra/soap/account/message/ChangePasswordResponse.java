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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

/**
<ChangePasswordResponse>
   <authToken>...</authToken>
   <lifetime>...</lifetime>
<ChangePasswordResponse/>
 * @zm-api-response-description Note: Returns new authToken, as old authToken will be invalidated on password change.
 */
@XmlRootElement(name=AccountConstants.E_CHANGE_PASSWORD_RESPONSE)
@XmlType(propOrder = {})
public class ChangePasswordResponse {

    /**
     * @zm-api-field-tag new-auth-token
     * @zm-api-field-description New authToken, as old authToken is invalidated on password change.
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN, required=true)
    private String authToken;
    /**
     * @zm-api-field-description Life time associated with <b>{new-auth-token}</b>
     */
    @XmlElement(name=AccountConstants.E_LIFETIME, required=true)
    private long lifetime;

    public ChangePasswordResponse() {
    }

    public String getAuthToken() { return authToken; }
    public long getLifetime() { return lifetime; }

    public ChangePasswordResponse setAuthToken(String authToken) {
        this.authToken = authToken;
        return this;
    }

    public ChangePasswordResponse setLifetime(long lifetime) {
        this.lifetime = lifetime;
        return this;
    }
}
