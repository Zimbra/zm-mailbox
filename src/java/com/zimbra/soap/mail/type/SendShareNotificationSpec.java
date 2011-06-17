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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class SendShareNotificationSpec {

    // Case insensitive type from com.zimbra.cs.mailbox.ACL
    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=false)
    private String grantType;

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_PATH /* path */, required=false)
    private String folderPath;

    public SendShareNotificationSpec() {
    }

    public void setGrantType(String grantType) { this.grantType = grantType; }
    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }
    public String getGrantType() { return grantType; }
    public String getZimbraId() { return zimbraId; }
    public String getDisplayName() { return displayName; }
    public String getFolderId() { return folderId; }
    public String getFolderPath() { return folderPath; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("grantType", grantType)
            .add("zimbraId", zimbraId)
            .add("displayName", displayName)
            .add("folderId", folderId)
            .add("folderPath", folderPath);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
