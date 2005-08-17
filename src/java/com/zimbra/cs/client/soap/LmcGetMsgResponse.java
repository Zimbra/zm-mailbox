package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcGetMsgResponse extends LmcSoapResponse {

    private LmcMessage mMsg;

    public LmcMessage getMsg() { return mMsg; }

    public void setMsg(LmcMessage m) { mMsg = m; }
}
