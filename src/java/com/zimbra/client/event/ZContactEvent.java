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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZJSONObject;

public class ZContactEvent implements ToZJSONObject {

    protected Element mContactEl;

    public ZContactEvent(Element e) throws ServiceException {
        mContactEl = e;
    }

    /**
     * @return id
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String getId() throws ServiceException {
        return mContactEl.getAttribute(MailConstants.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new flags or default value if flags didn't change
     */
    public String getFlags(String defaultValue) {
        return mContactEl.getAttribute(MailConstants.A_FLAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new tags or default value if tags didn't change
     */
    public String getTagIds(String defaultValue) {
        return mContactEl.getAttribute(MailConstants.A_TAGS, defaultValue);
    }

    public String getFolderId(String defaultValue) {
        return mContactEl.getAttribute(MailConstants.A_FOLDER, defaultValue);
    }

    public long getMetaDataChangedDate(long defaultValue) throws ServiceException {
        return mContactEl.getAttributeLong(MailConstants.A_CHANGE_DATE, defaultValue);
    }

    public long getDate(long defaultValue) throws ServiceException {
        return mContactEl.getAttributeLong(MailConstants.A_DATE, defaultValue);
    }
    
    public String getFileAsStr(String defaultValue) {
        return mContactEl.getAttribute(MailConstants.A_FILE_AS_STR, defaultValue);
    }

    public String getRevision(String defaultValue) {
        return mContactEl.getAttribute(MailConstants.A_REVISION, defaultValue);
    }

    public String getEmail(String defaultValue) {
        return mContactEl.getAttribute(ContactConstants.A_email, defaultValue);
    }

    public String getEmail2(String defaultValue) {
        return mContactEl.getAttribute(ContactConstants.A_email2, defaultValue);
    }

    public String getEmail3(String defaultValue) {
        return mContactEl.getAttribute(ContactConstants.A_email3, defaultValue);
    }

    public Map<String, String> getAttrs(Map<String, String> defaultValue) throws ServiceException {
    	Map<String, String> attrs = null;
        for (KeyValuePair pair : mContactEl.listKeyValuePairs(MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME)) {
            if (attrs == null) attrs = new HashMap<String, String>();
            attrs.put(pair.getKey(), pair.getValue());

        }
        return attrs != null ? attrs : defaultValue;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        try {
            zjo.put("id", getId());
            if (getFlags(null) != null) zjo.put("flags", getFlags(null));
            if (getTagIds(null) != null) zjo.put("tags", getTagIds(null));
            if (getFolderId(null) != null) zjo.put("folderId", getFolderId(null));
            if (getRevision(null) != null) zjo.put("revision", getRevision(null));
            if (getFileAsStr(null) != null) zjo.put("fileAsStr", getFileAsStr(null));
            if (getEmail(null) != null) zjo.put("email", getEmail(null));
            if (getEmail2(null) != null) zjo.put("email2", getEmail2(null));
            if (getEmail3(null) != null) zjo.put("email3", getEmail3(null));
            Map<String, String> attrs = getAttrs(null);
            if (attrs != null)
                zjo.putMap("attrs", attrs);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
        return zjo;
    }

    public String toString() {
        try {
            return String.format("[ZContactEvent %s]", getId());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
