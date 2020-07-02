package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SmimeConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_GET_SMIME_CERT_INFO_REQUEST)
public class GetSmimeCertificateInfoRequest {

    /**
     * @zm-api-field-tag certId
     * @zm-api-field-description Certificate Id. If it's value is specified, details of certificate matching that id will be returned.
             If value is not specified, details of all certificates of the user will be returned.
     */
    @XmlAttribute(name=SmimeConstants.A_CERT_ID, required=false)
    private String certId;

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("certId", certId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}
