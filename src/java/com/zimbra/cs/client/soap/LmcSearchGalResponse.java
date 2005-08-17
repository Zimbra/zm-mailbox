package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.LmcContact;

public class LmcSearchGalResponse extends LmcSoapResponse {
    
	private LmcContact mContacts[];

    public LmcContact[] getContacts() { return mContacts; }

    public void setContacts(LmcContact s[]) { mContacts = s; }
}
