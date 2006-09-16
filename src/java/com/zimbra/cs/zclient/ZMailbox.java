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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.soap.ZSoapMailbox;
import com.zimbra.soap.SoapTransport;

public abstract class ZMailbox {

    public final static String PATH_SEPARATOR = "/";
    
    public final static char PATH_SEPARATOR_CHAR = '/';    

    public enum SearchSortBy {
        dateDesc, dateAsc, subjDesc, subjAsc, nameDesc, nameAsc;

        public static SearchSortBy fromString(String s) throws ServiceException {
            try {
                return SearchSortBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid sortBy: "+s+", valid values: "+Arrays.asList(SearchSortBy.values()), e); 
            }
        }
    }
   
    public static ZMailbox getMailbox(String key, AccountBy by, String password, String uri, SoapTransport.DebugListener listener) throws ServiceException {
        return new ZSoapMailbox(key, by, password, uri, listener);
    }

    public static ZMailbox getMailbox(String authToken, String uri, SoapTransport.DebugListener listener) throws ServiceException {
        return new ZSoapMailbox(authToken, uri, listener);
    }
   
    /**
     * returns the parent folder path. First removes a trailing {@link #PATH_SEPARATOR} if one is present, then
     * returns the value of the path preceeding the last {@link #PATH_SEPARATOR} in the path.
     * @param path path must be absolute
     * @throws ServiceException 
     */
    public static String getParentPath(String path) throws ServiceException {
        if (path.equals(PATH_SEPARATOR)) return PATH_SEPARATOR;
        if (path.charAt(0) != PATH_SEPARATOR_CHAR) 
            throw ServiceException.INVALID_REQUEST("path must be absoliute: "+path, null);
        if (path.charAt(path.length()-1) == PATH_SEPARATOR_CHAR)
            path = path.substring(0, path.length()-1);
        int index = path.lastIndexOf(PATH_SEPARATOR_CHAR);
        path = path.substring(0, index);
        if (path.length() == 0) return PATH_SEPARATOR;
        else return path;
    }

    /**
     * returns the base folder path. First removes a trailing {@link #PATH_SEPARATOR} if one is present, then
     * returns the value of the path trailing the last {@link #PATH_SEPARATOR} in the path.
     * @throws ServiceException 
     */
    public static String getBasePath(String path) throws ServiceException {
        if (path.equals(PATH_SEPARATOR)) return PATH_SEPARATOR;
        if (path.charAt(0) != PATH_SEPARATOR_CHAR) 
            throw ServiceException.INVALID_REQUEST("path must be absoliute: "+path, null);
        if (path.charAt(path.length()-1) == PATH_SEPARATOR_CHAR)
            path = path.substring(0, path.length()-1);
        int index = path.lastIndexOf(PATH_SEPARATOR_CHAR);
        return path.substring(index+1);
    }
    
    /**
     * @return current size of mailbox in bytes
     */
    public abstract long getSize();

    /**
     * @return account name of mailbox
     */
    public abstract String getName() throws ServiceException;
    
    public abstract ZGetInfoResult getAccountInfo(boolean refresh) throws ServiceException;
    
    public abstract String getAuthToken();
    
    //  ------------------------
    
    /**
     * @return current List of all tags in the mailbox
     */
    public abstract List<ZTag> getAllTags();
    
    /**
     * @return current list of all tags names in the mailbox, sorted
     */
    public abstract List<String> getAllTagNames();
    
    /**
     * @return current list of all tags ids in the mailbox
     */
    public abstract List<String> getAllTagIds();
    
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
    public abstract ZTag createTag(String name, ZTag.Color color) throws ServiceException;

    /** modifies the tag's color */
    public abstract ZActionResult modifyTagColor(String id, ZTag.Color color) throws ServiceException;

    /** mark all items with tag as read */
    public abstract ZActionResult markTagRead(String id) throws ServiceException;

    /** delete tag */
    public abstract ZActionResult deleteTag(String id) throws ServiceException;    

    /** rename tag */
    public abstract ZActionResult renameTag(String id, String name) throws ServiceException;        
    
    // ------------------------

    public enum ContactSortBy {
         
        nameDesc, nameAsc;

