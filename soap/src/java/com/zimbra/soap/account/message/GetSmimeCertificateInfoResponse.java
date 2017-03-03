package com.zimbra.soap.account.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.account.type.CertificateInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_GET_SMIME_CERT_INFO_RESPONSE)
public class GetSmimeCertificateInfoResponse {

    /**
     * @zm-api-field-tag certificates
     * @zm-api-field-description list of certificates associated with the user account.
     */
    @XmlElement(name=SmimeConstants.E_CERTIFICATE, required=false)
    private List<CertificateInfo> certificates = Lists.newArrayList();

    public GetSmimeCertificateInfoResponse() {
    }

    public void setCertificates(Iterable <CertificateInfo> certificates) {
        this.certificates.clear();
        if (certificates != null) {
            Iterables.addAll(this.certificates,certificates);
        }
    }

    public void addCertificate(CertificateInfo certificate) {
        this.certificates.add(certificate);
    }

    public List<CertificateInfo> getCertificates() {
        return Collections.unmodifiableList(certificates);
    }
}
