/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.zclient.ToZJSONObject;
import com.zimbra.cs.zclient.ZJSONObject;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

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
        return mContactEl.getAttributeLong(MailConstants.A_MODIFIED_DATE, defaultValue);
    }

    public String getFileAsStr(String defaultValue) {
        return mContactEl.getAttribute(MailConstants.A_FILE_AS_STR, defaultValue);
    }

    public String getRevision(String defaultValue) {
        return mContactEl.getAttribute(MailConstants.A_REVISION, defaultValue);
    }

    public String getEmail(String defaultValue) {
        return mContactEl.getAttribute(Contact.A_email, defaultValue);
    }

    public String getEmail2(String defaultValue) {
        return mContactEl.getAttribute(Contact.A_email2, defaultValue);
    }

    public String getEmail3(String defaultValue) {
        return mContactEl.getAttribute(Contact.A_email3, defaultValue);
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
        return ZJSONObject.toString(this);
    }
}
