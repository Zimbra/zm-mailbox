/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client;

public class LmcFolder {

    private String folderID;
    private String name;
    private String parentID;
    private String numUnread;
    private String view;
    private LmcFolder mSubFolders[];

    public void setFolderID(String f) { folderID = f; }
    public void setName(String n) { name = n; }
    public void setParentID(String p) { parentID = p; }
    public void setNumUnread(String n) { numUnread = n; }
    public void setView(String v) { view = v; }
    public void setSubFolders(LmcFolder f[]) { mSubFolders = f; }

    public String getFolderID() { return folderID; }
    public String getName() { return name; }
    public String getParentID() { return parentID; }
    public String getNumUnread() { return numUnread; }
    public String getView() { return view; }
    public LmcFolder[] getSubFolders() { return mSubFolders; }

    // does not return subfolders
    public String toString() {
    	return "name=\"" + name + "\" ID=\"" + folderID + "\" parent=\"" +
    		   (view == null ? "" : "\" view=\"" + view) +
               parentID + "\" numUnread=\"" + numUnread + "\" numSubFolders=\"" +
               ((mSubFolders == null) ? 0 : mSubFolders.length) + "\"";
    }
}