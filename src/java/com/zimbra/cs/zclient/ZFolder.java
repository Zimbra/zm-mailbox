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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

public class ZFolder implements ZItem {
    
    public static final String ID_USER_ROOT = "1";
    public static final String ID_INBOX = "2";
    public static final String ID_TRASH = "3";
    public static final String ID_SPAM = "4";
    public static final String ID_SENT = "5";
    public static final String ID_DRAFTS = "6";
    public static final String ID_CONTACTS = "7";
    public static final String ID_TAGS = "8";
    public static final String ID_CONVERSATIONS = "9";
    public static final String ID_CALENDAR = "10";
    public static final String ID_ROOT = "11";
    public static final String ID_NOTEBOOK = "12";
    public static final String ID_AUTO_CONTACTS = "13";
    public static final String ID_FIRST_USER_ID = "256";

    private ZFolder.Color mColor;
    private String mId;
    private String mName;
    private int mUnreadCount;
    private View mDefaultView;
    private String mFlags;
    private int mMessageCount;
    private String mParentId;
    private String mRestURL;
    private String mRemoteURL;
    private String mEffectivePerms;
    private List<ZGrant> mGrants;
    private List<ZFolder> mSubFolders;
    private ZFolder mParent;
    
    public enum Flag {
        checkedInUI('#'),
        excludeFreeBusyInfo('b'),
        imapSubscribed('*');

        private char mFlagChar;
        
        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";            
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }
        
