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

package com.zimbra.client.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZJSONObject;
import com.zimbra.client.ZTag.Color;
import com.zimbra.soap.mail.type.RetentionPolicy;

import org.json.JSONException;

public class ZModifyTagEvent implements ZModifyItemEvent, ToZJSONObject {

    protected Element mTagEl;

    public ZModifyTagEvent(Element e) {
        mTagEl = e;
    }

    /**
     * @return folder id of modified tag
     * @throws com.zimbra.common.service.ServiceException
     */
    public String getId() throws ServiceException {
        return mTagEl.getAttribute(MailConstants.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getName(String defaultValue) {
        return mTagEl.getAttribute(MailConstants.A_NAME, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new color, or default value.
     */
    public Color getColor(Color defaultValue) {
        String newColor = mTagEl.getAttribute(MailConstants.A_RGB, null);
        if (newColor != null) {
                return Color.rgbColor.setRgbColor(newColor);
        } else {
            String s = mTagEl.getAttribute(MailConstants.A_COLOR, null);
            if (s != null) {
                try {
                    return Color.values()[(byte)Long.parseLong(s)];
                } catch (NumberFormatException se) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /**
     * Returns the modified retention policy, or {@code defaultValue} if it hasn't
     * been modified.
     */
    public RetentionPolicy getRetentionPolicy(RetentionPolicy defaultValue) throws ServiceException {
        Element rpEl = mTagEl.getOptionalElement(MailConstants.E_RETENTION_POLICY);
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
        return (int) mTagEl.getAttributeLong(MailConstants.A_UNREAD, defaultValue);
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", getId());
            String name = getName(null);
            if (name != null) zjo.put("name", name);
            if (getColor(null) != null) zjo.put("color", getColor(null).name());
            if (getUnreadCount(-1) != -1) zjo.put("unreadCount", getUnreadCount(-1));
            return zjo;
        } catch (ServiceException se) {
            throw new JSONException(se);
        }
    }

    public String toString() {
        try {
            return String.format("[ZModifyTagEvent %s]", getId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
