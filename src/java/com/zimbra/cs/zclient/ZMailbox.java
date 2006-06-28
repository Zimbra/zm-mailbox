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

import java.util.Collection;

import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.service.ServiceException;

public abstract class ZMailbox {

    public final static String PATH_SEPARATOR = "/";

    /**
     * @return current size of mailbox in bytes
     */
    public abstract long getSize();
    
    /**
     * @return current list of all tags in the mailbox
     */
    public abstract Collection<? extends ZTag> getAllTags();
    
    /**
     * returns the tag the specified name, or null if no such tag exists.
     * 
     * @param name
     * @return
     */
    public abstract ZTag getTagByName(String name);

    /**
     * returns the tag with the specified id, or null if no such tag exists.
     * 
     * @param id
     * @return
     */
    public abstract ZTag getTagById(String id);

    /** create a new tag with the specified color. */
    public abstract ZTag createTag(String name, int color) throws ServiceException;

    /** modifies the tag's color */
    public abstract ZActionResult setTagColor(String id, int color) throws ServiceException;

    /** mark all items with tag as read */
    public abstract ZActionResult markTagAsRead(String id) throws ServiceException;

    /** delete tag */
    public abstract ZActionResult deleteTag(String id) throws ServiceException;    

    /** rename tag */
    public abstract ZActionResult renameTag(String id, String name) throws ServiceException;        
    
    /** include items in the Trash folder */
    public static final String TC_INCLUDE_TRASH = "t";
    
    /** include items in the Spam/Junk folder */
    public static final String TC_INCLUDE_JUNK = "j";
    
    /** include items in the Sent folder */
    public static final String TC_INCLUDE_SENT = "s";
    
    /** include items in any other folder */
    public static final String TC_INCLUDE_OTHER = "o";

    /**
     * hard delete conversation(s).
     * 
     * @param ids list of conversation ids to act on
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult deleteConversation(String ids, String targetConstraints) throws ServiceException;

    /**
     * mark conversation as read/unread
     * 
     * @param ids list of conversation ids to act on
     * @param read mark read (TRUE) or unread (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult markConversationRead(String ids, boolean read, String targetConstraints) throws ServiceException;

    /**
     * flag/unflag conversations
     * 
     * @param ids list of conversation ids to act on
     * @param flag flag (TRUE) or unflag (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult flagConversation(String ids, boolean flag, String targetConstraints) throws ServiceException;

    /**
     * tag/untag conversations
     * 
     * @param ids list of conversation ids to act on
     * @param tagId id of tag to tag/untag with
     * @param tag tag (TRUE) or untag (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult tagConversation(String ids, String tagId, boolean tag, String targetConstraints) throws ServiceException;


    /**
     * move conversations
     * 
     * @param ids list of conversation ids to act on
     * @param destFolderId id of destination folder
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult moveConversation(String ids, String destFolderId, String targetConstraints) throws ServiceException;

    /**
     * spam/unspam a single conversation
     * 
     * @param id conversation id to act on
     * @param spam spam (TRUE) or not spam (FALSE)
     * @param destFolderId optional id of destination folder, only used with "not spam".
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items in a conversation. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult markConversationSpam(String id, boolean spam, String destFolderId, String targetConstraints) throws ServiceException;

    /**
     * hard delete item(s).
     * 
     * @param ids list of item ids to act on
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult deleteItem(String ids, String targetConstraints) throws ServiceException;

    /**
     * mark item as read/unread
     * 
     * @param ids list of ids to act on
     * @param read mark read (TRUE) or unread (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult markItemRead(String ids, boolean read, String targetConstraints) throws ServiceException;

    /**
     * flag/unflag items
     * 
     * @param ids list of ids to act on
     * @param flag flag (TRUE) or unflag (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult flagItem(String ids, boolean flag, String targetConstraints) throws ServiceException;

    /**
     * tag/untag items
     * 
     * @param ids list of ids to act on
     * @param tagId id of tag to tag/untag with
     * @param tag tag (TRUE) or untag (FALSE)
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items. A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult tagItem(String ids, String tagId, boolean tag, String targetConstraints) throws ServiceException;

    /**
     * move conversations
     * 
     * @param ids list of item ids to act on
     * @param destFolderId id of destination folder
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult moveItem(String ids, String destFolderId, String targetConstraints) throws ServiceException;

    /**
     * update message(s)
     * @param ids
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult updateItem(String ids, String destFolderId, String tagList, String flags, String targetConstraints) throws ServiceException;        
    
    /** hard delete message(s) */
    public abstract ZActionResult deleteMessage(String ids) throws ServiceException;

