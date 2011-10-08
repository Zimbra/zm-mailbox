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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class AuthToken {

    @XmlValue
    private String value;

    @XmlAttribute(name=AccountConstants.A_VERIFY_ACCOUNT, required=false)
    private Boolean verifyAccount;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AuthToken() {
        this((String) null, (Boolean) null);
    }

    public AuthToken(String value, Boolean verifyAccount) {
        this.value = value;
        this.verifyAccount = verifyAccount;
    }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Boolean getVerifyAccount() { return verifyAccount; }
    public void setVerifyAccount(Boolean verifyAccount) { this.verifyAccount = verifyAccount; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("value", value)
            .add("verifyAccount", verifyAccount)
            .toString();
    }
}
