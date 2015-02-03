package com.zimbra.common.lmtp;

public class LmtpClientException extends Exception {
    private static final long serialVersionUID = 1L;
    public LmtpClientException(String msg) {
        super(msg);
    }

    public LmtpClientException(Throwable e) {
        super(e);
    }
    public LmtpClientException(String msg, Throwable e) {
        super(msg,e);
    }
}