    /** mark message(s) as read/unread */
    public abstract ZActionResult markMessageRead(String ids, boolean read) throws ServiceException;
    
    /**
     *  mark message as spam/not spam 
     * @param spam spam (TRUE) or not spam (FALSE)
     * @param destFolderId optional id of destination folder, only used with "not spam".
     */
    public abstract ZActionResult markMessageSpam(String id, boolean spam, String destFolderId) throws ServiceException;
    
    /** flag/unflag message(s) */
    public abstract ZActionResult flagMessage(String ids, boolean flag) throws ServiceException;
    
    /** tag/untag message(s) */
    public abstract ZActionResult tagMessage(String ids, String tagId, boolean tag) throws ServiceException;    
    
    /** move message(s) */
    public abstract ZActionResult moveMessage(String ids, String destFolderId) throws ServiceException;        

    /**
     * update message(s)
     * @param ids
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult updateMessage(String ids, String destFolderId, String tagList, String flags) throws ServiceException;        

    /**
     * return the root user folder
     */
    public abstract ZFolder getUserRoot();

    /**
     * find the folder with the pecified path, starting from the user root.
     * @param path path of folder. Must start with {@link #PATH_SEPARATOR}.
     * @return ZFolder if found, null otherwise.
     */
    public abstract ZFolder getFolderByPath(String path) throws ServiceException;
    
    /**
     * find the folder with the specified id.
     * @param id id of  folder
     * @return ZFolder if found, null otherwise.
     */
    public abstract ZFolder getFolderById(String id);
    
    /**
     * find the search folder with the specified id.
     * @param id id of  folder
     * @return ZSearchFolder if found, null otherwise.
     */
    public abstract ZSearchFolder getSearchFolderById(String id);

    /**
     * find the link with the specified id.
     * @param id id of link
     * @return ZLink if found, null otherwise.
     */
    public abstract ZLink getLinkById(String id);
    
    /**
     * create a new sub folder of the specified parent folder.
     * 
     * @param parent parent folder
     * @param name name of new folder
     * @param defaultView default view of new folder. 
     * @see {@link ZFolder#VIEW_APPOINTMENT}
     * @see {@link ZFolder#VIEW_CONTACT}
     * @see {@link ZFolder#VIEW_CONVERSATION}
     * @see {@link ZFolder#VIEW_MESSAGE}
     *                
     * @return newly created folder
     * @throws ServiceException
     */
    public abstract ZFolder createFolder(ZFolder parent, String name, String defaultView) throws ServiceException;
    
    /**
     * create a new sub folder of the specified parent folder.
     * 
     * @param parent parent folder
     * @param name name of new folder
     * @param query search query (required)
     * @param types comma-sep list of types to search for. See {@link SearchParams} for more info. Use null for default value.
     * @parm sortBy how to sort the result. See {@link SearchParams} for more info. Use null for default value.
     * @see {@link ZSearchParams#SORT_BY_DATE_ASC}
     * @see {@link ZSearchParams#TYPE_MESSAGE}
     * @return newly created search folder
     * @throws ServiceException
     */
    public abstract ZSearchFolder createSearchFolder(ZFolder parent, String name, String query, String types, String sortBy) throws ServiceException;

