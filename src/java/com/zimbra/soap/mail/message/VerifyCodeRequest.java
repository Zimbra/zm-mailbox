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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-description Validate the verification code sent to a device. After successful validation the
 * server sets the device email address as the value of zimbraCalendarReminderDeviceEmail account attribute.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_VERIFY_CODE_REQUEST)
public class VerifyCodeRequest {

    /**
     * @zm-api-field-tag device-email-address
     * @zm-api-field-description Device email address
     */
    @XmlAttribute(name=MailConstants.A_ADDRESS /* a */, required=false)
    private String address;

    /**
     * @zm-api-field-tag verification-code
     * @zm-api-field-description Verification code
     */
    @XmlAttribute(name=MailConstants.A_VERIFICATION_CODE /* code */, required=false)
    private String verificationCode;

    public VerifyCodeRequest() {
        this(null, null);
    }

    public VerifyCodeRequest(String address, String verificationCode) {
        setAddress(address);
        setVerificationCode(verificationCode);
    }

    public void setAddress(String address) { this.address = address; }
    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
    public String getAddress() { return address; }
    public String getVerificationCode() { return verificationCode; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("address", address)
            .add("verificationCode", verificationCode);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
