/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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