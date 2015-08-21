package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CLEAR_TWO_FACTOR_AUTH_DATA_RESPONSE)
public class ClearTwoFactorAuthDataResponse {

    @XmlAttribute(name=AdminConstants.A_STATUS, required=false)
    private String status;

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }
}
