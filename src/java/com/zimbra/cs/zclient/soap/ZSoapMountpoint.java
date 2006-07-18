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

package com.zimbra.cs.zclient.soap;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.zclient.ZMountpoint;
import com.zimbra.soap.Element;

class ZSoapMountpoint extends ZSoapFolder implements ZMountpoint, ZSoapItem {

    private String mOwnerId;
    private String mOwnerDisplayName;
    private String mRemoteId;
    
    ZSoapMountpoint(Element e, ZSoapFolder parent, ZSoapMailbox mailbox) throws ServiceException {
        super(e, parent, mailbox);
        mOwnerDisplayName = e.getAttribute(MailService.A_DISPLAY);
        mRemoteId = e.getAttribute(MailService.A_REMOTE_ID);
        mOwnerId = e.getAttribute(MailService.A_ZIMBRA_ID);
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("ownerId", mOwnerId);
        sb.add("ownerDisplayName", mOwnerDisplayName);
        sb.add("remoteId", mRemoteId);
        toStringCommon(sb);
        sb.endStruct();
        return sb.toString();
    }

    public String getOwnerDisplayName() {
        return mOwnerDisplayName;
    }

    public String getRemoteId() {
        return mRemoteId;
    }

    public String getOwnerId() {
        return mOwnerId;
    }
}
