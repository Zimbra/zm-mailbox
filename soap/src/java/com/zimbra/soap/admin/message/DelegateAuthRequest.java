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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Used to request a new auth token that is valid for the specified account.
 * The id of the auth token will be the id of the target account, and the requesting admin's id will be stored in
 * the auth token for auditing purposes.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DELEGATE_AUTH_REQUEST)
@XmlType(propOrder = {})
public class DelegateAuthRequest {

    /**
     * @zm-api-field-description Details of target account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private AccountSelector account;

    /**
     * @zm-api-field-tag lifetime-in-seconds
     * @zm-api-field-description Lifetime in seconds of the newly-created authtoken. defaults to 1 hour. Can't be
     * longer then <b>zimbraAuthTokenLifetime</b>.
     */
    @XmlAttribute(name=AdminConstants.A_DURATION, required=false)
    private long duration;

    public DelegateAuthRequest() {
    }

    public DelegateAuthRequest(AccountSelector account) {
        this(account, null);
    }

    public DelegateAuthRequest(AccountSelector account, Long duration) {
        this.account = account;
        if (duration != null)
            this.duration = duration;
    }

    public void setAccount(AccountSelector account) {
        this.account = account;
    }

    public AccountSelector getAccount() {
        return account;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }
}
