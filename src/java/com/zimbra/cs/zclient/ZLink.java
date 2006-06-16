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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

public interface ZLink {

    public String getId();

    /** Returns the folder's name.  Note that this is the folder's
     *  name (e.g. <code>"foo"</code>), not its absolute pathname
     *  (e.g. <code>"/baz/bar/foo"</code>).
     * 
     * @see #getPath() 
     * 
     */
    public String getName();

    /** Returns the folder's absolute path.  Paths are UNIX-style with 
     *  <code>'/'</code> as the path delimiter.  Paths are relative to
     *  the user root folder,
     *  which has the path <code>"/"</code>.  So the Inbox's path is
     *  <code>"/Inbox"</code>, etc.
     */
    public String getPath();

    /**
     * 
     * @return parent id of folder, or null if root folder.
     */
    public String getParentId();

    /**
     * @return primary email address of the owner of the link-to resource
     */
    public String getOwnerDisplayName();

    /**
     * @return zimbra id of the owner of the linked-to resource
     */
    public String getOwnerId();

    /**
     * @return folder id of the remote/linked-to folder
     */
    public String getRemoteId();
    
    /** Returns the "hint" as to which view to use to display the folder's
     *  contents.  conversation|message|contact|appointment|note
     */
    public String getDefaultView();
    
    public String getFlags();

    public byte getColor();

}
