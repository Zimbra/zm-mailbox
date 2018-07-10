package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_SEND_SECURE_MSG_REQUEST)
public class SendSecureMsgRequest extends SendMsgRequest {

    /**
     * @zm-api-field-tag sign
     * @zm-api-field-description Sign mime
     */
    @XmlAttribute(name=SmimeConstants.A_SIGN, required=false)
    private ZmBoolean sign;

    /**
     * @zm-api-field-tag encrypt
     * @zm-api-field-description Encrypt mime
     */
    @XmlAttribute(name=SmimeConstants.A_ENCRYPT, required=false)
    private ZmBoolean encrypt;

    /**
     * @zm-api-field-tag certId
     * @zm-api-field-description Certificate Id
     */
    @XmlAttribute(name=SmimeConstants.A_CERT_ID, required=false)
    private String certId;

    public Boolean getSign() {
        return ZmBoolean.toBool(sign, false);
    }

    public void setSign(Boolean sign) {
        this.sign = ZmBoolean.fromBool(sign, false);
    }

    public Boolean getEncrypt() {
        return ZmBoolean.toBool(encrypt, false);
    }

    public void setEncrypt(Boolean encrypt) {
        this.encrypt = ZmBoolean.fromBool(encrypt, false);
    }

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("sign", sign)
            .add("encrypt", encrypt)
            .add("certId", certId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
