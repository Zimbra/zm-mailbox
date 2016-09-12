package com.zimbra.cs.mime;

import javax.mail.internet.MimeMessage;

public abstract class MimeProcessor {

    private Boolean sign = false;
    private Boolean encrypt = false;

    public abstract void process(MimeMessage mm);

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
