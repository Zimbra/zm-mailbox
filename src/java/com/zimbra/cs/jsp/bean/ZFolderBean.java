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
package com.zimbra.cs.jsp.bean;

import java.util.List;

import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZSearchFolder;

public class ZFolderBean {
    
    private ZFolder mFolder;
    
    public ZFolderBean(ZFolder folder) {
        mFolder = folder;
    }

    public ZFolderBean getParent() { return mFolder.getParent() == null ? null : new ZFolderBean(mFolder.getParent()); }

    public String getId() { return mFolder.getId(); }

    /** Returns the folder's name.  Note that this is the folder's
     *  name (e.g. <code>"foo"</code>), not its absolute pathname
     *  (e.g. <code>"/baz/bar/foo"</code>).
     * 
     * @see #getPath() 
     * 
     */
    public String getName() { return mFolder.getName(); }

    /** Returns the folder's absolute path.  Paths are UNIX-style with 
     *  <code>'/'</code> as the path delimiter.  Paths are relative to
     *  the user root folder,
     *  which has the path <code>"/"</code>.  So the Inbox's path is
     *  <code>"/Inbox"</code>, etc.
     */
    public String getPath() { return mFolder.getPath(); }

    /** Returns the folder's absolute path, with special chars in the names
     * URL encoded.
     */
    public String getPathUrlEncoded() { return mFolder.getPathUrlEncoded(); }

    /**
     * 
     * @return parent id of folder, or null if root folder.
     */
    public String getParentId() { return mFolder.getParentId(); }

    /**
     * @return number of unread items in folder
     */
    public int getUnreadCount() { return mFolder.getUnreadCount(); }
    
    public boolean getHasUnread() { return getUnreadCount() > 0; }

    /**
     * @return number of unread items in folder
     */
    public int getMessageCount() { return mFolder.getMessageCount(); }
    
    /** Returns the "hint" as to which view to use to display the folder's
     *  contents.
     */
    public String getDefaultView() { return mFolder.getDefaultView().name(); } 
    
    /**
     *  checked in UI (#), exclude free/(b)usy info, IMAP subscribed (*)
     */
    public String getFlags() { return mFolder.getFlags(); }

    public boolean getHasFlags() { return mFolder.hasFlags(); }
    
    public boolean getIsCheckedInUI() { return mFolder.isCheckedInUI(); }

    public boolean getIsExcludedFromFreeBusy() { return mFolder.isExcludedFromFreeBusy(); }

    public boolean getIsIMAPSubscribed() { return mFolder.isIMAPSubscribed(); }

    /**
     * range 0-127; defaults to 0 if not present; client can display only 0-7
     * 
     * @return color
     */
    public String getColor() { return mFolder.getColor().name(); }

    /**
     * remote URL (RSS, iCal, etc) this folder syncs to
     * 
     * @return
     */
    public String getRemoteURL() { return mFolder.getRemoteURL(); }
    
    /**
     * for remote folders, the access rights the authenticated user has on the folder.
     * 
     * @return
     */
    public String getEffectivePerm() { return mFolder.getEffectivePerm(); }
    
    /**
     * url to the folder on rest interface for rest-enabled apps (such as wiki and notebook)
     * 
     * @return URL, if returned from server.
     */
    public String getRestURL() { return mFolder.getRestURL(); }
    
    /**
     * return grants or empty list if no grants
     */
    public List<ZGrant> getGrants() { return mFolder.getGrants(); }

    /**
     * @return sub folders, or empty list if no sub folders
     */
    public List<ZFolder> getSubFolders() { return mFolder.getSubFolders(); }

    public boolean getIsSearchFolder() { return mFolder instanceof ZSearchFolder; }
    
    public String getQuery() { return getIsSearchFolder() ? ((ZSearchFolder) mFolder).getQuery() : ""; }
    
    //public ZFolder getSubFolderByPath(String path);
    
    public boolean getIsInbox() { return mFolder.getId().equals(ZFolder.ID_INBOX); }
    public boolean getIsTrash() { return mFolder.getId().equals(ZFolder.ID_TRASH); }
    public boolean getIsSpam() { return mFolder.getId().equals(ZFolder.ID_SPAM); }
    public boolean getIsSent() { return mFolder.getId().equals(ZFolder.ID_SENT); }    
    public boolean getIsDrafts() { return mFolder.getId().equals(ZFolder.ID_DRAFTS); }
    public boolean getIsContacts() { return mFolder.getId().equals(ZFolder.ID_CONTACTS); }
    public boolean getIsCalendar() { return mFolder.getId().equals(ZFolder.ID_CALENDAR); }    
    public boolean getIsNotebook() { return mFolder.getId().equals(ZFolder.ID_NOTEBOOK); }    
    public boolean getIsAutoContacts() { return mFolder.getId().equals(ZFolder.ID_AUTO_CONTACTS); }
    
    public boolean getIsMailView() { 
        ZFolder.View view = mFolder.getDefaultView();
        return view == null || view == ZFolder.View.message || view == ZFolder.View.conversation;
    }
    
    public boolean getIsNullView() { return mFolder.getDefaultView() == null; }    
    public boolean getIsMessageView() { return mFolder.getDefaultView() == ZFolder.View.message; }
    public boolean getIsContactView() { return mFolder.getDefaultView() == ZFolder.View.contact; }    
    public boolean getIsConversationView() { return mFolder.getDefaultView() == ZFolder.View.conversation; }        
    public boolean getIsAppointmentView() { return mFolder.getDefaultView() == ZFolder.View.appointment; }
    public boolean getIsWikiView() { return mFolder.getDefaultView() == ZFolder.View.wiki; }
    
    public boolean getIsSystemFolder() { return mFolder.isSystemFolder(); }
}
