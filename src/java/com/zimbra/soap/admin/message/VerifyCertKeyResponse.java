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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.CertMgrConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_VERIFY_CERTKEY_RESPONSE)
public class VerifyCertKeyResponse {

    /**
     * @zm-api-field-tag verify-result
     * @zm-api-field-description Verify result - <b>true|false|invalid</b>
     */
    @XmlAttribute(name=CertMgrConstants.A_verifyResult /* verifyResult */, required=true)
    private String verifyResult;

    public VerifyCertKeyResponse() {
    }

    private VerifyCertKeyResponse(String verifyResult) {
        setVerifyResult(verifyResult);
    }

    public static VerifyCertKeyResponse createForVerifyResult(String verifyResult) {
        return new VerifyCertKeyResponse(verifyResult);
    }

    public void setVerifyResult(String verifyResult) { this.verifyResult = verifyResult; }
    public String getVerifyResult() { return verifyResult; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("verifyResult", verifyResult);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
