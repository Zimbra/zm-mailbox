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

/**
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("certificate", certificate)
            .add("privateKey", privateKey);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
