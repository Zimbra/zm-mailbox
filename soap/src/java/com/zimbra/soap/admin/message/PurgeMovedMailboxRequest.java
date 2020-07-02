/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.Name;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Purge moved mailbox.  Following a successful mailbox move to a new server, the
 * mailbox on the old server remains.  This allows manually checking the new mailbox to confirm the move worked.
 * Afterwards, <b>PurgeMovedMailboxRequest</b> should be used to remove the old mailbox and reclaim the space.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_PURGE_MOVED_MAILBOX_REQUEST)
public class PurgeMovedMailboxRequest {

    /**
     * @zm-api-field-description Mailbox specification
     */
    @ZimbraUniqueElement
    @XmlElement(name=BackupConstants.E_MAILBOX /* mbox */, required=true)
    private final Name mailbox;

    /**
     * @zm-api-field-tag force delete blobs from store.
     * @zm-api-field-description force delete blobs from store.
     */
    @XmlAttribute(name=BackupConstants.A_FORCE_DELETE_BLOBS /* forceDeleteBlobs */, required=false)
    private ZmBoolean forceDeleteBlobs;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PurgeMovedMailboxRequest() {
        this((Name) null);
    }

    public PurgeMovedMailboxRequest(Name mailbox) {
        this.mailbox = mailbox;
    }

    public void setForceDeleteBlobs(Boolean forceDeleteBlobs) {
        this.forceDeleteBlobs = ZmBoolean.fromBool(forceDeleteBlobs);
    }

    public Name getMailbox() { return mailbox; }
    public Boolean getForceDeleteBlobs() {
        Boolean forceDelete = ZmBoolean.toBool(forceDeleteBlobs);
        return (forceDelete == null) ? false : forceDelete;
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("mailbox", mailbox)
            .add("forceDeleteBlobs", forceDeleteBlobs);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
