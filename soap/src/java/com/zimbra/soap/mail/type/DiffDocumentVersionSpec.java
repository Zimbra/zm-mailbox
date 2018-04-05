/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("version1", version1)
            .add("version2", version2);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
