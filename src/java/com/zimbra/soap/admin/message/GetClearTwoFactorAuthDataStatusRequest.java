package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_CLEAR_TWO_FACTOR_AUTH_DATA_STATUS_REQUEST)
public class GetClearTwoFactorAuthDataStatusRequest {
    @XmlElement(name=AdminConstants.E_COS, required=false)
    private CosSelector cos;

    private GetClearTwoFactorAuthDataStatusRequest() {}

    public GetClearTwoFactorAuthDataStatusRequest(CosSelector cos) {
        setCos(cos);
    }

    public void setCos(CosSelector cos) {this.cos = cos; }
    public CosSelector getCos() {return cos; }
}
