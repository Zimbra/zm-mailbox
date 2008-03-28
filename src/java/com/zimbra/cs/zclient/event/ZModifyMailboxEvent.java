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
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.ZSoapSB;
import com.zimbra.common.soap.Element;

public class ZModifyMailboxEvent implements ZModifyEvent {

    protected Element mMailboxEl;

    public ZModifyMailboxEvent(Element e) {
        mMailboxEl = e;
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new size, or defaultValue if unchanged
     * @throws ServiceException on error
     */
    public long getSize(long defaultValue) throws ServiceException {
        return mMailboxEl.getAttributeLong(MailConstants.A_SIZE, defaultValue);
    }

    public String getOwner(String defaultId) {
        return mMailboxEl.getAttribute(HeaderConstants.A_ACCOUNT_ID, defaultId);
    }

    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            if (getSize(-1) != -1) sb.add("size", getSize(-1));
            if (getOwner(null) != null) sb.add("owner", getOwner(null));
            sb.endStruct();
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }
}
