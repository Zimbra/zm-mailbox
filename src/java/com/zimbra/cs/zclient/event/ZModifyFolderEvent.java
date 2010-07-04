/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.ToZJSONObject;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZFolder.Color;
import com.zimbra.cs.zclient.ZFolder.View;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZJSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ZModifyFolderEvent implements ZModifyItemEvent, ToZJSONObject {

    protected Element mFolderEl;

    public ZModifyFolderEvent(Element e) {
        mFolderEl = e;
    }

    /**
     * @return folder id of modified folder 
     * @throws ServiceException
     */
    public String getId() throws ServiceException {
        return mFolderEl.getAttribute(MailConstants.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getName(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_NAME, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new parent id or defaultValue if parent didn't change
     */
    public String getParentId(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_FOLDER, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new flags or default value if flags didn't change
     */
    public String getFlags(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_FLAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new color, or default value.
     */
    public Color getColor(Color defaultValue) {
        String newColor = mFolderEl.getAttribute(MailConstants.A_COLOR, null);
        if (newColor != null) {
            try {
                return ZFolder.Color.fromString(newColor);
            } catch (ServiceException se) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new unread count, or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public int getUnreadCount(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_UNREAD, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new unread count (including IMAP \Deleted items), or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public int getImapUnreadCount(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_IMAP_UNREAD, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new size count, or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public long getSize(long defaultValue) throws ServiceException {
        return mFolderEl.getAttributeLong(MailConstants.A_SIZE, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new message count, or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public int getMessageCount(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_NUM, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new message count (including IMAP \Deleted ones), or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public int getImapMessageCount(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_IMAP_NUM, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new default view, or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public View getDefaultView(View defaultValue) throws ServiceException {
        String newView = mFolderEl.getAttribute(MailConstants.A_DEFAULT_VIEW, null);
        return (newView != null) ? View.fromString(newView) : defaultValue;
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new metadata sequence or defaultValue if unchanged
     * @throws ServiceException
     */
    public int getModifiedSequence(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new content sequence or defaultValue if unchanged
     * @throws ServiceException
     */
    public int getContentSequence(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_REVISION, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new IMAP UIDNEXT or defaultValue if unchanged
     * @throws ServiceException
     */
    public int getImapUIDNEXT(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_IMAP_UIDNEXT, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new IMAP MODSEQ or defaultValue if unchanged
     * @throws ServiceException
     */
    public int getImapMODSEQ(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailConstants.A_IMAP_MODSEQ, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new remote URL or defaultValue if unchanged
     */
    public String getRemoteURL(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_URL, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new effective perms or defaultValue if unchanged
     */
    public String getEffectivePerm(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_RIGHTS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return grants or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public List<ZGrant> getGrants(List<ZGrant> defaultValue) throws ServiceException {
        Element aclEl = mFolderEl.getOptionalElement(MailConstants.E_ACL);
        if (aclEl != null) {
            List<ZGrant> grants = new ArrayList<ZGrant>();
            for (Element grant : aclEl.listElements(MailConstants.E_GRANT)) {
                grants.add(new ZGrant(grant));
            }
            return grants;
        }
        return defaultValue;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject zjo = new ZJSONObject ();
            zjo.put("id", getId());
            if (getName(null) != null) zjo.put("name", getName(null));
            if (getParentId(null) != null) zjo.put("parentId", getParentId(null));
            if (getFlags(null) != null) zjo.put("flags", getFlags(null));
            if (getColor(null) != null) zjo.put("color", getColor(null).name());
            if (getUnreadCount(-1) != -1) zjo.put("unreadCount", getUnreadCount(-1));
            if (getImapUnreadCount(-1) != -1) zjo.put("imapUnreadCount", getImapUnreadCount(-1));
            if (getMessageCount(-1) != -1) zjo.put("messageCount", getMessageCount(-1));
            if (getImapMessageCount(-1) != -1) zjo.put("imapMessageCount", getImapMessageCount(-1));
            if (getDefaultView(null) != null) zjo.put("view", getDefaultView(null).name());
            if (getImapUIDNEXT(-1) != -1) zjo.put("imapUIDNEXT", getImapUIDNEXT(-1));
            if (getImapMODSEQ(-1) != -1) zjo.put("imapMODSEQ", getImapMODSEQ(-1));
            if (getRemoteURL(null) != null) zjo.put("url", getRemoteURL(null));
            if (getEffectivePerm(null) != null) zjo.put("effectivePermissions", getEffectivePerm(null));
            List<ZGrant> grants = getGrants(null);
            if (grants != null) zjo.put("grants", grants);
            return zjo;
        } catch(ServiceException se) {
            throw new JSONException(se);
        }
    }
    
    @Override public String toString() {
        try {
            return String.format("[ZModifyFolderEvent %s]", getId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}

