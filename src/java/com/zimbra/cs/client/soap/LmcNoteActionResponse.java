package com.zimbra.cs.client.soap;

public class LmcNoteActionResponse extends LmcSoapResponse {

    private String mIDList;
    private String mOp;

    public String getNoteList() { return mIDList; }
    public String getOp() { return mOp; }
    
    public void setNoteList(String idList) { mIDList = idList; }
    public void setOp(String op) { mOp = op; }
}
