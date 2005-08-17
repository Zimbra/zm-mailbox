package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcGetConvResponse extends LmcSoapResponse {

    private LmcConversation mConv;

    public LmcConversation getConv() { return mConv; }

    public void setConv(LmcConversation c) { mConv = c; }

}
