/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Name;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Puts the mailbox of the specified account into maintenance lockout or removes it from maintenance lockout
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_LOCKOUT_MAILBOX_REQUEST)
public class LockoutMailboxRequest {

    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Account email address
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=true)
    private Name account;

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description one of 'start' or 'end'
     */
    @XmlAttribute(name=AdminConstants.A_OPERATION)
    private String operation;


    private LockoutMailboxRequest() {
    }

    private LockoutMailboxRequest(Name account) {
        setAccount(account);
    }

    public static LockoutMailboxRequest create(Name account) {
        return new LockoutMailboxRequest(account);
    }

    public void setAccount(Name account) { this.account = account; }
    public Name getAccount() { return account; }

    public void setOperation(String operation) { this.operation = operation; }
    public String getOperation() { return operation; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("account", account)
            .add("operation", operation);

    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
