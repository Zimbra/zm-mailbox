/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyFolderEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZFolder implements ZItem, Comparable {
    
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
    public static final String ID_CHATS = "14";
    public static final String ID_TASKS = "15";
    public static final String ID_FIRST_USER_ID = "256";

    private ZFolder.Color mColor;
    private String mId;
    private String mName;
    private int mUnreadCount;
    private long mSize;
    private View mDefaultView;
    private String mFlags;
    private int mMessageCount;
    private String mParentId;
    private int mContentSequence;
    private String mRemoteURL;
    private String mEffectivePerms;
    private List<ZGrant> mGrants;
    private List<ZFolder> mSubFolders;
    private ZFolder mParent;
    private boolean mIsPlaceholder;

    public int compareTo(Object obj) {
        if (!(obj instanceof ZFolder))
            return 0;
        ZFolder other = (ZFolder) obj;
        return getName().compareToIgnoreCase(other.getName());
    }

    public enum Flag {
        checkedInUI('#'),
        doesNotInheritPermissions('i'),
        excludeFreeBusyInfo('b'),
        imapDeleted('x'),
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
        
        defaultColor(0),
        blue(1),
        cyan(2), 
        green(3),
        purple(4),
        red(5),
        yellow(6),
        pink(7),
        gray(8),
        orange(9);
        
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
        document,
        message,
        wiki,
        task,
        voice;

        public static View fromString(String s) throws ServiceException {
            try {
                return View.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid view: "+s+", valid values: "+Arrays.asList(View.values()), e);                
            }
        }

        public boolean isAppointment() { return equals(appointment); }
        public boolean isContact() { return equals(contact); }
        public boolean isConversation() { return equals(conversation); }
        public boolean isDocument() { return equals(document); }
        public boolean isMessage() { return equals(message); }
        public boolean isWiki() { return equals(wiki); }
        public boolean isTask() { return equals(task); }

    }
    
    public ZFolder(Element e, ZFolder parent) throws ServiceException {
        mParent = parent;
        mId = e.getAttribute(MailConstants.A_ID);
        mName = e.getAttribute(MailConstants.A_NAME);
        mParentId = e.getAttribute(MailConstants.A_FOLDER, null);
        mIsPlaceholder = mParentId == null;
        mFlags = e.getAttribute(MailConstants.A_FLAGS, null);
        try {
            mColor = ZFolder.Color.fromString(e.getAttribute(MailConstants.A_COLOR, "0"));
        } catch (ServiceException se) {
            mColor = ZFolder.Color.orange;
        }
        mUnreadCount = (int) e.getAttributeLong(MailConstants.A_UNREAD, 0);
        mMessageCount = (int) e.getAttributeLong(MailConstants.A_NUM, 0);
        mDefaultView = View.fromString(e.getAttribute(MailConstants.A_DEFAULT_VIEW, View.conversation.name()));
        mContentSequence = (int) e.getAttributeLong(MailConstants.A_REVISION, -1);
        mRemoteURL = e.getAttribute(MailConstants.A_URL, null);
        mEffectivePerms = e.getAttribute(MailConstants.A_RIGHTS, null);
        mSize = e.getAttributeLong(MailConstants.A_SIZE, 0);
        
        mGrants = new ArrayList<ZGrant>();
        mSubFolders = new ArrayList<ZFolder>();

        Element aclEl = e.getOptionalElement(MailConstants.E_ACL);

        if (aclEl != null) {
            for (Element grant : aclEl.listElements(MailConstants.E_GRANT)) {
                mGrants.add(new ZGrant(grant));
            }
        }

        // sub folders
        for (Element child : e.listElements(MailConstants.E_FOLDER))
            mSubFolders.add(createSubFolder(child));

        // search
        for (Element s : e.listElements(MailConstants.E_SEARCH))
            mSubFolders.add(new ZSearchFolder(s, this));
        
        // link
        for (Element l : e.listElements(MailConstants.E_MOUNT))
            mSubFolders.add(new ZMountpoint(l, this));
    }

    protected ZFolder createSubFolder(Element element) throws ServiceException {
        return new ZFolder(element, this);
    }

    synchronized void addChild(ZFolder folder) {
        List<ZFolder> newSubs = new ArrayList<ZFolder>(mSubFolders);
        newSubs.add(folder);
        mSubFolders = newSubs;
    }
    
    synchronized void removeChild(ZFolder folder) {
        List<ZFolder> newSubs = new ArrayList<ZFolder>(mSubFolders);
        newSubs.remove(folder);
        mSubFolders = newSubs;
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
        if (event instanceof ZModifyFolderEvent) {
            ZModifyFolderEvent fevent = (ZModifyFolderEvent) event;
            if (fevent.getId().equals(mId)) {
                mName = fevent.getName(mName);
                mParentId = fevent.getParentId(mParentId);
                mFlags = fevent.getFlags(mFlags);
                mColor = fevent.getColor(mColor);
                mUnreadCount = fevent.getUnreadCount(mUnreadCount);
                mMessageCount = fevent.getMessageCount(mMessageCount);
                mDefaultView = fevent.getDefaultView(mDefaultView);
                mContentSequence = fevent.getContentSequence(mContentSequence);
                mRemoteURL = fevent.getRemoteURL(mRemoteURL);
                mEffectivePerms = fevent.getEffectivePerm(mEffectivePerms);
                mGrants = fevent.getGrants(mGrants);
                mSize = fevent.getSize(mSize);
            }
        }
    }
        
    public ZFolder getParent() {
        return mParent;
    }

    void setParent(ZFolder parent) {
        mParent = parent;
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

     /** Returns the folder's  path relative to the root
     * @return path
     */
    public String getRootRelativePath() {
        String path = getPath();
        if (path.startsWith(ZMailbox.PATH_SEPARATOR)) {
            return path.substring(ZMailbox.PATH_SEPARATOR.length());
        } else {
            return path;
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
     * @return <tt>true</tt> if this is just a shim for setting up hierarchy namespace for subfolders,
     * <tt>false</tt> if it's a real folder visible to the user
     */
    boolean isHierarchyPlaceholder() {
        return mIsPlaceholder;
    }

    /**
     * @return number of unread items in folder
     */
    public int getUnreadCount() {
        return mUnreadCount; 
    }

    /**
     *
     * @return size of all items in folder (not including sub folders) in bytes
     */

    public long getSize() {
        return mSize;
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

    /** Returns the sequence number for the last change in the folder's location or name.
     */
    public int getContentSequence() {
        return mContentSequence;
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

    public boolean isIMAPDeleted() {
        return hasFlags() && mFlags.indexOf(Flag.imapDeleted.getFlagChar()) != -1;
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
    public String getEffectivePerms() {
        return mEffectivePerms;
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
                if (subpath != null) return f.getSubFolderByPath(subpath);
                else return (f.isHierarchyPlaceholder() ? null : f);
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
