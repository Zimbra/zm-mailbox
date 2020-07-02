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

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("verifyResult", verifyResult);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
