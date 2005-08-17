package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.LmcContact;

public class LmcSearchGalResponse extends LmcSoapResponse {
    
	private LmcContact mContacts[];

    public LmcContact[] getContacts() { return mContacts; }

    public void setContacts(LmcContact s[]) { mContacts = s; }
}
