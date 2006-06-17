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
import com.zimbra.cs.zclient.ZLink;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.soap.Element;

class ZSoapLink implements ZLink, ZSoapItem {

    private byte mColor;
    private String mId;
    private String mName;
    private String mDefaultView;
    private String mFlags;
    private String mParentId;
    private String mOwnerId;
    private String mOwnerDisplayName;
    private String mRemoteId;
    private ZFolder mParent;
    
    ZSoapLink(Element e, ZFolder parent, ZSoapMailbox mailbox) throws ServiceException {
        mParent = parent;
        mId = e.getAttribute(MailService.A_ID);
        mName = e.getAttribute(MailService.A_NAME);
        mParentId = e.getAttribute(MailService.A_FOLDER);
        mFlags = e.getAttribute(MailService.A_FLAGS, "");
        mColor = (byte) e.getAttributeLong(MailService.A_COLOR, 0);
        mDefaultView = e.getAttribute(MailService.A_DEFAULT_VIEW, "");
        mOwnerDisplayName = e.getAttribute(MailService.A_DISPLAY);
        mRemoteId = e.getAttribute(MailService.A_REMOTE_ID);
        mOwnerId = e.getAttribute(MailService.A_ZIMBRA_ID);
        mailbox.addItemIdMapping(this);
    }

    public ZFolder getParent() {
        return mParent;
    }

    public byte getColor() {
        return mColor;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String toString() {
        return String.format("link: { id: %s, name: %s, parentId: %s, flags: %s, color: %d, view: %s, owner: %s, ownerId: %s, remoteId: %s, path: %s }",
                mId, mName, mParentId, mFlags, mColor, mDefaultView, mOwnerDisplayName, mOwnerId, mRemoteId, getPath());
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
        // TODO: CACHE? compute upfront?
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
