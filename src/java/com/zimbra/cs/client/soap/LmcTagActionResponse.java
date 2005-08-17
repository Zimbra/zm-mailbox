package com.zimbra.cs.client.soap;

public class LmcTagActionResponse extends LmcSoapResponse {

    private String mIDList;
    private String mOp;

    public String getTagList() { return mIDList; }
    public String getOp() { return mOp; }
    
    public void setTagList(String idList) { mIDList = idList; }
    public void setOp(String op) { mOp = op; }
}
