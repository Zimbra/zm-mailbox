package com.liquidsys.coco.client.soap;

public class LmcSendMsgResponse extends LmcSoapResponse {

    private String mID;

    /**
     * Get the ID of the created message.
     */
    public String getID() { return mID; }

    public void setID(String id) { mID = id; }

}
