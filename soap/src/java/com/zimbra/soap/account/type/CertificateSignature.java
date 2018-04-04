/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class CertificateSignature {

    /**
     * @zm-api-field-tag serialNumber
     * @zm-api-field-description serialNumber of the certificate, which is used to uniquely identify the certificate.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_SERIAL_NO, required=false)
    private String serialNumber;

    /**
     * @zm-api-field-tag algorithm
     * @zm-api-field-description algorithm used to create the signature.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_ALGORITHM, required=false)
    private String algorithm;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("serialNo", serialNumber)
            .add("algorithm", algorithm);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}
