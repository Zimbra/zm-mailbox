package com.zimbra.cs.client.soap;

import java.util.HashMap;

public class LmcGetInfoResponse extends LmcSoapResponse {

    private String mAcctName;
    private String mLifetime;
    private HashMap mPrefMap;

    public HashMap getPrefMap() { return mPrefMap; }
    public String getLifetime() { return mLifetime; }
    public String getAcctName() { return mAcctName; }

    public void setPrefMap(HashMap p) { mPrefMap = p; }
    public void setLifetime(String l) { mLifetime = l; }
    public void setAcctName(String a) { mAcctName = a; }
}
