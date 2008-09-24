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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.ToZJSONObject;
import com.zimbra.cs.zclient.ZJSONObject;
import org.json.JSONException;

public class ZModifyMessageEvent implements ZModifyItemEvent, ZModifyItemFolderEvent, ToZJSONObject {

    protected Element mMessageEl;

    public ZModifyMessageEvent(Element e) throws ServiceException {
        mMessageEl = e;
    }

    /**
     * @return id
     * @throws com.zimbra.common.service.ServiceException on error
     */
    public String getId() throws ServiceException {
        return mMessageEl.getAttribute(MailConstants.A_ID);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new flags or default value if flags didn't change
     */
    public String getFlags(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_FLAGS, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new tags or default value if tags didn't change
     */
    public String getTagIds(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_TAGS, defaultValue);
    }

    public String getFolderId(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_FOLDER, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new conv id or defaultValue if unchanged
     */
    public String getConversationId(String defaultValue) {
        return mMessageEl.getAttribute(MailConstants.A_CONV_ID, defaultValue);
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        try {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", getId());
            if (getConversationId(null) != null) zjo.put("conversationId", getConversationId(null));
            if (getFlags(null) != null) zjo.put("flags", getFlags(null));
            if (getTagIds(null) != null) zjo.put("tags", getTagIds(null));
            if (getFolderId(null) != null) zjo.put("folderId", getFolderId(null));            
            return zjo;
        } catch (ServiceException se) {
            throw new JSONException(se);
        }
    }

    public String toString() {
        return ZJSONObject.toString(this);
    }
}
