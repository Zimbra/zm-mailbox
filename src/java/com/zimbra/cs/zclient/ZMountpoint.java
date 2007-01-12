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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyMountpointEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.soap.Element;

public class ZMountpoint extends ZFolder {

    private String mOwnerId;
    private String mOwnerDisplayName;
    private String mRemoteId;
    
    public ZMountpoint(Element e, ZFolder parent) throws ServiceException {
        super(e, parent);
        mOwnerDisplayName = e.getAttribute(MailConstants.A_OWNER_NAME, null); // TODO: change back to required when DF is on main
        mRemoteId = e.getAttribute(MailConstants.A_REMOTE_ID);
        mOwnerId = e.getAttribute(MailConstants.A_ZIMBRA_ID);
    }

    public void modifyNotification(ZModifyEvent e) throws ServiceException {
        if (e instanceof ZModifyMountpointEvent) {
            ZModifyMountpointEvent mpe = (ZModifyMountpointEvent) e;
            if (mpe.getId().equals(getId())) {
                mOwnerDisplayName = mpe.getOwnerDisplayName(mOwnerDisplayName);
                mRemoteId = mpe.getRemoteId(mRemoteId);
                mOwnerId = mpe.getOwnerId(mOwnerId);
                super.modifyNotification(e);
            }
        }
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


    /**
     * @return primary email address of the owner of the mounted resource
     */
    public String getOwnerDisplayName() {
        return mOwnerDisplayName;
    }

    /**
     * @return zimbra id of the owner of the mounted resource
     */
    public String getOwnerId() {
        return mOwnerId;
    }

    /**
     * @return remote folder id of the mounted folder
     */
    public String getRemoteId() {
        return mRemoteId;
    }

    /**
     *
     * @return the canonical remote id: {owner-id}:{remote-id}
     */
    public String getCanonicalRemoteId() {
        return mOwnerId+":"+mRemoteId;       
    }

}
