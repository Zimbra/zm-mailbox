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

@XmlAccessorType(XmlAccessType.NONE)
public class FreeBusyUserSpec {

    /**
     * @zm-api-field-tag calendar-folder-id
     * @zm-api-field-description Calendar folder ID; if omitted, get f/b on all calendar folders
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private Integer folderId;

    /**
     * @zm-api-field-tag zimbra-id
     * @zm-api-field-description Zimbra ID Either "name" or "id" must be specified
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag email
     * @zm-api-field-description Email address.  Either "name" or "id" must be specified
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    public FreeBusyUserSpec() {
    }

    public void setFolderId(Integer folderId) { this.folderId = folderId; }
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public Integer getFolderId() { return folderId; }
    public String getId() { return id; }
    public String getName() { return name; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("folderId", folderId)
            .add("id", id)
            .add("name", name);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
