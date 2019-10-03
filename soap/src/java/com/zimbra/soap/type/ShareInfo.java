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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@JsonPropertyOrder({ "ownerId", "ownerEmail", "ownerName", "folderId", "folderUuid", "folderPath", "view", "rights",
    "granteeType", "granteeId", "granteeName", "granteeDisplayName", "mid" })
@GraphQLType(name=GqlConstants.CLASS_SHARE_INFO, description="share info")
public class ShareInfo {

    /**
     * @zm-api-field-tag share-owner-id
     * @zm-api-field-description Owner ID
     */
    @XmlAttribute(name=AccountConstants.A_OWNER_ID /* ownerId */, required=true)
    private String ownerId;

    /**
     * @zm-api-field-tag share-owner-email
     * @zm-api-field-description Owner email
     */
    @XmlAttribute(name=AccountConstants.A_OWNER_EMAIL /* ownerEmail */, required=true)
    private String ownerEmail;

    /**
     * @zm-api-field-tag share-owner-display-name
     * @zm-api-field-description Owner display name
     */
    @XmlAttribute(name=AccountConstants.A_OWNER_DISPLAY_NAME /* ownerName */, required=true)
    private String ownerDisplayName;

    /**
     * @zm-api-field-tag share-folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=AccountConstants.A_FOLDER_ID /* folderId */, required=true)
    private int folderId;

    /**
     * @zm-api-field-tag share-folder-uuid
     * @zm-api-field-description Folder UUID
     */
    @XmlAttribute(name=AccountConstants.A_FOLDER_UUID /* folderUuid */, required=true)
    private String folderUuid;

    /**
     * @zm-api-field-tag share-fully-qualified-path
     * @zm-api-field-description Fully qualified path
     */
    @XmlAttribute(name=AccountConstants.A_FOLDER_PATH /* folderPath */, required=true)
    private String folderPath;

    /**
     * @zm-api-field-tag share-default-view
     * @zm-api-field-description Default type
     */
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW /* view */, required=true)
    private String defaultView;

    /**
     * @zm-api-field-tag share-rights
     * @zm-api-field-description Rights
     */
    @XmlAttribute(name=AccountConstants.A_RIGHTS /* rights */, required=true)
    private String rights;

    /**
     * @zm-api-field-tag grantee-type
     * @zm-api-field-description Grantee type
     */
    @XmlAttribute(name=AccountConstants.A_GRANTEE_TYPE /* granteeType */, required=true)
    private String granteeType;

    /**
     * @zm-api-field-tag grantee-id
     * @zm-api-field-description Grantee ID
     */
    @XmlAttribute(name=AccountConstants.A_GRANTEE_ID /* granteeId */, required=true)
    private String granteeId;

    /**
     * @zm-api-field-tag grantee-name
     * @zm-api-field-description Grantee name
     */
    @XmlAttribute(name=AccountConstants.A_GRANTEE_NAME /* granteeName */, required=true)
    private String granteeName;

    /**
     * @zm-api-field-tag grantee-display-name
     * @zm-api-field-description Grantee display name
     */
    @XmlAttribute(name=AccountConstants.A_GRANTEE_DISPLAY_NAME /* granteeDisplayName */, required=true)
    private String granteeDisplayName;

    /**
     * @zm-api-field-tag mountpoint-id
     * @zm-api-field-description Returned if the share is already mounted.  Contains the folder id of the mountpoint
     * in the local mailbox.
     */
    @XmlAttribute(name=AccountConstants.A_MOUNTPOINT_ID /* mid */, required=false)
    private String mountpointId;

    public ShareInfo() {
    }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.OWNER_ID, description="owner id")
    public String getOwnerId() { return ownerId; }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.OWNER_EMAIL, description="owner email")
    public String getOwnerEmail() { return ownerEmail; }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    @GraphQLQuery(name=GqlConstants.OWNER_NAME, description="owner display name")
    public String getOwnerDisplayName() { return ownerDisplayName; }

    public void setFolderId(int folderId) { this.folderId = folderId; }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.FOLDER_ID, description="shared folder id")
    public int getFolderId() { return folderId; }

    public void setFolderUuid(String folderUuid) { this.folderUuid = folderUuid; }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.FOLDER_UUID, description="shared folder UUID")
    public String getFolderUuid() { return folderUuid; }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.FOLDER_PATH, description="shared folder path")
    public String getFolderPath() { return folderPath; }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.VIEW, description="shared folder view")
    public String getDefaultView() { return defaultView; }

    public void setRights(String rights) { this.rights = rights; }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.RIGHTS, description="rights")
    public String getRights() { return rights; }

    public void setGranteeType(String granteeType) {
        this.granteeType = granteeType;
    }

    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.GRANTEE_TYPE, description="grantee type")
    public String getGranteeType() { return granteeType; }

    public void setGranteeId(String granteeId) {
        this.granteeId = granteeId;
    }

    @GraphQLQuery(name=GqlConstants.RIGHTS, description="rights")
    public String getGranteeId() { return granteeId; }

    public void setGranteeName(String granteeName) {
        this.granteeName = granteeName;
    }

    @GraphQLQuery(name=GqlConstants.GRNATEE_NAME, description="grantee name")
    public String getGranteeName() { return granteeName; }

    public void setGranteeDisplayName(String granteeDisplayName) {
        this.granteeDisplayName = granteeDisplayName;
    }

    @GraphQLQuery(name=GqlConstants.GRANTEE_DISPLAY_NAME, description="grantee display name")
    public String getGranteeDisplayName() { return granteeDisplayName; }

    public void setMountpointId(String mountpointId) {
        this.mountpointId = mountpointId;
    }

    @GraphQLQuery(name=GqlConstants.MOUNTPOINT_ID, description="mountpoint id")
    public String getMountpointId() { return mountpointId; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("ownerId", ownerId)
            .add("ownerEmail", ownerEmail)
            .add("ownerDisplayName", ownerDisplayName)
            .add("folderId", folderId)
            .add("folderUuid", folderUuid)
            .add("folderPath", folderPath)
            .add("defaultView", defaultView)
            .add("rights", rights)
            .add("granteeType", granteeType)
            .add("granteeId", granteeId)
            .add("granteeName", granteeName)
            .add("granteeDisplayName", granteeDisplayName)
            .add("mountpointId", mountpointId)
            .toString();
    }
}
