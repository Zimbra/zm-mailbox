/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.Name;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-description Reload Account
 * <br />
 * Called after another server has made changes to the account object, this request tells the server to reload the
 * account object from the ldap master to pick up the changes.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_RELOAD_ACCOUNT_REQUEST)
public class ReloadAccountRequest {

    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Account email address
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=true)
    private Name account;

    private ReloadAccountRequest() {
    }

    private ReloadAccountRequest(Name account) {
        setAccount(account);
    }

    public static ReloadAccountRequest create(Name account) {
        return new ReloadAccountRequest(account);
    }

    public void setAccount(Name account) { this.account = account; }
    public Name getAccount() { return account; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("account", account);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
