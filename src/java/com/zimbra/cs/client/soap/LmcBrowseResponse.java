package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcBrowseResponse extends LmcSoapResponse {

    private LmcBrowseData mData[];
    
    public void setData(LmcBrowseData d[]) { mData = d; }
    
    public LmcBrowseData[] getData() { return mData; }
}
