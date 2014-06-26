/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountBy;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountSelector {

    /**
     * @zm-api-field-tag acct-selector-by
     * @zm-api-field-description Select the meaning of <b>{acct-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=true)
    private final AccountBy accountBy;

    /**
     * @zm-api-field-tag acct-selector-key
     * @zm-api-field-description The key used to identify the account. Meaning determined by <b>{acct-selector-by}</b>
     */
    @XmlValue
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountSelector() {
        this.accountBy = null;
        this.key = null;
    }

    public AccountSelector(AccountBy by, String key) {
        this.accountBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public AccountBy getBy() { return accountBy; }

    public static AccountSelector fromId(String id) {
        return new AccountSelector(AccountBy.id, id);
    }

    public static AccountSelector fromName(String name) {
        return new AccountSelector(AccountBy.name, name);
    }
}
