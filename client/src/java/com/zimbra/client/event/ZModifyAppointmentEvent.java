/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZAppointment;
import com.zimbra.client.ZItem;
import com.zimbra.client.ZJSONObject;
import org.json.JSONException;

public class ZModifyAppointmentEvent implements ZModifyItemEvent, ZModifyItemFolderEvent, ToZJSONObject {

    protected Element mApptEl;

    public ZModifyAppointmentEvent(Element e) throws ServiceException {
        mApptEl = e;
    }

    /**
     * @return id
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String getId() throws ServiceException {
        return mApptEl.getAttribute(MailConstants.A_ID);
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", getId());
            return zjo;
        } catch (ServiceException e) {
            throw new JSONException(e);
        }
    }

    public String toString() {
        try {
            return String.format("[ZModifyAppointmentEvent %s]", getId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public ZItem getItem() throws ServiceException {
        return new ZAppointment(mApptEl);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new flags or default value if flags didn't change
     */
    public String getFlags(String defaultValue) {
        return mApptEl.getAttribute(MailConstants.A_FLAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new tags or default value if tags didn't change
     */
    public String getTagIds(String defaultValue) {
        return mApptEl.getAttribute(MailConstants.A_TAGS, defaultValue);
    }

    public String getFolderId(String defaultValue) {
        return mApptEl.getAttribute(MailConstants.A_FOLDER, defaultValue);
    }

}
