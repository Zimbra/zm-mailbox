package com.liquidsys.coco.client;

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