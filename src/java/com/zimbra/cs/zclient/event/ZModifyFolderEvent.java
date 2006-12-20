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

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZFolder.Color;
import com.zimbra.cs.zclient.ZFolder.View;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZSoapSB;
import com.zimbra.soap.Element;

import java.util.List;
import java.util.ArrayList;

public class ZModifyFolderEvent implements ZModifyItemEvent {

    protected Element mFolderEl;

    public ZModifyFolderEvent(Element e) throws ServiceException {
        mFolderEl = e;
    }

    /**
     * @return folder id of modified folder 
     * @throws ServiceException
     */
    public String getId() throws ServiceException {
        return mFolderEl.getAttribute(MailService.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getName(String defaultValue) {
        return mFolderEl.getAttribute(MailService.A_NAME, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new parent id or defaultValue if parent didn't change
     */
    public String getParentId(String defaultValue) {
        return mFolderEl.getAttribute(MailService.A_FOLDER, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new flags or default value if flags didn't change
     */
    public String getFlags(String defaultValue) {
        return mFolderEl.getAttribute(MailService.A_FLAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new color, or default value.
     */
    public Color getColor(Color defaultValue) {
        String newColor = mFolderEl.getAttribute(MailService.A_COLOR, null);
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
     * @return new unread count, or defaultVslue if unchanged
     * @throws ServiceException on error
     */
    public int getUnreadCount(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailService.A_UNREAD, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new message count, or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public int getMessageCount(int defaultValue) throws ServiceException {
        return (int) mFolderEl.getAttributeLong(MailService.A_NUM, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new default view, or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public View getDefaultView(View defaultValue) throws ServiceException {
        String newView = mFolderEl.getAttribute(MailService.A_DEFAULT_VIEW, null);
        return (newView != null) ? View.fromString(newView) : defaultValue;        
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new rest URL or defaultValue if unchanged
     */
    public String getRestURL(String defaultValue) {
        return mFolderEl.getAttribute(MailService.A_REST_URL, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new remote URL or defaultValue if unchanged
     */
    public String getRemoteURL(String defaultValue) {
        return mFolderEl.getAttribute(MailService.A_URL, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new effective perms or defaultValue if unchanged
     */
    public String getEffectivePerm(String defaultValue) {
        return mFolderEl.getAttribute(MailService.A_RIGHTS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return grants or defaultValue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public List<ZGrant> getGrants(List<ZGrant> defaultValue) throws ServiceException {
        Element aclEl = mFolderEl.getOptionalElement(MailService.E_ACL);
        if (aclEl != null) {
            List<ZGrant> grants = new ArrayList<ZGrant>();
            for (Element grant : aclEl.listElements(MailService.E_GRANT)) {
                grants.add(new ZGrant(grant));
            }
            return grants;
        }
        return defaultValue;
    }

    protected void toStringCommon(ZSoapSB sb) throws ServiceException {
        sb.add("id", getId());
        if (getName(null) != null) sb.add("name", getName(null));
        if (getParentId(null) != null) sb.add("parentId", getParentId(null));
        if (getFlags(null) != null) sb.add("flags", getFlags(null));
        if (getColor(null) != null) sb.add("color", getColor(null).name());
        if (getUnreadCount(-1) != -1) sb.add("unreadCount", getUnreadCount(-1));
        if (getMessageCount(-1) != -1) sb.add("messageCount", getMessageCount(-1));
        if (getDefaultView(null) != null) sb.add("view", getDefaultView(null).name());
        if (getRestURL(null) != null) sb.add("restURL", getRestURL(null));
        if (getRemoteURL(null) != null) sb.add("url", getRemoteURL(null));
        if (getEffectivePerm(null) != null) sb.add("effectivePermissions", getEffectivePerm(null));
        List<ZGrant> grants = getGrants(null);
        if (grants != null) sb.add("grants", grants, false, false);
    }
    
    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            toStringCommon(sb);
            sb.endStruct();
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }
}

