package com.zimbra.cs.client.soap;

public class LmcAddMsgResponse extends LmcSoapResponse {

    private String mID;

    /**
     * Get the ID of the added message.
     */
    public String getID() { return mID; }

    public void setID(String id) { mID = id; }
}
