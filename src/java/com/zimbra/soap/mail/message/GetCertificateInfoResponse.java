package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.mail.type.CertificateInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_GET_CERT_INFO_RESPONSE)
public class GetCertificateInfoResponse {

    @XmlElement(name=SmimeConstants.E_CERTIFICATE /* cert */, required=false)
    private List<CertificateInfo> certificates = Lists.newArrayList();

    public GetCertificateInfoResponse() {
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
