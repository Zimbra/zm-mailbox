/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.AccountBy;

/**
 * <account> element created for backwards compatibility with old API that uses "name" attribute:
 * <account name='username@domain' />
 */
@XmlAccessorType(XmlAccessType.NONE)
public class AccountNameSelector {

    /**
     * @zm-api-field-tag acct-selector-by
     * @zm-api-field-description Select the meaning of <b>{acct-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=false)
    private final AccountBy accountBy;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     * @deprecated
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=false)
    private final String name;


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
    private AccountNameSelector() {
        this.accountBy = null;
        this.key = null;
        this.name = null;
    }

    public AccountNameSelector(AccountBy by, String key) {
        this.accountBy = by;
        this.key = key;
        if(by == AccountBy.name) {
            this.name = key;
        } else {
            this.name = null;
        }
    }

    public String getKey() {
        if(key == null || key.length() == 0) {
            return name;
        } else {
            return key;
        }
    }

    public AccountBy getBy() {
        if(accountBy == null) {
            return AccountBy.name;
        } else {
            return accountBy;
        }
    }

    public String getName() {
        return name;
    }

    public AccountNameSelector(String name) {
        this.name = name;
        this.key = name;
        this.accountBy = AccountBy.name;
    }

    public static AccountNameSelector fromId(String id) {
        return new AccountNameSelector(AccountBy.id, id);
    }

    public static AccountNameSelector fromName(String name) {
        return new AccountNameSelector(AccountBy.name, name);
    }
}
