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

import com.zimbra.common.soap.Element;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.zclient.ZSoapSB;

import java.util.Map;
import java.util.HashMap;

public class ZContactEvent {

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

    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            sb.add("id", getId());
            if (getFlags(null) != null) sb.add("flags", getFlags(null));
            if (getTagIds(null) != null) sb.add("tags", getTagIds(null));
            if (getFolderId(null) != null) sb.add("folderId", getFolderId(null));
            if (getRevision(null) != null) sb.add("revision", getRevision(null));
            if (getFileAsStr(null) != null) sb.add("fileAsStr", getFileAsStr(null));
            if (getEmail(null) != null) sb.add("email", getEmail(null));
            if (getEmail2(null) != null) sb.add("email2", getEmail2(null));
            if (getEmail3(null) != null) sb.add("email3", getEmail3(null));
            Map<String, String> attrs = getAttrs(null);
            if (attrs != null) {
                sb.beginStruct("attrs");
                for (Map.Entry<String, String> entry : attrs.entrySet()) {
                    sb.add(entry.getKey(), entry.getValue());
                }
                sb.endStruct();
            }
            sb.endStruct();
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }
}
