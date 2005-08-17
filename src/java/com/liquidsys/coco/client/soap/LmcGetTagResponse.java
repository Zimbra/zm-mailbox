package com.liquidsys.coco.client.soap;

import java.util.*;

import com.liquidsys.coco.client.*;

public class LmcGetTagResponse extends LmcSoapResponse {

    // for storing the returned tags
    private ArrayList mTags;

    public LmcTag[] getTags() {
        if (mTags == null || mTags.size() == 0)
        	return null;
        LmcTag tags[] = new LmcTag[mTags.size()];
        return (LmcTag []) mTags.toArray(tags);
    }
    
    public void setTags(ArrayList a) { mTags = a; }
}
