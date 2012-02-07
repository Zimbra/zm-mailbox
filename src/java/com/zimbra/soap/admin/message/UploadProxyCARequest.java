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
 * @zm-api-command-description Upload proxy CA
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_UPLOAD_PROXYCA_REQUEST)
public class UploadProxyCARequest {

    /**
     * @zm-api-field-description Certificate attach ID
     */
    @XmlAttribute(name=CertMgrConstants.A_CERT_AID /* cert.aid */, required=true)
    private String certificateAttachId;

    /**
     * @zm-api-field-description Certificate name
     */
    @XmlAttribute(name=CertMgrConstants.A_CERT_NAME /* cert.filename */, required=true)
    private String certificateName;

    private UploadProxyCARequest() {
    }

    private UploadProxyCARequest(String certificateAttachId, String certificateName) {
        setCertificateAttachId(certificateAttachId);
        setCertificateName(certificateName);
    }

    public static UploadProxyCARequest createForAttachIdAndCert(String certificateAttachId, String certificateName) {
        return new UploadProxyCARequest(certificateAttachId, certificateName);
    }

    public void setCertificateAttachId(String certificateAttachId) { this.certificateAttachId = certificateAttachId; }
    public void setCertificateName(String certificateName) { this.certificateName = certificateName; }
    public String getCertificateAttachId() { return certificateAttachId; }
    public String getCertificateName() { return certificateName; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("certificateAttachId", certificateAttachId)
            .add("certificateName", certificateName);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
