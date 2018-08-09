/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="AuthToken", description="Auth token")
public class AuthToken {

    /**
     * @zm-api-field-description Value for authorization token
     */
    @XmlValue
    private String value;

    /**
     * @zm-api-field-description If verifyAccount="1", &lt;account> is required and the account in the auth token is
     * compared to the named account.
     * If verifyAccount="0" (default), only the auth token is verified and any <b>&lt;account></b> element specified
     * is ignored.
     */
    @XmlAttribute(name=AccountConstants.A_VERIFY_ACCOUNT /* verifyAccount */, required=false)
    private ZmBoolean verifyAccount;
    /**
     * @zm-api-field-description Life time of the auth token
     */
    @XmlAttribute(name=AccountConstants.E_LIFETIME /* lifetime */, required=false)
    private Long lifetime;
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AuthToken() {
        this((String) null, (Boolean) null);
    }

    public AuthToken(String value, Boolean verifyAccount) {
        this.value = value;
        this.verifyAccount = ZmBoolean.fromBool(verifyAccount);
    }

    @GraphQLQuery(name="value", description="Value for authorization token")
    public String getValue() { return value; }
    @GraphQLInputField(name="value", description="Value for authorization token")
    public void setValue(String value) { this.value = value; }

    @GraphQLQuery(name="verifyAccount", description="Denotes whether to verify account data in the request")
    public Boolean getVerifyAccount() { return ZmBoolean.toBool(verifyAccount); }
    @GraphQLInputField(name="verifyAccount", description="Denotes whether to verify account data in the request")
    public void setVerifyAccount(Boolean verifyAccount) { this.verifyAccount = ZmBoolean.fromBool(verifyAccount); }

    @GraphQLQuery(name="lifetime", description="Life time of the auth token")
    public Long getLifetime() {
        return lifetime;
    }

    @GraphQLInputField(name="lifetime", description="Life time of the auth token")
    public void setLifetime(Long lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("value", value)
            .add("verifyAccount", verifyAccount)
            .toString();
    }
}
