/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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


import com.zimbra.common.soap.AdminConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description gets offline request license request certificate
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name= AdminConstants.E_GET_OFFLINE_LICENSE_CERTIFICATE_RESPONSE)
public class GetOfflineLicenseCertificateResponse {

    /**
     * @zm-api-field-description licenseCode
     */
    @XmlElement(name=AdminConstants.E_OFFLINE_LICENSE_ACTIVATION_REQUEST_CERTIFICATE /* activationRequestCertificate */, required=true)
    private final String activationRequestCertificate;

    public GetOfflineLicenseCertificateResponse() {
        this.activationRequestCertificate = null;
    }

    public String getActivationRequestCertificate() {
        return activationRequestCertificate;
    }
}
