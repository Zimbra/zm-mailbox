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

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.zclient.ZSoapSB;
import com.zimbra.cs.zclient.ZItem;

public class ZModifyAppointmentEvent implements ZModifyItemEvent, ZModifyItemFolderEvent {

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


    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            sb.add("id", getId());
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }

    public ZItem getItem() throws ServiceException {
        return null;
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

    public String getPriority(String defaultValue) {
        return mApptEl.getAttribute(MailConstants.A_CAL_PRIORITY, defaultValue);
    }

    public String getPercentComplete(String defaultValue) {
        return mApptEl.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, defaultValue);
    }

    public String getStatus(String defaultValue) {
        return mApptEl.getAttribute(MailConstants.A_CAL_STATUS, defaultValue);
    }


}
