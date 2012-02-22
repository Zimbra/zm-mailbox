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
public class DiffDocumentVersionSpec {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag revision-1
     * @zm-api-field-description Revision 1
     */
    @XmlAttribute(name=MailConstants.A_V1 /* v1 */, required=false)
    private Integer version1;

    /**
     * @zm-api-field-tag revision-2
     * @zm-api-field-description Revision 2
     */
    @XmlAttribute(name=MailConstants.A_V2 /* v2 */, required=false)
    private Integer version2;

    public DiffDocumentVersionSpec() {
    }

    public void setId(String id) { this.id = id; }
    public void setVersion1(Integer version1) { this.version1 = version1; }
    public void setVersion2(Integer version2) { this.version2 = version2; }
    public String getId() { return id; }
    public Integer getVersion1() { return version1; }
    public Integer getVersion2() { return version2; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("version1", version1)
            .add("version2", version2);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
