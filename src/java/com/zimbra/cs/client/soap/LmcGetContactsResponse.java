package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcGetContactsResponse extends LmcSoapResponse {

    private LmcContact mContacts[];

    public LmcContact[] getContacts() { return mContacts; }

    public void setContacts(LmcContact s[]) { mContacts = s; }
}
