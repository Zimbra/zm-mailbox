package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
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
     * @zm-api-field-tag emailAddr
     * @zm-api-field-description email address of the subject (The person, or entity identified.)
     */
    @XmlElement(name=SmimeConstants.E_EMAIL_ADDR, required=true)
    private String emailAddr;

    /**
     * @zm-api-field-tag subjectDN
     * @zm-api-field-description details of the subject (The person, or entity identified.)
     */
    @XmlElement(name=SmimeConstants.E_SUBJECT_DN, required=true)
    private String subjectDN;

    /**
     * @zm-api-field-tag issuerDN
     * @zm-api-field-description details of the issuer (who has issued the certificate)
     */
    @XmlElement(name=SmimeConstants.E_ISSUER_DN, required=false)
    private String issuerDN;

    /**
     * @zm-api-field-tag validity
     * @zm-api-field-description validity of the certificate
     */
    @XmlElement(name=SmimeConstants.E_VALIDITY, required=false)
    private CertificateValidity validity;

    /**
     * @zm-api-field-tag signature
     * @zm-api-field-description signature details of the certificate
     */
    @XmlElement(name=SmimeConstants.E_SIGNATURE, required=false)
    private CertificateSignature signature;

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("pubCertId", pubCertId)
            .add("pvtKeyId", pvtKeyId)
            .add("defaultCert", "default")
            .add("subjectDn", subjectDN)
            .add("issuerDn", issuerDN)
            .add("signature", signature)
            .add("validity", validity)
            .add("emailAddr", emailAddr);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }

}
