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
import com.zimbra.cs.zclient.ZLink;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.Element;

class ZSoapFolder implements ZFolder, ZSoapItem {

    private int mColor;
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
    private List<ZLink> mLinks;        
    private ZFolder mParent;
    
    ZSoapFolder(Element e, ZSoapFolder parent, ZSoapMailbox mailbox) throws ServiceException {
        mParent = parent;
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mParentId = e.getAttribute(MailService.A_FOLDER);
        mFlags = e.getAttribute(MailService.A_FLAGS, "");
        mColor = (int) e.getAttributeLong(MailService.A_COLOR, 0);
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, 0);
        mMessageCount = (int) e.getAttributeLong(MailService.A_NUM, 0);
        mDefaultView = View.fromString(e.getAttribute(MailService.A_DEFAULT_VIEW, View.conversation.name()));
        mRestURL = e.getAttribute(MailService.A_REST_URL, null);
        mRemoteURL = e.getAttribute(MailService.A_URL, null);
        mEffectivePerms = e.getAttribute(MailService.A_RIGHTS, null);
        
        mGrants = new ArrayList<ZGrant>();            
        mSubFolders = new ArrayList<ZFolder>();
        mLinks = new ArrayList<ZLink>();        

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
            new ZSoapLink(l, this, mailbox);

        mailbox.addItemIdMapping(this);
        if (parent != null) parent.addChild(this);
    }

    void modifyNotification(Element e) throws ServiceException {
        mName = e.getAttribute(MailService.A_NAME, mName);
        mParentId = e.getAttribute(MailService.A_FOLDER, mParentId); // TODO: re-compute mParent!
        mFlags = e.getAttribute(MailService.A_FLAGS, mFlags);
        mColor = (int) e.getAttributeLong(MailService.A_COLOR, mColor);
        mUnreadCount = (int) e.getAttributeLong(MailService.A_UNREAD, mUnreadCount);
        mMessageCount = (int) e.getAttributeLong(MailService.A_NUM, mMessageCount);
        String newView = e.getAttribute(MailService.A_DEFAULT_VIEW, null);
        if (newView != null) mDefaultView = View.fromString(newView);
        mRestURL = e.getAttribute(MailService.A_REST_URL, mRestURL);
        mRemoteURL = e.getAttribute(MailService.A_URL, mRemoteURL);
        mEffectivePerms = e.getAttribute(MailService.A_RIGHTS, mEffectivePerms);
        // TODO: ACLs, sub/search/link?
    }

    void addChild(ZFolder folder)        { mSubFolders.add(folder); }
    void addChild(ZLink link)            { mLinks.add(link); }
    
    void removeChild(ZFolder folder)       { mSubFolders.remove(folder); }
    void removeChild(ZLink link)           { mLinks.remove(link); }

    public ZFolder getParent() {
        return mParent;
    }

    public List<ZGrant> getGrants() {
        return mGrants;
    }

    public int getColor() {
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
    
    public String toString() {
        return toString("folder", null);
    }
    
    protected String toString(String type, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (ZFolder child : mSubFolders) {
            sb.append("\n").append( ((ZSoapFolder)child).toString()).append("\n");
        }
        for (ZLink link : mLinks) {
            sb.append("\n").append(link).append("\n");
        }                
        sb.append("}");
        
        return String.format("%s: { id: %s, name: %s, parentId: %s, flags: %s, color: %d, unreadCount: %d, " +
                "messageCount: %d, view: %s, restURL: %s, url: %s, perms: %s, grants: %s, children: %s, path: %s%s} ",
                type,
                mId, mName, mParentId, mFlags, mColor, mUnreadCount, mMessageCount, mDefaultView, 
                mRestURL, mRemoteURL, mEffectivePerms, mGrants.toString(), sb.toString(), getPath(), 
                extra != null ? extra : ""); 
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
    
    public List<ZLink> getLinks() {
        return mLinks;
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

}
