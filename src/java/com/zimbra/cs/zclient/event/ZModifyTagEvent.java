/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZTag.Color;
import com.zimbra.cs.zclient.ZSoapSB;
import com.zimbra.soap.Element;

public class ZModifyTagEvent implements ZModifyItemEvent {

    protected Element mTagEl;

    public ZModifyTagEvent(Element e) throws ServiceException {
        mTagEl = e;
    }

    /**
     * @return folder id of modified tag
     * @throws com.zimbra.common.service.ServiceException
     */
    public String getId() throws ServiceException {
        return mTagEl.getAttribute(MailService.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getName(String defaultValue) {
        return mTagEl.getAttribute(MailService.A_NAME, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new color, or default value.
     */
    public Color getColor(Color defaultValue) {
        String newColor = mTagEl.getAttribute(MailService.A_COLOR, null);
        if (newColor != null) {
            try {
                return Color.fromString(newColor);
            } catch (ServiceException se) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new unread count, or defaultVslue if unchanged
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public int getUnreadCount(int defaultValue) throws ServiceException {
        return (int) mTagEl.getAttributeLong(MailService.A_UNREAD, defaultValue);
    }

    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            sb.add("id", getId());
            String name = getName(null);
            if (name != null) sb.add("name", name);
            if (getColor(null) != null) sb.add("color", getColor(null).name());
            if (getUnreadCount(-1) != -1) sb.add("unreadCount", getUnreadCount(-1));
            sb.endStruct();
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }
}
