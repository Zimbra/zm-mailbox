/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ShareInfo {

    @XmlAttribute(name=AccountConstants.A_OWNER_ID, required=true)
    private String ownerId;
    @XmlAttribute(name=AccountConstants.A_OWNER_EMAIL, required=true)
    private String ownerEmail;
    @XmlAttribute(name=AccountConstants.A_OWNER_DISPLAY_NAME, required=true)
    private String ownerDisplayName;
    @XmlAttribute(name=AccountConstants.A_FOLDER_ID, required=true)
    private int folderId;
    @XmlAttribute(name=AccountConstants.A_FOLDER_PATH, required=true)
    private String folderPath;
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW, required=true)
    private String defaultView;
    @XmlAttribute(name=AccountConstants.A_RIGHTS, required=true)
    private String rights;
    @XmlAttribute(name=AccountConstants.A_GRANTEE_TYPE, required=true)
    private String granteeType;
    @XmlAttribute(name=AccountConstants.A_GRANTEE_ID, required=true)
    private String granteeId;
    @XmlAttribute(name=AccountConstants.A_GRANTEE_NAME, required=true)
    private String granteeName;
    @XmlAttribute(name=AccountConstants.A_GRANTEE_DISPLAY_NAME, required=true)
    private String granteeDisplayName;
    @XmlAttribute(name=AccountConstants.A_MOUNTPOINT_ID, required=false)
    private String mountpointId;

    public ShareInfo() {
    }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerId() { return ownerId; }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerEmail() { return ownerEmail; }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getOwnerDisplayName() { return ownerDisplayName; }

    public void setFolderId(int folderId) { this.folderId = folderId; }

    public int getFolderId() { return folderId; }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getFolderPath() { return folderPath; }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public String getDefaultView() { return defaultView; }

    public void setRights(String rights) { this.rights = rights; }

    public String getRights() { return rights; }

    public void setGranteeType(String granteeType) {
        this.granteeType = granteeType;
    }

    public String getGranteeType() { return granteeType; }

    public void setGranteeId(String granteeId) {
        this.granteeId = granteeId;
    }

    public String getGranteeId() { return granteeId; }

    public void setGranteeName(String granteeName) {
        this.granteeName = granteeName;
    }

    public String getGranteeName() { return granteeName; }

    public void setGranteeDisplayName(String granteeDisplayName) {
        this.granteeDisplayName = granteeDisplayName;
    }

    public String getGranteeDisplayName() { return granteeDisplayName; }

    public void setMountpointId(String mountpointId) {
        this.mountpointId = mountpointId;
    }

    public String getMountpointId() { return mountpointId; }
}
