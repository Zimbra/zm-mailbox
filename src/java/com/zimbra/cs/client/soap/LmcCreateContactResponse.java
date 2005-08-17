package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcCreateContactResponse extends LmcSoapResponse {
    
    private LmcContact mContact;
    
    public void setContact(LmcContact c) { mContact = c; }
    
    public LmcContact getContact() { return mContact; }

}
