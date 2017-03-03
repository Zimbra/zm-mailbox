package com.zimbra.cs.mime;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

public abstract class MimeProcessor {

    private Boolean sign = false;
    private Boolean encrypt = false;
    private String certId = null;

    public abstract void process(MimeMessage mm, Mailbox mbox) throws ServiceException;

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

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }
}
