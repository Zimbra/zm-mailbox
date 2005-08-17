package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcCreateTagResponse extends LmcSoapResponse {

    private LmcTag mTag;

    public LmcTag getTag() { return mTag; }

    public void setTag(LmcTag t) { mTag = t; }
}
