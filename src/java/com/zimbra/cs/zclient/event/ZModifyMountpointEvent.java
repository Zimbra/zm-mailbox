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

public class ZModifyMountpointEvent extends ZModifyFolderEvent {


    public ZModifyMountpointEvent(Element e) throws ServiceException {
        super(e);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getOwnerDisplayName(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_OWNER_NAME, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getRemoteId(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_REMOTE_ID, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getOwnerId(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_ZIMBRA_ID, defaultValue);
    }
    
    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            toStringCommon(sb);
            if (getOwnerId(null) != null) sb.add("ownerId", getOwnerId(null));
            if (getOwnerDisplayName(null) != null) sb.add("ownerDisplayName", getOwnerDisplayName(null));
            if (getRemoteId(null) != null) sb.add("remoteId", getRemoteId(null));
            sb.endStruct();
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }
}
