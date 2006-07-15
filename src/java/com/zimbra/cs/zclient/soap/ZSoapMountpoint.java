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
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMountpoint;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.Element;

class ZSoapMountpoint implements ZMountpoint, ZSoapItem {

    private int mColor;
    private String mId;
    private String mName;
    private String mDefaultView;
    private String mFlags;
    private String mParentId;
    private String mOwnerId;
    private String mOwnerDisplayName;
    private String mRemoteId;
    private ZFolder mParent;
    
    ZSoapMountpoint(Element e, ZSoapFolder parent, ZSoapMailbox mailbox) throws ServiceException {
        mParent = parent;
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mParentId = e.getAttribute(MailService.A_FOLDER);
        mFlags = e.getAttribute(MailService.A_FLAGS, null);
        mColor = (int) e.getAttributeLong(MailService.A_COLOR, 0);
        mDefaultView = e.getAttribute(MailService.A_DEFAULT_VIEW, null);
        mOwnerDisplayName = e.getAttribute(MailService.A_DISPLAY);
        mRemoteId = e.getAttribute(MailService.A_REMOTE_ID);
        mOwnerId = e.getAttribute(MailService.A_ZIMBRA_ID);
        mailbox.addItemIdMapping(this);
        if (parent != null) parent.addChild(this);
    }

    public ZFolder getParent() {
        return mParent;
    }

    public int getColor() {
        return mColor;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("name", mName);
        sb.add("path", getPath());
        sb.add("view", mDefaultView);
        sb.add("flags", mFlags);
        sb.add("parent", mParentId);
        sb.add("color", mColor);
        sb.add("ownerId", mOwnerId);
        sb.add("ownerDisplayName", mOwnerDisplayName);
        sb.add("remoteId", mRemoteId);
        sb.endStruct();
        return sb.toString();
    }

    public String getDefaultView() {
        return mDefaultView;
    }

    public String getFlags() {
        return mFlags;
    }

    public String getParentId() {
        return mParentId;
    }

    public String getPath() {
        if (mParent == null)
            return ZMailbox.PATH_SEPARATOR;
        else {
            String pp = mParent.getPath();
            return pp.length() == 1 ? (pp + mName) : (pp + ZMailbox.PATH_SEPARATOR + mName);
        }
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
