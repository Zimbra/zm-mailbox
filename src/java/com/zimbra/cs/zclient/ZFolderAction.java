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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

public class ZFolderAction {
    
    private Op mOp;
    private boolean mChecked;
    private boolean mExcludeFreeBusy;
    private int mColor;
    private String mURL;
    private String mName;
    private String mTargetFolderId;
    
    public static enum Op {
        CHECK,        
        COLOR,        
        DELETE,
        EMPTY,
        GRANT, // TODO
        IMPORT,        
        MARK_AS_READ,
        MOVE,        
        RENAME,
        EXCLUDE_FREE_BUSY,
        SYNC,
        URL;
    }

    private ZFolderAction(Op op) {
        mOp = op;
    }

    private ZFolderAction initChecked(boolean state)    { mChecked = state; return this; }
    private ZFolderAction initExcludeFreeBusy(boolean s){ mExcludeFreeBusy = s; return this; }
    private ZFolderAction initColor(int color)          { mColor = color; return this; }
    private ZFolderAction initURL(String url)           { mURL = url; return this; }    
    private ZFolderAction initName(String name)         { mName = name; return this; }        
    private ZFolderAction initTargetFolderId(String id) { mTargetFolderId = id; return this; }
    
    /** used with {@link #setChecked(boolean)} action */
    public boolean getChecked() { return mChecked; }

    /** used with {@link #setExcludeFreeBusy(boolean)} action */
    public boolean getExcludeFreeBusy() { return mExcludeFreeBusy; }
    
    /** used with {@link #setColor(byte)} action */
    public int getColor() { return mColor; }
    
    /** used with {@link #importURL(url)} and {@link #setURL(url)} actions */
    public String getURL() { return mURL; }
    
    /** used with {@link #rename(name)} action */
    public String getName() { return mName; }    

    /** used with {@link #move(id)} action */
    public String getTargertFolderId() { return mTargetFolderId; }

    /** sets or unsets the folder's checked state in the UI */
    public static ZFolderAction setChecked(boolean state) {  return new ZFolderAction(Op.CHECK).initChecked(state); }
    
    /** sets or unsets the folder's checked state in the UI */
    public static ZFolderAction setColor(int color) {  return new ZFolderAction(Op.COLOR).initColor(color); }

    /** hard delete the folder, all items in folder and all sub folders */
    public static ZFolderAction delete() {  return new ZFolderAction(Op.DELETE); }

    /** hard delete all items in folder and sub folders */
    public static ZFolderAction empty() {  return new ZFolderAction(Op.EMPTY); }    
    
    /** mark all items in folder as read */
    public static ZFolderAction markAsRead() {  return new ZFolderAction(Op.MARK_AS_READ); }
    
    /** add the contents of the remote feed at target-url to the folder (one time action) */ 
    public static ZFolderAction importURL(String url) {  return new ZFolderAction(Op.IMPORT).initURL(url); }

    /** move the folder to be a child of {target-folder} */
    public static ZFolderAction move(String folderId) {  return new ZFolderAction(Op.MOVE).initTargetFolderId(folderId); }
    
    /** move the folder to be a child of {target-folder} */
    public static ZFolderAction move(ZFolder folder) {  return new ZFolderAction(Op.MOVE).initTargetFolderId(folder.getId()); }    

    /** change the folder's name; if new name  begins with '/', the folder is moved to the new path and any missing path elements are created */
    public static ZFolderAction rename(String name) {  return new ZFolderAction(Op.RENAME).initName(name); }

    /** sets or unsets the folder's exclude from free busy state */
    public static ZFolderAction setExcludeFreeBusy(boolean s) {  return new ZFolderAction(Op.EXCLUDE_FREE_BUSY).initExcludeFreeBusy(s); }

    /** add the contents of the remote feed at target-url to the folder (one time action) */ 
    public static ZFolderAction setURL(String url) {  return new ZFolderAction(Op.URL).initURL(url); }    

    /** sync the folder's contents to the remote feed specified by the folders URL */
    public static ZFolderAction sync() {  return new ZFolderAction(Op.SYNC); }

    public Op getOp() { return mOp; }
    
    public static class Result {
        private String mIds;
        
        public Result(String ids) {
            mIds = ids;
        }
        
        public String getIds() {
            return mIds;
        }
        
        public String[] getIdsAsArray() {
            return mIds.split(",");
        }
        
        public String toString() {
            return String.format("actionResult: { ids: %s }", mIds);
        }
    }
}
