/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.mail.type.Folder;

public class ZSharedFolder extends ZFolder {

    private final String targetId;
    /**
     * This represents a folder that has been shared with the owning mailbox.
     * It differs from a mountpoint in that the share has not necessarily been accepted,
     * so this folder is typically only visible via a mechanism similar to IMAP's
     * "/home/<username>/..." namespace mechanism.
     */
    public ZSharedFolder(Folder f, ZFolder parent, String targetId, ZMailbox mailbox) throws ServiceException {
        super(f, parent, mailbox);
        this.targetId = targetId;
    }

    public String getTargetId() {
        return targetId;
    }

    public ItemIdentifier getTargetItemIdentifier() throws ServiceException {
        return new ItemIdentifier(targetId, null);
    }

    @Override
    public boolean isHidden() {
        if (getParent() == null) {
            return false;
        }
        return super.isHidden();
    }

    @Override public boolean inTrash() {
        return false;
    }

    @Override public String toString() {
        return String.format("[ZSharedFolder %s targeId=%s]", getPath(), targetId);
    }

}