    /**
     * modify a search folder.
     * 
     * @param id id of search folder
     * @param query search query or null to leave unchanged.
     * @param types new types or null to leave unchanged.
     * @parm sortBy new sortBy or null to leave unchanged
     * @return modified search folder
     * @throws ServiceException
     */
    public abstract ZSearchFolder modifySearchFolder(String id, String query, String types, String sortBy) throws ServiceException;
 
    public static class ZActionResult {
        private String mIds;
        
        public ZActionResult(String ids) {
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

    /** sets or unsets the folder's checked state in the UI */
    public abstract ZActionResult setFolderChecked(String ids, boolean checkedState) throws ServiceException;

    /** modifies the folder's color */
    public abstract ZActionResult setFolderColor(String ids, int color) throws ServiceException;
    
    /** hard delete the folder, all items in folder and all sub folders */
    public abstract ZActionResult deleteFolder(String ids) throws ServiceException;

    /** hard delete all items in folder and sub folders (doesn't delete the folder itself) */
    public abstract ZActionResult emptyFolder(String ids) throws ServiceException;    

    /** mark all items in folder as read */
    public abstract ZActionResult markFolderAsRead(String ids) throws ServiceException;

    /** add the contents of the remote feed at target-url to the folder (one time action) */ 
    public abstract ZActionResult importURLIntoFolder(String id, String url) throws ServiceException;

    /** move the folder to be a child of {target-folder} */
    public abstract ZActionResult moveFolder(String folderId, String targetFolderId) throws ServiceException;
    
    /** change the folder's name; if new name  begins with '/', the folder is moved to the new path and any missing path elements are created */
    public abstract ZActionResult renameFolder(String folderId, String name) throws ServiceException;
    
    /** sets or unsets the folder's exclude from free busy state */
    public abstract ZActionResult setFolderExcludeFreeBusy(String folderId, boolean state) throws ServiceException;
    
    /** 
     * set the synchronization url on the folder to {target-url}, empty the folder, and 
     * synchronize the folder's contents to the remote feed, also sets {exclude-free-busy-boolean} 
     */
    public abstract ZActionResult setFolderURL(String folderId, String url) throws ServiceException;    

    /**
     * sync the folder's contents to the remote feed specified by the folders URL
     */
    public abstract ZActionResult syncFolder(String folderId) throws ServiceException;    

    /**
     * 
     * @param params
     * @return
     * @throws ServiceException
     */
    public abstract ZSearchResult search(ZSearchParams params) throws ServiceException;
    
    /**
     * 
     * @param params
     * @return
     * @throws ServiceException
     */
    public abstract ZSearchResult searchConversation(String convId, ZSearchParams params) throws ServiceException;
    
    /**
     * A request that does nothing and always returns nothing. Used to keep a session alive, and return
     * any pending notifications.
     *
     * @throws ServiceException
     */
    public abstract void noOp() throws ServiceException;    

    public enum OwnerBy { BY_ID, BY_NAME }
    
    public enum SharedItemBy { BY_ID, BY_PATH }
    
    /**
     * create a new mointpoint in the specified parent folder.
     * 
     * @param parent parent folder
     * @param name name of new folder
     * @param defaultView default view of new folder.
     * @param ownerBy used to specify whether owner is an id or account name (email address) 
     * @param owner either the id or name of the owner
     * @param itemBy used to specify whether sharedItem is an id or path to the shared item
     * @param sharedItem either the id or path of the item
     * @see {@link ZFolder#VIEW_APPOINTMENT}
     * @see {@link ZFolder#VIEW_CONTACT}
     * @see {@link ZFolder#VIEW_CONVERSATION}
     * @see {@link ZFolder#VIEW_MESSAGE}
     *                
     * @return newly created folder
     * @throws ServiceException
     */
    public abstract ZLink createMountpoint(
            ZFolder parent, String name, 
            String defaultView,
            OwnerBy ownerBy,
            String owner,
            SharedItemBy itemBy,
            String sharedItem
            ) throws ServiceException;
}

