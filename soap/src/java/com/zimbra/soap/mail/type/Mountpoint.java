/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

/*
            <link id="1" name="new-mount-point" l="1" n="6" u="1" f="u" owner="user1@example.com" zid="151bd192-e19a-40be-b8c9-259b21ffac48" rid="2" oname="user1folder">

 */
// Root element name needed to differentiate between types of folder
@XmlRootElement(name=MailConstants.E_MOUNT /* link */)
@GraphQLType(name=GqlConstants.CLASS_MOUNTPOINT, description="mountpoint details")
public class Mountpoint
extends Folder {

    /**
     * @zm-api-field-tag owner-email-addr
     * @zm-api-field-description Primary email address of the owner of the linked-to resource
     */
    @XmlAttribute(name=MailConstants.A_OWNER_NAME /* owner */, required=false)
    private String ownerEmail;

    /**
     * @zm-api-field-tag owner-zimbra-id
     * @zm-api-field-description Zimbra ID (guid) of the owner of the linked-to resource
     */
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String ownerAccountId;

    /**
     * @zm-api-field-tag id-of-shared-item
     * @zm-api-field-description Item ID of the linked-to resource in the remote mailbox
     */
    @XmlAttribute(name=MailConstants.A_REMOTE_ID /* rid */, required=false)
    private int remoteFolderId;

    /**
     * @zm-api-field-tag uuid-of-shared-item
     * @zm-api-field-description UUID of the linked-to resource in the remote mailbox
     */
    @XmlAttribute(name=MailConstants.A_REMOTE_UUID /* ruuid */, required=false)
    private String remoteUuid;

    /**
     * @zm-api-field-tag owner-name-for-item
     * @zm-api-field-description The name presently used for the item by the owner
     */
    @XmlAttribute(name=MailConstants.A_OWNER_FOLDER_NAME /* oname */, required=false)
    private String remoteFolderName;

    /**
     * @zm-api-field-tag display-reminders
     * @zm-api-field-description If set, client should display reminders for shared appointments/tasks
     */
    @XmlAttribute(name=MailConstants.A_REMINDER /* reminder */, required=false)
    private ZmBoolean reminderEnabled;

    /**
     * @zm-api-field-tag broken-link
     * @zm-api-field-description If "tr" is true in the request, <b>broken</b> is set if this is a broken link
     */
    @XmlAttribute(name=MailConstants.A_BROKEN /* broken */, required=false)
    private ZmBoolean broken;

    public Mountpoint() {
    }

    @GraphQLQuery(name=GqlConstants.OWNER_EMAIL, description="Primary email address of the owner of the linked-to resource")
    public String getOwnerEmail() {
        return ownerEmail;
    }

    @GraphQLQuery(name=GqlConstants.OWNER_ACCOUNT_ID, description="Zimbra ID (guid) of the owner of the linked-to resource")
    public String getOwnerAccountId() {
        return ownerAccountId;
    }

    @GraphQLQuery(name=GqlConstants.REMOTE_FOLDER_ID, description="Item ID of the linked-to resource in the remote mailbox")
    public int getRemoteFolderId() {
        return remoteFolderId;
    }

    @GraphQLQuery(name=GqlConstants.OWNER_FOLDER_NAME, description="The name presently used for the item by the owner")
    public String getRemoteFolderName() {
        return remoteFolderName;
    }

    @GraphQLQuery(name=GqlConstants.REMINDER_ENABLED, description="If set, client should display reminders for shared appointments/tasks")
    public Boolean getReminderEnabled() {
        return ZmBoolean.toBool(reminderEnabled);
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public void setOwnerAccountId(String accountId) {
        this.ownerAccountId = accountId;
    }

    public void setRemoteFolderId(int remoteFolderId) {
        this.remoteFolderId = remoteFolderId;
    }

    public void setRemoteFolderName(String remoteFolderName) {
        this.remoteFolderName = remoteFolderName;
    }

    public void setReminderEnabled(Boolean reminderEnabled) {
        this.reminderEnabled = ZmBoolean.fromBool(reminderEnabled);
    }

    @GraphQLQuery(name=GqlConstants.IS_BROKEN, description="If \"tr\" is true in the request, broken is set if this is a broken link")
    public Boolean getBroken() {
        return ZmBoolean.toBool(broken);
    }

    public void setBroken(Boolean broken) {
        this.broken = ZmBoolean.fromBool(broken);
    }

    @GraphQLQuery(name=GqlConstants.REMOTE_UUID, description="UUID of the linked-to resource in the remote mailbox")
    public String getRemoteUuid() {
        return remoteUuid;
    }

    public void setRemoteUuid(String remoteUuid) {
        this.remoteUuid = remoteUuid;
    }
}
