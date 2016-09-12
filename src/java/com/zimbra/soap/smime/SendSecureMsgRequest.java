package com.zimbra.soap.smime;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.mail.message.SendMsgRequest;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SmimeConstants.E_SEND_SECURE_MSG_REQUEST)
public class SendSecureMsgRequest extends SendMsgRequest {

    /**
     * @zm-api-field-tag sign
     * @zm-api-field-description Sign mime
     */
    @XmlAttribute(name=SmimeConstants.A_SIGN, required=false)
    private Boolean sign;

    /**
     * @zm-api-field-tag encrypt
     * @zm-api-field-description Encrypt mime
     */
    @XmlAttribute(name=SmimeConstants.A_ENCRYPT, required=false)
    private Boolean encrypt;

    public Boolean isSign() {
        return sign;
    }

    public void setSign(Boolean sign) {
        this.sign = sign;
    }

    public Boolean isEncrypt() {
        return encrypt;
    }

    public void setEncrypt(Boolean encrypt) {
        this.encrypt = encrypt;
    }

}
