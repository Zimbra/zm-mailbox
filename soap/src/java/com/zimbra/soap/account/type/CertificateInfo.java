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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = SmimeConstants.E_CERTIFICATE)
public class CertificateInfo {

    /**
     * @zm-api-field-tag pubCertId
     * @zm-api-field-description Item Id associated with the public certificate
     */
    @XmlAttribute(name=SmimeConstants.A_PUB_CERT_ID, required=true)
    private String pubCertId;

    /**
     * @zm-api-field-tag pvtKeyId
     * @zm-api-field-description Item Id associated with the private key
     */
    @XmlAttribute(name=SmimeConstants.A_PVT_KEY_ID, required=true)
    private String pvtKeyId;

    /**
     * @zm-api-field-tag defaultCert
     * @zm-api-field-description Is this certificate the default one, in case user has multiple certificates
     */
    @XmlAttribute(name=SmimeConstants.A_DEFAULT, required=false)
    private ZmBoolean defaultCert;

    /**
     * @zm-api-field-tag error code
     * @zm-api-field-description error code
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_ERROR_CODE, required=false)
    private String errorCode;

    /**
     * @zm-api-field-tag emailAddr
     * @zm-api-field-description email address of the subject (The person, or entity identified.)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_EMAIL_ADDR, required=true)
    private String emailAddr;

    /**
     * @zm-api-field-tag subjectDN
     * @zm-api-field-description details of the subject (The person, or entity identified.)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_SUBJECT_DN, required=true)
    private CertificateDN subjectDN;

    /**
     * @zm-api-field-tag issuerDN
     * @zm-api-field-description details of the issuer (who has issued the certificate)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_ISSUER_DN, required=false)
    private CertificateDN issuerDN;

    /**
     * @zm-api-field-tag validity
     * @zm-api-field-description validity of the certificate
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_VALIDITY, required=false)
    private CertificateValidity validity;

    /**
     * @zm-api-field-tag signature
     * @zm-api-field-description signature details of the certificate
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_SIGNATURE, required=false)
    private CertificateSignature signature;

    /**
     * @zm-api-field-tag subjectAltName
     * @zm-api-field-description subjectAltName details of the certificate
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_SUBJECT_ALT_NAME, required=false)
    private CertificateAltNames subjectAltName;

    /**
     * @zm-api-field-tag issuerAltName
     * @zm-api-field-description issuerAltName details of the certificate
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_ISSUER_ALT_NAME, required=false)
    private CertificateAltNames issuerAltName;

    public CertificateDN getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(CertificateDN subjectDN) {
        this.subjectDN = subjectDN;
    }

    public CertificateDN getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(CertificateDN issuerDN) {
        this.issuerDN = issuerDN;
    }

    public CertificateValidity getValidity() {
        return validity;
    }

    public void setValidity(CertificateValidity validity) {
        this.validity = validity;
    }

    public CertificateSignature getSignature() {
        return signature;
    }

    public void setSignature(CertificateSignature signature) {
        this.signature = signature;
    }

    public String getPubCertId() {
        return pubCertId;
    }

    public void setPubCertId(String pubCertId) {
        this.pubCertId = pubCertId;
    }

    public String getPvtKeyId() {
        return pvtKeyId;
    }

    public void setPvtKeyId(String pvtKeyId) {
        this.pvtKeyId = pvtKeyId;
    }

    public Boolean isDefaultCert() {
        return ZmBoolean.toBool(defaultCert, false);
    }

    public void setDefaultCert(Boolean defaultCert) {
        this.defaultCert = ZmBoolean.fromBool(defaultCert, false);
    }

    public String getEmailAddr() {
        return emailAddr;
    }

    public void setEmailAddr(String emailAddr) {
        this.emailAddr = emailAddr;
    }

    public CertificateAltNames getSubjectAltName() {
        return subjectAltName;
    }

    public void setSubjectAltName(CertificateAltNames subjectAltName) {
        this.subjectAltName = subjectAltName;
    }

    public CertificateAltNames getIssuerAltName() {
        return issuerAltName;
    }

    public void setIssuerAltName(CertificateAltNames issuerAltName) {
        this.issuerAltName = issuerAltName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("pubCertId", pubCertId)
            .add("pvtKeyId", pvtKeyId)
            .add("defaultCert", defaultCert)
            .add("subjectDn", subjectDN)
            .add("issuerDn", issuerDN)
            .add("signature", signature)
            .add("validity", validity)
            .add("emailAddr", emailAddr)
            .add("subjectAltName", subjectAltName)
            .add("issuerAltName", issuerAltName);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}
