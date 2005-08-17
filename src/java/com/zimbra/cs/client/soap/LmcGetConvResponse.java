package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcGetConvResponse extends LmcSoapResponse {

    private LmcConversation mConv;

    public LmcConversation getConv() { return mConv; }

    public void setConv(LmcConversation c) { mConv = c; }

}
