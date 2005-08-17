package com.liquidsys.coco.client;

public class LmcContactAttr {
    
    private String mAttrName;
    private String mID;
    private String mRef;
    private String mAttrData;

    public LmcContactAttr(String attrName,
                          String id,
                          String ref,
                          String attrData)
    {
        mAttrName = attrName;
        mID = id;
        mRef = ref;
        mAttrData = attrData;
    }
    
    public String getAttrName() { return mAttrName; }
    
    public String getID() { return mID; }
    
    public String getRef() { return mRef; }
    
    public String getAttrData() { return mAttrData; }
}