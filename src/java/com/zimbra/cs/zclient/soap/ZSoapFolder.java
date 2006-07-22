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

package com.zimbra.cs.zclient.soap;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.Element;

class ZSoapFolder implements ZFolder, ZSoapItem {

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
    
    ZSoapFolder(Element e, ZSoapFolder parent, ZSoapMailbox mailbox) throws ServiceException {
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
                mGrants.add(new ZSoapGrant(grant));
            }
        }

        // sub folders
        for (Element child : e.listElements(MailService.E_FOLDER))
            new ZSoapFolder(child, this, mailbox);
        
        // search
        for (Element s : e.listElements(MailService.E_SEARCH))
            new ZSoapSearchFolder(s, this, mailbox);
        
        // link
        for (Element l : e.listElements(MailService.E_MOUNT))
            new ZSoapMountpoint(l, this, mailbox);

        mailbox.addItemIdMapping(this);
        if (parent != null) parent.addChild(this);
    }

    void modifyNotification(Element e, ZMailbox mbox) throws ServiceException {
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
                mGrants.add(new ZSoapGrant(grant));
            }
        }
        // TODO: sub/search/link?
        
    }

    void addChild(ZFolder folder)        { mSubFolders.add(folder); }
    
    void removeChild(ZFolder folder)       { mSubFolders.remove(folder); }

    public ZFolder getParent() {
        return mParent;
    }

    public List<ZGrant> getGrants() {
        return mGrants;
    }

    public ZFolder.Color getColor() {
        return mColor;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public int getUnreadCount() {
        return mUnreadCount;
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

    public View getDefaultView() {
        return mDefaultView;
    }

    public String getFlags() {
        return mFlags;
    }

    public int getMessageCount() {
        return mMessageCount;
    }

    public String getParentId() {
        return mParentId;
    }

    public String getPath() {
        // TODO: CACHE? compute upfront?
        if (mParent == null)
            return ZMailbox.PATH_SEPARATOR;
        else {
            String pp = mParent.getPath();
            return pp.length() == 1 ? (pp + mName) : (pp + ZMailbox.PATH_SEPARATOR + mName);
        }
    }

    public String getEffectivePerm() {
        return mEffectivePerms;
    }

    public String getRemoteURL() {
        return mRemoteURL;
    }

    public String getRestURL() {
        return mRestURL;
    }

    public List<ZFolder> getSubFolders() {
        return mSubFolders;
    }

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

}
