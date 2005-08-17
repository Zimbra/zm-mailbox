package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcCreateTagResponse extends LmcSoapResponse {

    private LmcTag mTag;

    public LmcTag getTag() { return mTag; }

    public void setTag(LmcTag t) { mTag = t; }
}
