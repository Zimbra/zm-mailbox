/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.client.event;

import org.json.JSONException;

import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZJSONObject;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.RetentionPolicy;

public class ZModifySmartFolderEvent implements ZModifyItemEvent, ToZJSONObject {

    protected Element mSmartFolderEl;

    public ZModifySmartFolderEvent(Element e) {
        mSmartFolderEl = e;
    }

    /**
     * @return folder id of modified smart folder
     * @throws com.zimbra.common.service.ServiceException
     */
    @Override
    public String getId() throws ServiceException {
        return mSmartFolderEl.getAttribute(MailConstants.A_ID);
    }

    /**
     * Returns the modified retention policy, or {@code defaultValue} if it hasn't
     * been modified.
     */
    public RetentionPolicy getRetentionPolicy(RetentionPolicy defaultValue) throws ServiceException {
        Element rpEl = mSmartFolderEl.getOptionalElement(MailConstants.E_RETENTION_POLICY);
        if (rpEl == null) {
            return defaultValue;
        }
        return new RetentionPolicy(rpEl);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new unread count, or defaultVslue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public int getUnreadCount(int defaultValue) throws ServiceException {
        return (int) mSmartFolderEl.getAttributeLong(MailConstants.A_UNREAD, defaultValue);
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", getId());
            if (getUnreadCount(-1) != -1) zjo.put("unreadCount", getUnreadCount(-1));
            return zjo;
        } catch (ServiceException se) {
            throw new JSONException(se);
        }
    }

    @Override
    public String toString() {
        try {
            return String.format("[ZModifySmartFolderEvent %s]", getId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
