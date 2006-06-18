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
    
    public abstract ZFolder createFolder(ZFolder parent, String defaultView) throws ServiceException;
 
    public abstract ZFolderAction.Result doAction(ZFolderAction action, ZFolder folder) throws ServiceException;
    
    public abstract ZFolderAction.Result doAction(ZFolderAction action, String ids) throws ServiceException;    
    
    public abstract ZSearchResult search(ZSearchParams params) throws ServiceException;
}
