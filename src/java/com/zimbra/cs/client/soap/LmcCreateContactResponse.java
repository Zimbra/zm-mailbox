package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcCreateContactResponse extends LmcSoapResponse {
    
    private LmcContact mContact;
    
    public void setContact(LmcContact c) { mContact = c; }
    
    public LmcContact getContact() { return mContact; }

}
