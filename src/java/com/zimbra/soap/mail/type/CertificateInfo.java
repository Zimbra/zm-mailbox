package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class CertificateInfo {

    @XmlAttribute(name=SmimeConstants.A_PUB_CERT_ID, required=true)
    private String pubCertId;

    @XmlAttribute(name=SmimeConstants.A_PVT_KEY_ID, required=true)
    private String pvtKeyId;

    @XmlAttribute(name=SmimeConstants.A_DEFAULT, required=false)
    private ZmBoolean defaultCert;

    @XmlElement(name=SmimeConstants.E_SUBJECT_DN, required=true)
    private String subjectDN;

    @XmlElement(name=SmimeConstants.E_ISSUER_DN, required=false)
    private String issuerDN;

    @XmlElement(name=SmimeConstants.E_VALIDITY, required=false)
    private Validity validity;

    @XmlElement(name=SmimeConstants.E_SIGNATURE, required=false)
    private Signature signature;

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

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("pubCertId", pubCertId)
            .add("pvtKeyId", pvtKeyId)
            .add("defaultCert", "default")
            .add("subjectDn", subjectDN)
            .add("issuerDn", issuerDN)
            .add("signature", signature)
            .add("validity", validity);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }

}
