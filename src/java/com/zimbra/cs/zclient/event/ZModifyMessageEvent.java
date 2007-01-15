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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zclient.ZSoapSB;

public class ZModifyMessageEvent implements ZModifyItemEvent, ZModifyItemFolderEvent {

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

    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            sb.add("id", getId());
            if (getConversationId(null) != null) sb.add("conversationId", getConversationId(null));
            if (getFlags(null) != null) sb.add("flags", getFlags(null));
            if (getTagIds(null) != null) sb.add("tags", getTagIds(null));
            if (getFolderId(null) != null) sb.add("folderId", getFolderId(null));            
            sb.endStruct();
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }
}
