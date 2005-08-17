package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcBrowseResponse extends LmcSoapResponse {

    private LmcBrowseData mData[];
    
    public void setData(LmcBrowseData d[]) { mData = d; }
    
    public LmcBrowseData[] getData() { return mData; }
}
