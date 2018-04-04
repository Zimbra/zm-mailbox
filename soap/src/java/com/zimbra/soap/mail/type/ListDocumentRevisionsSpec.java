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
public class ListDocumentRevisionsSpec {

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag version
     * @zm-api-field-description Version
     */
    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=false)
    private Integer version;

    /**
     * @zm-api-field-tag num-revisions
     * @zm-api-field-description Maximum number of revisions to return starting from <b>{version}</b>
     */
    @XmlAttribute(name=MailConstants.A_COUNT /* count */, required=false)
    private Integer count;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ListDocumentRevisionsSpec() {
        this((String) null);
    }

    public ListDocumentRevisionsSpec(String id) {
        this.id = id;
    }

    public void setVersion(Integer version) { this.version = version; }
    public void setCount(Integer count) { this.count = count; }
    public String getId() { return id; }
    public Integer getVersion() { return version; }
    public Integer getCount() { return count; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("version", version)
            .add("count", count);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
