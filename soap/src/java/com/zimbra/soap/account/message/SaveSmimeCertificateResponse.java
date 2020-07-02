/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.account.type.CertificateInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_SAVE_SMIME_CERTIFICATE_RESPONSE)
public class SaveSmimeCertificateResponse {

    /**
     * @zm-api-field-tag certificate
     * @zm-api-field-description details of certificate saved
     */
    @XmlElement(name=SmimeConstants.E_CERTIFICATE, required=false)
    private CertificateInfo certificate;

    public SaveSmimeCertificateResponse() {
    }

    public CertificateInfo getCertificate() {
        return certificate;
    }

    public void setCertificate(CertificateInfo certificate) {
        this.certificate = certificate;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("certificate", certificate).toString();
    }
    
}
