package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcAuthResponse extends LmcSoapResponse {

    private LmcSession mSession;

    public void setSession(LmcSession s) { mSession = s; }
    
    public LmcSession getSession() { return mSession; }

}
    
