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

import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.soap.ZSoapMailbox;
import com.zimbra.soap.SoapTransport;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    public static class Options {
        private String mAccount;
        private AccountBy mAccountBy = AccountBy.name;
        private String mPassword;
        private String mAuthToken;
        private String mUri;
        private SoapTransport.DebugListener mDebugListener;
        private String mTargetAccount;
        private AccountBy mTargetAccountBy = AccountBy.name;
        private boolean mNoSession;
        private boolean mNoNotify;

        public Options() {
        }
        
        public Options(String account, AccountBy accountBy, String password, String uri) {
            mAccount = account;
            mAccountBy = accountBy;
            mPassword = password;
            mUri = uri;
        }

        public Options(String authToken, String uri) {
            mAuthToken = authToken;
            mUri = uri;
        }

        public String getAccount() { return mAccount; }
        public void setAccount(String account) { mAccount = account; }

        public AccountBy getAccountBy() { return mAccountBy; }
        public void setAccountBy(AccountBy accountBy) { mAccountBy = accountBy; }

        public String getTargetAccount() { return mAccount; }
        public void setTargetAccount(String targetAccount) { mTargetAccount = targetAccount; }

        public AccountBy getTaretAccountBy() { return mTargetAccountBy; }
        public void setTargetAccountBy(AccountBy targetAccountBy) { mTargetAccountBy = targetAccountBy; }

        public String getPassword() { return mPassword; }
        public void setPassword(String password) { mPassword = password; }

        public String getAuthToken() { return mAuthToken; }
        public void setAuthToken(String authToken) { mAuthToken = authToken; }

        public String getUri() { return mUri; }
        public void setUri(String uri) { mUri = uri; }

        public SoapTransport.DebugListener getDebugListener() { return mDebugListener; }
        public void setDebugListener(SoapTransport.DebugListener liistener) { mDebugListener = liistener; }

        public boolean getNoSession() { return mNoSession; }
        public void setNoSession(boolean noSession) { mNoSession = noSession; }

        public boolean getNoNotify() { return mNoNotify; }
        public void setNoNotify(boolean noNotify) { mNoNotify = noNotify; }

    }

    public static ZMailbox getMailbox(Options options) throws ServiceException {
        return new ZSoapMailbox(options);
    }

    /**
     * returns the parent folder path. First removes a trailing {@link #PATH_SEPARATOR} if one is present, then
     * returns the value of the path preceeding the last {@link #PATH_SEPARATOR} in the path.
     * @param path path must be absolute
     * @throws ServiceException if an error occurs
     * @return the parent folder path
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
     * @throws ServiceException if an error occurs
     * @return base path
     * @param path the path we are getting the base from
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
     * @throws com.zimbra.cs.service.ServiceException on error
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
     * @param name tag name
     * @return the tag, or null if tag not found
     */
    public abstract ZTag getTagByName(String name);

    /**
     * returns the tag with the specified id, or null if no such tag exists.
     * 
     * @param id the tag id
     * @return tag with given id, or null
     */
    public abstract ZTag getTagById(String id);

    /**
     * create a new tag with the specified color.
     * 
     * @return newly created tag
     * @param name name of the tag
     * @param color color of the tag
     * @throws com.zimbra.cs.service.ServiceException if an error occurs
     *
     */
    public abstract ZTag createTag(String name, ZTag.Color color) throws ServiceException;

    /**
     * modifies the tag's color
     * @return action result
     * @param id id of tag to modify
     * @param color color of tag to modify
     * @throws com.zimbra.cs.service.ServiceException on error
     */
    public abstract ZActionResult modifyTagColor(String id, ZTag.Color color) throws ServiceException;

    /** mark all items with tag as read
     * @param id id of tag to mark read 
     * @return action reslult
     * @throws ServiceException on error
     */
    public abstract ZActionResult markTagRead(String id) throws ServiceException;

    /**
     * delete tag
     * @param id id of tag to delete
     * @return action result
     * @throws ServiceException on error 
     */
    public abstract ZActionResult deleteTag(String id) throws ServiceException;    

    /**
     * rename tag
     * @param id id of tag
     * @param name new name of tag
     * @throws ServiceException on error
     * @return action result
     */
    public abstract ZActionResult renaZActionResultmeTag(String id, String name) throws ServiceException;
    
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
     * @return list of contacts
     * @throws ServiceException on error
     * @param attrs specified attrs to return, or null for all.
     */
    public abstract List<ZContact> getAllContacts(String optFolderId, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException;
    
    public abstract ZContact createContact(String folderId, String tags, Map<String, String> attrs) throws ServiceException;
    
    /**
     * 
     * @param id of contact
     * @param replace if true, replace all attrs with specified attrs, otherwise merge with existing
     * @param attrs modified attrs
     * @return updated contact
     * @throws ServiceException on error
     */
    public abstract ZContact modifyContact(String id, boolean replace, Map<String, String> attrs) throws ServiceException;
    
    /**
     * 
     * @param ids comma-separated list of contact ids
     * @param attrs limit attrs returns to given list
     * @param sortBy sort results (null for no sorting)
     * @param sync if true, return modified date on contacts
     * @return list of contacts
     * @throws ServiceException on error
     */
    public abstract List<ZContact> getContacts(String ids, ContactSortBy sortBy, boolean sync, List<String> attrs) throws ServiceException;
    
    public abstract ZActionResult moveContact(String ids, String destFolderId) throws ServiceException;
    
    public abstract ZActionResult deleteContact(String ids) throws ServiceException;    
    
    public abstract ZActionResult flagContact(String ids, boolean flag) throws ServiceException;
    
    public abstract ZActionResult tagContact(String ids, String tagId, boolean tag) throws ServiceException;

    /**
     * update items(s)
     * @param ids list of contact ids to update
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
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
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult moveItem(String ids, String destFolderId, String targetConstraints) throws ServiceException;

    /**
     * update items(s)
     * @param ids list of items to act on
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @param targetConstraints list of charecters comprised of TC_INCLUDE_* strings. Constrains the set of
     *         affected items A leading '-' means to negate the constraint(s). Use null for
     *         no constraints.  
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult updateItem(String ids, String destFolderId, String tagList, String flags, String targetConstraints) throws ServiceException;        

    /* ------------------------------------------------- */

    public abstract String uploadAttachments(File[] files, int msTimeout) throws ServiceException;

    public abstract String uploadAttachment(String name, byte[] attachment, String contentType, int msTimeout) throws ServiceException;
    
    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged"
     * @param tags coma-spearated list of tags, or null for no tags
     * @param receivedDate (optional) time the message was originally received, in MILLISECONDS since the epoch 
     * @param content message content
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws com.zimbra.cs.service.ServiceException on error
     */
    public abstract String addMessage(String folderId, String flags, String tags, long receivedDate, String content, boolean noICal) throws ServiceException;
    
    /**
     * @param folderId (required) folderId of folder to add message to
     * @param flags non-comma-separated list of flags, e.g. "sf" for "sent by me and flagged"
     * @param tags coma-spearated list of tags, or null for no tags
     * @param receivedDate (optional) time the message was originally received, in MILLISECONDS since the epoch 
     * @param content message content
     * @param noICal if TRUE, then don't process iCal attachments.
     * @return ID of newly created message
     * @throws ServiceException on error
     */
    public abstract String addMessage(String folderId, String flags, String tags, long receivedDate, byte[] content, boolean noICal) throws ServiceException;
    
    public abstract ZMessage getMessage(String id, boolean markRead, boolean wantHtml, boolean neuterImages, boolean rawContent, String part) throws ServiceException;
    
    /**
     * hard delete message(s)
     * @param ids ids to act on
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult deleteMessage(String ids) throws ServiceException;

    /**
     * mark message(s) as read/unread
     * @param ids ids to act on
     * @return action result
     * @throws ServiceException on error
     * @param read mark read/unread
     */
    public abstract ZActionResult markMessageRead(String ids, boolean read) throws ServiceException;
    
    /**
     *  mark message as spam/not spam 
     * @param spam spam (TRUE) or not spam (FALSE)
     * @param id id of message
     * @param destFolderId optional id of destination folder, only used with "not spam".
     * @throws ServiceException on error
     * @return action result
     */
    public abstract ZActionResult markMessageSpam(String id, boolean spam, String destFolderId) throws ServiceException;
    
    /** flag/unflag message(s)
     *
     * @return action result
     * @param ids of messages to flag
     * @param flag flag on /off
     * @throws com.zimbra.cs.service.ServiceException on error
     */
    public abstract ZActionResult flagMessage(String ids, boolean flag) throws ServiceException;
    
    /** tag/untag message(s)
     * @param ids ids of messages to tag
     * @param tagId tag id to tag with
     * @param tag tag/untag
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult tagMessage(String ids, String tagId, boolean tag) throws ServiceException;    
    
    /** move message(s)
     * @param ids list of ids to move
     * @param destFolderId destination folder id
     * @return action result
     * @throws ServiceException on error 
     */
    public abstract ZActionResult moveMessage(String ids, String destFolderId) throws ServiceException;        

    /**
     * update message(s)
     * @param ids ids of messages to update
     * @param destFolderId optional destination folder
     * @param tagList optional new list of tag ids
     * @param flags optional new value for flags
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult updateMessage(String ids, String destFolderId, String tagList, String flags) throws ServiceException;        

    // ------------------------
    
    /**
     * return the root user folder
     * @return user root folder
     */
    public abstract ZFolder getUserRoot();

    /**
     * find the folder with the pecified path, starting from the user root.
     * @param path path of folder. Must start with {@link #PATH_SEPARATOR}.
     * @return ZFolder if found, null otherwise.
     * @throws ServiceException on error
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
     * @return URI of path
     * @throws ServiceException on error
     */
    public abstract URI getRestURI(String relativePath) throws ServiceException;
    
    /**
     * 
     * @param relativePath a relative path (i.e., "/Calendar", "Inbox?fmt=rss", etc).
     * @param os the stream to send the output to
     * @param closeOs whether or not to close the output stream when done
     * @param msecTimeout connection timeout
     * @throws ServiceException on error
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
     * @throws ServiceException on error
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
     * @throws ServiceException on error
     */
    public abstract ZFolder createFolder(String parentId, String name, ZFolder.View defaultView, ZFolder.Color color, String flags) throws ServiceException;
    
    /**
     * create a new sub folder of the specified parent folder.
     * 
     * @param parentId parent folder id
     * @param name name of new folder
     * @param query search query (required)
     * @param types comma-sep list of types to search for. See {@link SearchParams} for more info. Use null for default value.
     * @param sortBy how to sort the result. Use null for default value.
     * @see {@link ZSearchParams#TYPE_MESSAGE}
     * @return newly created search folder
     * @throws ServiceException on error
     * @param color color of folder
     */
    public abstract ZSearchFolder createSearchFolder(String parentId, String name, String query, String types, SearchSortBy sortBy, ZFolder.Color color) throws ServiceException;

    /**
     * modify a search folder.
     * 
     * @param id id of search folder
     * @param query search query or null to leave unchanged.
     * @param types new types or null to leave unchanged.
     * @param sortBy new sortBy or null to leave unchanged
     * @return modified search folder
     * @throws ServiceException on error
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

    /** sets or unsets the folder's checked state in the UI
     * @param ids ids of folder to check
     * @param checkedState checked/unchecked
     * @throws ServiceException on error
     * @return action result
     */
    public abstract ZActionResult modifyFolderChecked(String ids, boolean checkedState) throws ServiceException;

    /** modifies the folder's color
     * @param ids ids to modify
     * @param color new color
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult modifyFolderColor(String ids, ZFolder.Color color) throws ServiceException;
    
    /** hard delete the folder, all items in folder and all sub folders
     * @param ids ids to delete
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult deleteFolder(String ids) throws ServiceException;

    /** hard delete all items in folder and sub folders (doesn't delete the folder itself)
     * @param ids ids of folders to empty
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult emptyFolder(String ids) throws ServiceException;    

    /** mark all items in folder as read
     * @param ids ids of folders to mark as read
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult markFolderRead(String ids) throws ServiceException;

    /** add the contents of the remote feed at target-url to the folder (one time action)
     * @param id of folder to import into
     * @param url url to import
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult importURLIntoFolder(String id, String url) throws ServiceException;

    /** move the folder to be a child of {target-folder}
     * @param folderId folder id to move
     * @param targetFolderId id of target folder
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult moveFolder(String folderId, String targetFolderId) throws ServiceException;
    
    /** change the folder's name; if new name  begins with '/', the folder is moved to the new path and any missing path elements are created
     * @param folderId id of folder to rename
     * @param name new name
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult renameFolder(String folderId, String name) throws ServiceException;

    /** sets or unsets the folder's exclude from free busy state
     * @param folderId folder id
     * @param state exclude/not-exclude
     * @throws ServiceException on error
     * @return action result
     */
    public abstract ZActionResult modifyFolderExcludeFreeBusy(String folderId, boolean state) throws ServiceException;

    /**
     * 
     * @param folderId to modify
     * @param grantreeType type of grantee
     * @param grantreeId id of grantree
     * @param perms permission mask ("rwid")
     * @param args extra args
     * @param inherit inherited bit
     * @return action result
     * @throws ServiceException on error 
     */
    public abstract ZActionResult modifyFolderGrant(String folderId, ZGrant.GranteeType grantreeType, String grantreeId, String perms, String args, boolean inherit) throws ServiceException;

    /**
     * revoke a grant
     * @param folderId folder id to modify
     * @param grantreeId zimbra ID 
     * @return action result
     * @throws ServiceException on error 
     */
    public abstract ZActionResult modifyFolderRevokeGrant(String folderId, String grantreeId) throws ServiceException;
    
    /** 
     * set the synchronization url on the folder to {target-url}, empty the folder, and 
     * synchronize the folder's contents to the remote feed, also sets {exclude-free-busy-boolean}
     * @param folderId id of folder
     * @param url new URL
     * @return action result
     * @throws ServiceException on error
     */
    public abstract ZActionResult modifyFolderURL(String folderId, String url) throws ServiceException;    

    /**
     * sync the folder's contents to the remote feed specified by the folders URL
     * @param folderId folder id
     * @throws ServiceException on error
     * @return action result
     */
    public abstract ZActionResult syncFolder(String folderId) throws ServiceException;    

    // ------------------------
    
    /**
     * do a search 
     * @param params search prams
     * @return search result
     * @throws ServiceException on error
     */
    public abstract ZSearchResult search(ZSearchParams params) throws ServiceException;
    
    /**
     *  do a search conv
     * @param convId id of conversation to search 
     * @param params convId onversation id
     * @return search result
     * @throws ServiceException on error  
     */
    public abstract ZSearchResult searchConversation(String convId, ZSearchParams params) throws ServiceException;
    
    /**
     * A request that does nothing and always returns nothing. Used to keep a session alive, and return
     * any pending notifications.
     *
     * @throws ServiceException on error
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
     * @throws ServiceException on error
     * @param color initial color
     * @param flags initial flags
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

    /**
     * Sends an iCalendar REPLY object
     * @param ical iCalendar data
     * @throws ServiceException on error
     */
    public abstract void iCalReply(String ical) throws ServiceException;
}
