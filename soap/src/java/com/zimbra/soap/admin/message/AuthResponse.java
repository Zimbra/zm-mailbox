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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTH_RESPONSE)
@GraphQLType(name=GqlConstants.CLASS_AUTH_RESPONSE, description="Response to account authentication request.")
public class AuthResponse {

    /**
     * @zm-api-field-tag auth-token
     * @zm-api-field-description Auth Token
     */
    @XmlElement(name=AdminConstants.E_AUTH_TOKEN /* authToken */, required=true)
    private String authToken;

    /**
     * @zm-api-field-description if client is CSRF token enabled , the CSRF token Returned only when client says it is CSRF enabled .
     */
    @XmlElement(name = HeaderConstants.E_CSRFTOKEN /* CSRF token */, required = false)
    private String csrfToken;

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
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.AUTH_TOKEN, description="The authorization token")
    public String getAuthToken() { return authToken; }
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.LIFETIME, description="Lifetime of the token")
    public long getLifetime() { return lifetime; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("authToken", authToken)
            .add("lifetime", lifetime);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    /**
     * @return the csrfToken
     */
    @GraphQLQuery(name=GqlConstants.CSRF_TOKEN, description="The csrf token returned if the client says it is csrf enabled")
    public String getCsrfToken() {
        return csrfToken;
    }

    /**
     * @param csrfToken
     *            the csrfToken to set
     */
    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

}
