package com.liquidsys.coco.client.soap;

public class LmcMaintainTablesResponse extends LmcSoapResponse {
    
    int mNumTables = 0;
    
    LmcMaintainTablesResponse(int numTables) {
        mNumTables = numTables;
    }
    
    public int getNumTables() {
        return mNumTables;
    }
}
