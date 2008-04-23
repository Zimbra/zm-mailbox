package com.zimbra.cs.mailclient.imap;

public interface ResponseHandler {
    boolean handleResponse(ImapResponse res) throws Exception;
}
