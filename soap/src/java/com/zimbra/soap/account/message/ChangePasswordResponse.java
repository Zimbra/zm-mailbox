/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/**
<ChangePasswordResponse>
   <authToken>...</authToken>
   <lifetime>...</lifetime>
<ChangePasswordResponse/>
 * @zm-api-response-description Note: Returns new authToken, as old authToken will be invalidated on password change.
 */
@XmlRootElement(name=AccountConstants.E_CHANGE_PASSWORD_RESPONSE)
@GraphQLType(name=GqlConstants.CLASS_CHANGE_PASSWORD_RESPONSE, description="The response to change password request.")
@XmlType(propOrder = {})
public class ChangePasswordResponse {

    /**
     * @zm-api-field-tag new-auth-token
     * @zm-api-field-description New authToken, as old authToken is invalidated on password change.
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN /* authToken */, required=true)
    private String authToken;
    /**
     * @zm-api-field-description Life time associated with <b>{new-auth-token}</b>
     */
    @ZimbraJsonAttribute
    @XmlElement(name=AccountConstants.E_LIFETIME /* lifetime */, required=true)
    private long lifetime;

    public ChangePasswordResponse() {
    }

    @GraphQLQuery(name=GqlConstants.AUTH_TOKEN, description="Auth token based on the new password")
    public String getAuthToken() { return authToken; }
    @GraphQLQuery(name=GqlConstants.LIFETIME, description="Life time of the auth token")
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
