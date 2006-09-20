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

import java.util.Arrays;
import java.util.List;

import com.zimbra.cs.service.ServiceException;

public interface ZFolder {
    
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

    public ZFolder getParent();

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

    /** Returns the folder's absolute path, with special chars in the names
     * URL encoded.
     */
    public String getPathUrlEncoded();

    /**
     * 
     * @return parent id of folder, or null if root folder.
     */
    public String getParentId();

    /**
     * @return number of unread items in folder
     */
    public int getUnreadCount();

    /**
     * @return number of unread items in folder
     */
    public int getMessageCount();
    
    /** Returns the "hint" as to which view to use to display the folder's
     *  contents.
     */
    public View getDefaultView();
    
    /**
     *  checked in UI (#), exclude free/(b)usy info, IMAP subscribed (*)
     */
    public String getFlags();

    public boolean hasFlags();
    
    public boolean isCheckedInUI();

    public boolean isExcludedFromFreeBusy();

    public boolean isIMAPSubscribed();

    /**
     * range 0-127; defaults to 0 if not present; client can display only 0-7
     * 
     * @return color
     */
    public Color getColor();

    /**
     * remote URL (RSS, iCal, etc) this folder syncs to
     * 
     * @return
     */
    public String getRemoteURL();
    
    /**
     * for remote folders, the access rights the authenticated user has on the folder.
     * 
     * @return
     */
    public String getEffectivePerm();
    
    /**
     * url to the folder on rest interface for rest-enabled apps (such as wiki and notebook)
     * 
     * @return URL, if returned from server.
     */
    public String getRestURL();
    
    /**
     * return grants or empty list if no grants
     */
    public List<ZGrant> getGrants();

    /**
     * @return sub folders, or empty list if no sub folders
     */
    public List<ZFolder> getSubFolders();

    /**
     * return sub folder with specified path. Path must not start with the mailbox path separator. 
     * @param path
     * @return sub folder of this folder, 
     */
    public ZFolder getSubFolderByPath(String path);

    public boolean isSystemFolder();
}
