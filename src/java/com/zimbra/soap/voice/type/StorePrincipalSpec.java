/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class StorePrincipalSpec {

    /**
     * @zm-api-field-tag id-of-user-in-the-backing-store
     * @zm-api-field-description ID of user in the backing store
     */
    @XmlAttribute(name=VoiceConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag name-of-user-in-the-backing-store
     * @zm-api-field-description Name of user in the backing store
     */
    @XmlAttribute(name=VoiceConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag account-number
     * @zm-api-field-description Account Number
     */
    @XmlAttribute(name=VoiceConstants.A_ACCOUNT_NUMBER /* accountNumber */, required=false)
    private String accountNumber;

    public StorePrincipalSpec() {
    }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAccountNumber() { return accountNumber; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("name", name)
            .add("accountNumber", accountNumber);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
