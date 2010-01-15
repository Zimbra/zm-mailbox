/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client;

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