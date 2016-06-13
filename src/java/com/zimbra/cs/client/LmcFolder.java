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