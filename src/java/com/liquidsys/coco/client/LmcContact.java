package com.liquidsys.coco.client;

public class LmcContact {
    
    private String mID;
    private String mTags;
    private String mFlags;
    private String mModDate;
    private String mFolder;
    private LmcContactAttr mAttrs[];

    public String getID() { return mID; }
    public String getTags() { return mTags; }
    public String getFlags() { return mFlags; }
    public String getModDate() { return mModDate; }
    public String getFolder() { return mFolder; }
    public LmcContactAttr[] getAttrs() { return mAttrs; }

    public void setID(String id) { mID = id; }
    public void setTags(String t) { mTags = t; }
    public void setFlags(String f) { mFlags = f; }
    public void setModDate(String md) { mModDate = md; }
    public void setFolder(String f) { mFolder = f; }
    public void setAttrs(LmcContactAttr attrs[]) { mAttrs = attrs; }
    
    public String toString() {
        String result = "Contact: mID=\"" + mID + "\" tags=\"" + mTags + "\" flags=\"" +
            mFlags + "\" moddate=\"" + mModDate + "\" folder=\"" + mFolder + "\"";
        if (mAttrs != null)
            result += " and " + mAttrs.length + " attributes";
        return result;
    }
}