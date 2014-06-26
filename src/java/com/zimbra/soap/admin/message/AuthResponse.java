/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

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
    @ZimbraJsonAttribute
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
