/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.client;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZModifyFolderEvent;
import com.zimbra.client.event.ZModifyMountpointEvent;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MountpointStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.Mountpoint;

public class ZMountpoint extends ZFolder implements MountpointStore {

    private String mOwnerId;
    private String mOwnerDisplayName;
    private String mRemoteId;

    public ZMountpoint(Element e, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        super(e, parent, mailbox);
        mOwnerDisplayName = e.getAttribute(MailConstants.A_OWNER_NAME, null);
        mRemoteId = e.getAttribute(MailConstants.A_REMOTE_ID);
        mOwnerId = e.getAttribute(MailConstants.A_ZIMBRA_ID);
    }

    public ZMountpoint(Mountpoint m, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        super(m, parent, mailbox);
        mOwnerDisplayName = m.getOwnerEmail();
        mRemoteId = Integer.toString(m.getRemoteFolderId());
        mOwnerId = m.getOwnerAccountId();
    }

    @Override
    public void modifyNotification(ZModifyEvent e) throws ServiceException {
        if (e instanceof ZModifyMountpointEvent) {
            ZModifyMountpointEvent mpe = (ZModifyMountpointEvent) e;
            if (mpe.getId().equals(getId())) {
                mOwnerDisplayName = mpe.getOwnerDisplayName(mOwnerDisplayName);
                mRemoteId = mpe.getRemoteId(mRemoteId);
                mOwnerId = mpe.getOwnerId(mOwnerId);
                super.modifyNotification(e);
            }
        } else if (e instanceof ZModifyFolderEvent) {
            super.modifyNotification(e);
        }
    }

    @Override
    public String toString() {
        return String.format("[ZMountpoint %s]", getPath());
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = super.toZJSONObject();
        jo.put("ownerId", mOwnerId);
        jo.put("ownerDisplayName", mOwnerDisplayName);
        jo.put("remoteId", mRemoteId);
        return jo;
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

    @Override
    public ItemIdentifier getTargetItemIdentifier() {
        return ItemIdentifier.fromOwnerAndRemoteId(mOwnerId, mRemoteId);
    }
}
