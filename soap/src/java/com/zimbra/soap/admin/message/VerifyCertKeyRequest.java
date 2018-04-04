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

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Verify Certificate Key
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_VERIFY_CERTKEY_REQUEST)
public class VerifyCertKeyRequest {

    /**
     * @zm-api-field-description Certificate
     */
    @XmlAttribute(name=CertMgrConstants.E_cert /* cert */, required=false)
    private String certificate;

    /**
     * @zm-api-field-description Private key
     */
    @XmlAttribute(name=CertMgrConstants.A_privkey /* privkey */, required=false)
    private String privateKey;

    public VerifyCertKeyRequest() {
    }

    private VerifyCertKeyRequest(String certificate, String privateKey) {
        setCertificate(certificate);
        setPrivateKey(privateKey);
    }

    public static VerifyCertKeyRequest createForCertAndPrivateKey(String certificate, String privateKey) {
        return new VerifyCertKeyRequest(certificate, privateKey);
    }

    public void setCertificate(String certificate) { this.certificate = certificate; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getCertificate() { return certificate; }
    public String getPrivateKey() { return privateKey; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("certificate", certificate)
            .add("privateKey", privateKey);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
