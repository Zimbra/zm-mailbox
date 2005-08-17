package com.liquidsys.coco.client.soap;

public class LmcFolderActionResponse extends LmcSoapResponse {

    private String mIDList;
    private String mOp;

    public String getFolderList() { return mIDList; }
    public String getOp() { return mOp; }
    
    public void setFolderList(String idList) { mIDList = idList; }
    public void setOp(String op) { mOp = op; }
}
