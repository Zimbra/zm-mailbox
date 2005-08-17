package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcAuthResponse extends LmcSoapResponse {

    private LmcSession mSession;

    public void setSession(LmcSession s) { mSession = s; }
    
    public LmcSession getSession() { return mSession; }

}
    
