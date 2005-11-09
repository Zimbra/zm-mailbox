/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client;

public class LmcFolder {

    private String folderID;
    private String name;
    private String parentID;
    private String numUnread;
    private LmcFolder mSubFolders[];

    public void setFolderID(String f) { folderID = f; }
    public void setName(String n) { name = n; }
    public void setParentID(String p) { parentID = p; }
    public void setNumUnread(String n) { numUnread = n; }
    public void setSubFolders(LmcFolder f[]) { mSubFolders = f; }

    public String getFolderID() { return folderID; }
    public String getName() { return name; }
    public String getParentID() { return parentID; }
    public String getNumUnread() { return numUnread; }
    public LmcFolder[] getSubFolders() { return mSubFolders; }

    // does not return subfolders
    public String toString() {
    	return "name=\"" + name + "\" ID=\"" + folderID + "\" parent=\"" + 
               parentID + "\" numUnread=\"" + numUnread + "\" numSubFolders=\"" +
               ((mSubFolders == null) ? 0 : mSubFolders.length) + "\"";
    }
}