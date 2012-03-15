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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AuthToken {

    /**
     * @zm-api-field-description Value for authorization token
     */
    @XmlValue
    private String value;

    /**
     * @zm-api-field-description * If verifyAccount="1", &lt;account> is required and the account in the auth token is
     * compared to the named account.
     * If verifyAccount="0" (default), only the auth token is verified and any <account> element specified is ignored.
     */
    @XmlAttribute(name=AccountConstants.A_VERIFY_ACCOUNT /* verifyAccount */, required=false)
    private ZmBoolean verifyAccount;

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

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Boolean getVerifyAccount() { return ZmBoolean.toBool(verifyAccount); }
    public void setVerifyAccount(Boolean verifyAccount) { this.verifyAccount = ZmBoolean.fromBool(verifyAccount); }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("value", value)
            .add("verifyAccount", verifyAccount)
            .toString();
    }
}
