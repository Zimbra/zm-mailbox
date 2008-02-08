package com.zimbra.cs.mailclient;

import java.io.IOException;

public class MailException extends IOException {
    public MailException() {}
    
    public MailException(String msg) {
        super(msg);
    }
}
