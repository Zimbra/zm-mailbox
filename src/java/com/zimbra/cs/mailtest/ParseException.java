package com.zimbra.cs.mailtest;

/**
 * Parser error.
 */
public class ParseException extends MailException {
    public ParseException() {}

    public ParseException(String msg) {
        super(msg);
    }
}
