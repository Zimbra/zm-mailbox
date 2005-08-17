package com.zimbra.cs.client.soap;

import java.util.HashMap;

public class LmcGetPrefsResponse extends LmcSoapResponse {

    // for storing the returned preferences
    private HashMap mPrefMap;

    public HashMap getPrefsMap() { return mPrefMap; }

    public void setPrefsMap(HashMap p) { mPrefMap = p; }

}
