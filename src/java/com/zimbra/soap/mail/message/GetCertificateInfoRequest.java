package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.SmimeConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_GET_CERT_INFO_REQUEST)
public class GetCertificateInfoRequest {

    @XmlAttribute(name=SmimeConstants.A_CERT_ID, required=false)
    private String certId;

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("certId", certId);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }

}
