package com.liquidsys.coco.client.soap;

public class LmcConvActionResponse extends LmcSoapResponse {

    private String mIDList;
    private String mOp;

    public String getConvList() { return mIDList; }
    public String getOp() { return mOp; }
    
    public void setConvList(String idList) { mIDList = idList; }
    public void setOp(String op) { mOp = op; }
}
