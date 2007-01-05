package com.zimbra.cs.im;

import java.util.HashMap;

public enum  IMGatewayType {
    aol,
    msn,
    yahoo,
    irq,
    xmpp;

    static private HashMap<String, IMGatewayType> sTypeMap = new HashMap<String, IMGatewayType>();
    
    public String getShortName() { return toString(); }
}