        Flag(char flagChar) {
            mFlagChar = flagChar;
            
        }
    }

    public enum Color {
        
        orange(0),
        blue(1),
        cyan(2), 
        green(3),
        purple(4),
        red(5),
        yellow(6),
        pink(7),
        gray(8);
        
        private int mValue;

        public int getValue() { return mValue; }

        public static Color fromString(String s) throws ServiceException {
            try {
                return Color.values()[Integer.parseInt(s)];
            } catch (NumberFormatException e) {
            } catch (IndexOutOfBoundsException e) {
            }
            
            try {
                return Color.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid color: "+s+", valid values: "+Arrays.asList(Color.values()), e);
            }
        }

        Color(int value) { mValue = value; } 
    }

    public enum View {
        
        appointment,
        contact,
        conversation,
        message,
        wiki;

        public static View fromString(String s) throws ServiceException {
            try {
                return View.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid view: "+s+", valid values: "+Arrays.asList(View.values()), e);                
            }
        }
    }
    
    public ZFolder(Element e, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        mParent = parent;
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mParentId = e.getAttribute(MailService.A_FOLDER);
        mFlags = e.getAttribute(MailService.A_FLAGS, null);
        try {
            mColor = ZFolder.Color.fromString(e.getAttribute(MailService.A_COLOR, "0"));
        } catch (ServiceException se) {
            mColor = ZFolder.Color.orange;
        }
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, 0);
        mMessageCount = (int) e.getAttributeLong(MailService.A_NUM, 0);
        mDefaultView = View.fromString(e.getAttribute(MailService.A_DEFAULT_VIEW, View.conversation.name()));
        mRestURL = e.getAttribute(MailService.A_REST_URL, null);
        mRemoteURL = e.getAttribute(MailService.A_URL, null);
        mEffectivePerms = e.getAttribute(MailService.A_RIGHTS, null);
        
        mGrants = new ArrayList<ZGrant>();            
        mSubFolders = new ArrayList<ZFolder>();

        Element aclEl = e.getOptionalElement(MailService.E_ACL);

        if (aclEl != null) {
            for (Element grant : aclEl.listElements(MailService.E_GRANT)) {
                mGrants.add(new ZGrant(grant));
            }
        }

        // sub folders
        for (Element child : e.listElements(MailService.E_FOLDER))
            new ZFolder(child, this, mailbox);
        
        // search
        for (Element s : e.listElements(MailService.E_SEARCH))
            new ZSearchFolder(s, this, mailbox);
        
        // link
        for (Element l : e.listElements(MailService.E_MOUNT))
            new ZMountpoint(l, this, mailbox);

        mailbox.addItemIdMapping(this);
        if (parent != null) parent.addChild(this);
    }

    void addChild(ZFolder folder)        { mSubFolders.add(folder); }
    
    void removeChild(ZFolder folder)       { mSubFolders.remove(folder); }

    public void modifyNotification(Element e, ZMailbox mbox) throws ServiceException {
        mName = e.getAttribute(MailService.A_NAME, mName);
        String oldParentId = mParentId;
        mParentId = e.getAttribute(MailService.A_FOLDER, mParentId);
        if (mParentId != oldParentId) {
            //re-compute mParent!
            mParent = mbox.getFolderById(mParentId);
        }
        mFlags = e.getAttribute(MailService.A_FLAGS, mFlags);
        String newColor = e.getAttribute(MailService.A_COLOR, null);
        if (newColor != null) {
            try {
                mColor = ZFolder.Color.fromString(newColor);
            } catch (ServiceException se) {
            }
        }
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, mUnreadCount);
        mMessageCount = (int) e.getAttributeLong(MailService.A_NUM, mMessageCount);
        String newView = e.getAttribute(MailService.A_DEFAULT_VIEW, null);
        if (newView != null) mDefaultView = View.fromString(newView);
        mRestURL = e.getAttribute(MailService.A_REST_URL, mRestURL);
        mRemoteURL = e.getAttribute(MailService.A_URL, mRemoteURL);
        mEffectivePerms = e.getAttribute(MailService.A_RIGHTS, mEffectivePerms);
        
        Element aclEl = e.getOptionalElement(MailService.E_ACL);
        if (aclEl != null) {
            mGrants.clear();
            for (Element grant : aclEl.listElements(MailService.E_GRANT)) {
                mGrants.add(new ZGrant(grant));
            }
        }
        // TODO: sub/search/link?
        
    }
        
    public ZFolder getParent() {
        return mParent;
    }

    public String getId() {
        return mId;
    }

    /** Returns the folder's name.  Note that this is the folder's
     *  name (e.g. <code>"foo"</code>), not its absolute pathname
     *  (e.g. <code>"/baz/bar/foo"</code>).
     * 
     * @see #getPath() 
     * 
     */
    public String getName() {
        return mName;
    }
    
    /** Returns the folder's absolute path.  Paths are UNIX-style with 
     *  <code>'/'</code> as the path delimiter.  Paths are relative to
     *  the user root folder,
     *  which has the path <code>"/"</code>.  So the Inbox's path is
     *  <code>"/Inbox"</code>, etc.
     */
    public String getPath() {
        // TODO: CACHE? compute upfront?
        if (mParent == null)
            return ZMailbox.PATH_SEPARATOR;
        else {
            String pp = mParent.getPath();
            return pp.length() == 1 ? (pp + mName) : (pp + ZMailbox.PATH_SEPARATOR + mName);
        }
    }

    /** Returns the folder's absolute path, with special chars in the names
     * URL encoded.
     */
    public String getPathUrlEncoded() {
        // TODO: CACHE? compute upfront?
        if (mParent == null)
            return ZMailbox.PATH_SEPARATOR;
        else {
            String pp = mParent.getPath();
            String n;
            try {
                n = URLEncoder.encode(mName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // should never happen
                n = mName;
            }
            return pp.length() == 1 ? (pp + n) : (pp + ZMailbox.PATH_SEPARATOR + n);
        }       
    }

    /**
     * 
     * @return parent id of folder, or null if root folder.
     */
    public String getParentId() {
        return mParentId;
    }

    /**
     * @return number of unread items in folder
     */
    public int getUnreadCount() {
        return mUnreadCount; 
    }

    /**
     * @return number of unread items in folder
     */
    public int getMessageCount() {
        return mMessageCount;
    }
    
    /** Returns the "hint" as to which view to use to display the folder's
     *  contents.
     */
    public View getDefaultView() {
        return mDefaultView;
    }
    
    /**
     *  checked in UI (#), exclude free/(b)usy info, IMAP subscribed (*)
     */
    public String getFlags() {
        return mFlags;
    }
    
    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean isCheckedInUI() {
        return hasFlags() && mFlags.indexOf(Flag.checkedInUI.getFlagChar()) != -1;
    }

    public boolean isExcludedFromFreeBusy() {
        return hasFlags() && mFlags.indexOf(Flag.excludeFreeBusyInfo.getFlagChar()) != -1;
    }

    public boolean isIMAPSubscribed() {
        return hasFlags() && mFlags.indexOf(Flag.imapSubscribed.getFlagChar()) != -1;
    }    

    /**
     * range 0-127; defaults to 0 if not present; client can display only 0-7
     * 
     * @return color
     */
    public Color getColor() {
        return mColor;
    }

    /**
     * remote URL (RSS, iCal, etc) this folder syncs to
     * 
     * @return
     */
    public String getRemoteURL() {
        return mRemoteURL;
    }
    
    /**
     * for remote folders, the access rights the authenticated user has on the folder.
     * 
     * @return
     */
    public String getEffectivePerm() {
        return mEffectivePerms;
    }
    
    /**
     * url to the folder on rest interface for rest-enabled apps (such as wiki and notebook)
     * 
     * @return URL, if returned from server.
     */
    public String getRestURL() {
        return mRestURL;
    }
    
    /**
     * return grants or empty list if no grants
     */
    public List<ZGrant> getGrants() {
        return mGrants;
    }

    /**
     * @return sub folders, or empty list if no sub folders
     */
    public List<ZFolder> getSubFolders() {
        return mSubFolders;
    }

    /**
     * return sub folder with specified path. Path must not start with the mailbox path separator. 
     * @param path
     * @return sub folder of this folder, 
     */
    public ZFolder getSubFolderByPath(String path) {
        if (path.length() == 0) return this;
        int index = path.indexOf(ZMailbox.PATH_SEPARATOR);
        String name = index == -1 ? path : path.substring(0, index);
        String subpath = index == -1 ? null : path.substring(index+1);
        for (ZFolder f: getSubFolders()) {
            if (f.getName().equalsIgnoreCase(name)) {
                return (subpath == null) ? f : f.getSubFolderByPath(subpath);
            }
        }
        return null;
    }

    public boolean isSystemFolder() {
        try {
            return Integer.parseInt(mId) < Integer.parseInt(ZFolder.ID_FIRST_USER_ID);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    protected void toStringCommon(ZSoapSB sb) {
        sb.add("id", mId);
        sb.add("name", mName);
        sb.add("path", getPath());
        sb.add("parentId", mParentId);
        sb.add("flags", mFlags);
        sb.add("color", mColor.name());
        sb.add("unreadCount", mUnreadCount);
        sb.add("messageCount", mMessageCount);
        sb.add("view", mDefaultView.name());
        sb.add("restURL", mRestURL);
        sb.add("url", mRemoteURL);
        sb.add("effectivePermissions", mEffectivePerms);
        sb.add("grants", mGrants, false, false);
        sb.add("children", mSubFolders, false, false);
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        toStringCommon(sb);
        sb.endStruct();
        return sb.toString();
    }

}
