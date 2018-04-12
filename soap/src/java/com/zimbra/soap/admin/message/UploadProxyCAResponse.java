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
@XmlRootElement(name=CertMgrConstants.E_UPLOAD_PROXYCA_RESPONSE)
public class UploadProxyCAResponse {

    /**
     * @zm-api-field-tag certificate-content
     * @zm-api-field-description Certificate content
     */
    @XmlAttribute(name=CertMgrConstants.A_cert_content /* cert_content */, required=false)
    private String certificateContent;

    private UploadProxyCAResponse() {
    }

    private UploadProxyCAResponse(String certificateContent) {
        setCertificateContent(certificateContent);
    }

    public static UploadProxyCAResponse createForCert(String certificateContent) {
        return new UploadProxyCAResponse(certificateContent);
    }

    public void setCertificateContent(String certificateContent) { this.certificateContent = certificateContent; }
    public String getCertificateContent() { return certificateContent; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("certificateContent", certificateContent);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
