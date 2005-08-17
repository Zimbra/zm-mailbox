package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcGetMsgResponse extends LmcSoapResponse {

    private LmcMessage mMsg;

    public LmcMessage getMsg() { return mMsg; }

    public void setMsg(LmcMessage m) { mMsg = m; }
}
