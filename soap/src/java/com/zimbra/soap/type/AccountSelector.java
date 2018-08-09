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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="AccountSelector")
public class AccountSelector {

    /**
     * @zm-api-field-tag acct-selector-by
     * @zm-api-field-description Select the meaning of <b>{acct-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=true)
    @GraphQLNonNull
    @GraphQLQuery(name="accountBy", description="Select the meaning of {acct-selector-key}")
    private final AccountBy accountBy;

    /**
     * @zm-api-field-tag acct-selector-key
     * @zm-api-field-description The key used to identify the account. Meaning determined by <b>{acct-selector-by}</b>
     */
    @XmlValue
    @GraphQLQuery(name="key", description="The key used to identify the account. Meaning determined by {acct-selector-by}")
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountSelector() {
        this.accountBy = null;
        this.key = null;
    }

    public AccountSelector(
        @GraphQLInputField @GraphQLNonNull AccountBy by,
        @GraphQLInputField String key) {
        this.accountBy = by;
        this.key = key;
    }

    @GraphQLQuery(name="key", description="The key used to identify the account. Meaning determined by {acct-selector-by}")
    public String getKey() { return key; }

    @GraphQLNonNull
    @GraphQLQuery(name="accountBy", description="Select the meaning of {acct-selector-key}")
    public AccountBy getBy() { return accountBy; }

    public static AccountSelector fromId(String id) {
        return new AccountSelector(AccountBy.id, id);
    }

    public static AccountSelector fromName(String name) {
        return new AccountSelector(AccountBy.name, name);
    }
}