         public static ContactSortBy fromString(String s) throws ServiceException {
             try {
                 return ContactSortBy.valueOf(s);
             } catch (IllegalArgumentException e) {
                 throw ZClientException.CLIENT_ERROR("invalid sortBy: "+s+", valid values: "+Arrays.asList(ContactSortBy.values()), e);                  
             }
         }
    }

    /**
     * 
     * @param optFolderId return contacts only in specified folder (null for all folders)
     * @param sortBy sort results (null for no sorting)
     * @param sync if true, return modified date on contacts
     * @return
     * @throws ServiceException
     */
    public abstract List<ZContact> getAllContacts(String optFolderId, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException;
    
    public abstract ZContact createContact(String folderId, String tags, Map<String, String> attrs) throws ServiceException;
    
    /**
     * 
     * @param id
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs
     * @return
     * @throws ServiceException
     */
    public abstract ZContact modifyContact(String id, boolean replace, Map<String, String> attrs) throws ServiceException;
    
    /**
     * 
     * @param ids comma-separated list of contact ids
     * @param attrs limit attrs returns to given list
     * @param sortBy sort results (null for no sorting)
     * @param sync if true, return modified date on contacts
     * @return
     * @throws ServiceException
     */
    public abstract List<ZContact> getContacts(String ids, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException;
    
    public abstract ZActionResult moveContact(String ids, String destFolderId) throws ServiceException;
    
    public abstract ZActionResult deleteContact(String ids) throws ServiceException;    
    
    public abstract ZActionResult flagContact(String ids, boolean flag) throws ServiceException;
    
    public abstract ZActionResult tagContact(String ids, String tagId, boolean tag) throws ServiceException;

    /**
     * update items(s)
     * @param ids
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult updateContact(String ids, String destFolderId, String tagList, String flags) throws ServiceException;        
    
    //  ------------------------
    
    public abstract ZConversation getConversation(String id) throws ServiceException;
    
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

    // ------------------------
    
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
     * update items(s)
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

    /* ------------------------------------------------- */

    public abstract String uploadAttachments(File[] files, int msTimeout) throws ServiceException;
    
    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged"
     * @param tags coma-spearated list of tags, or null for no tags
     * @param receivedDate (optional) time the message was originally received, in MILLISECONDS since the epoch 
     * @param content message content
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     */
    public abstract String addMessage(String folderId, String flags, String tags, long receivedDate, String content, boolean noICal) throws ServiceException;
    
    public abstract ZMessage getMessage(String id, boolean markRead, boolean defangedHtml, boolean rawContent, String part, String subId) throws ServiceException;
    
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

    // ------------------------
    
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
     * returns a rest URL relative to this mailbox. 
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @return
     * @throws ServiceException
     */
    public abstract URI getRestURI(String relativePath) throws ServiceException;
    
    /**
     * 
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param os the stream to send the output to
     * @param closeOs whether or not to close the output stream when done
     * @param msecTimeout connection timeout
     * @throws ServiceException
     */
    public abstract void getRESTResource(String relativePath, OutputStream os, boolean closeOs, int msecTimeout) throws ServiceException;

        
    /**
     * 
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param is the input stream to post
     * @param closeIs whether to close the input stream when done
     * @param length length of inputstream, or 0/-1 if length is unknown.
     * @param contentType optional content-type header value (defaults to "application/octect-stream")
     * @param msecTimeout connection timeout
     * @throws ServiceException
     */
    public abstract void postRESTResource(String relativePath, InputStream is, boolean closeIs, long length, String contentType, int msecTimeout) 
        throws ServiceException; 
    
    /**
     * find the search folder with the specified id.
     * @param id id of  folder
     * @return ZSearchFolder if found, null otherwise.
     */
    public abstract ZSearchFolder getSearchFolderById(String id);

    /**
     * find the mountpoint with the specified id.
     * @param id id of mountpoint
     * @return ZMountpoint if found, null otherwise.
     */
    public abstract ZMountpoint getMountpointById(String id);
    
    /**
     * create a new sub folder of the specified parent folder.
     * 
     * @param parentId parent folder id
     * @param name name of new folder
     * @param defaultView default view of new folder or null.
     * @param color color of folder, or null to use default
     * @param flags flags for folder, or null
     *                
     * @return newly created folder
     * @throws ServiceException
     */
    public abstract ZFolder createFolder(String parentId, String name, ZFolder.View defaultView, ZFolder.Color color, String flags) throws ServiceException;
    
    /**
     * create a new sub folder of the specified parent folder.
     * 
     * @param parentId parent folder id
     * @param name name of new folder
     * @param query search query (required)
     * @param types comma-sep list of types to search for. See {@link SearchParams} for more info. Use null for default value.
     * @parm sortBy how to sort the result. Use null for default value.
     * @see {@link ZSearchParams#TYPE_MESSAGE}
     * @return newly created search folder
     * @throws ServiceException
     */
    public abstract ZSearchFolder createSearchFolder(String parentId, String name, String query, String types, SearchSortBy sortBy, ZFolder.Color color) throws ServiceException;

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
    public abstract ZSearchFolder modifySearchFolder(String id, String query, String types, SearchSortBy sortBy) throws ServiceException;
 
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
    public abstract ZActionResult modifyFolderChecked(String ids, boolean checkedState) throws ServiceException;

    /** modifies the folder's color */
    public abstract ZActionResult modifyFolderColor(String ids, ZFolder.Color color) throws ServiceException;
    
    /** hard delete the folder, all items in folder and all sub folders */
    public abstract ZActionResult deleteFolder(String ids) throws ServiceException;

    /** hard delete all items in folder and sub folders (doesn't delete the folder itself) */
    public abstract ZActionResult emptyFolder(String ids) throws ServiceException;    

    /** mark all items in folder as read */
    public abstract ZActionResult markFolderRead(String ids) throws ServiceException;

    /** add the contents of the remote feed at target-url to the folder (one time action) */ 
    public abstract ZActionResult importURLIntoFolder(String id, String url) throws ServiceException;

    /** move the folder to be a child of {target-folder} */
    public abstract ZActionResult moveFolder(String folderId, String targetFolderId) throws ServiceException;
    
    /** change the folder's name; if new name  begins with '/', the folder is moved to the new path and any missing path elements are created */
    public abstract ZActionResult renameFolder(String folderId, String name) throws ServiceException;
    
    /** sets or unsets the folder's exclude from free busy state */
    public abstract ZActionResult modifyFolderExcludeFreeBusy(String folderId, boolean state) throws ServiceException;

    /**
     * 
     * @param folderId
     * @param grantreeType
     * @param grantreeId
     * @param perms permission mask ("rwid")
     * @param args
     * @param inherit
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult modifyFolderGrant(String folderId, ZGrant.GranteeType grantreeType, String grantreeId, String perms, String args, boolean inherit) throws ServiceException;

    /**
     * revoke a grant
     * @param folderId
     * @param grantreeId zimbra ID 
     * @return
     * @throws ServiceException
     */
    public abstract ZActionResult modifyFolderRevokeGrant(String folderId, String grantreeId) throws ServiceException;
    
    /** 
     * set the synchronization url on the folder to {target-url}, empty the folder, and 
     * synchronize the folder's contents to the remote feed, also sets {exclude-free-busy-boolean} 
     */
    public abstract ZActionResult modifyFolderURL(String folderId, String url) throws ServiceException;    

    /**
     * sync the folder's contents to the remote feed specified by the folders URL
     */
    public abstract ZActionResult syncFolder(String folderId) throws ServiceException;    

    // ------------------------
    
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
     * @param parentId parent folder id
     * @param name name of new folder
     * @param defaultView default view of new folder.
     * @param ownerBy used to specify whether owner is an id or account name (email address) 
     * @param owner either the id or name of the owner
     * @param itemBy used to specify whether sharedItem is an id or path to the shared item
     * @param sharedItem either the id or path of the item
     *                
     * @return newly created folder
     * @throws ServiceException
     */
    public abstract ZMountpoint createMountpoint(
            String parentId, String name, 
            ZFolder.View defaultView,
            ZFolder.Color color, 
            String flags,            
            OwnerBy ownerBy,
            String owner,
            SharedItemBy itemBy,
            String sharedItem
            ) throws ServiceException;
}

