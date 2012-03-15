/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public final class NewContactAttr {

    /**
     * @zm-api-field-tag attr-name
     * @zm-api-field-description Attribute name
     */
    @XmlAttribute(name=MailConstants.A_ATTRIBUTE_NAME /* n */, required=true)
    private String name;

    /**
     * @zm-api-field-tag upload-id
     * @zm-api-field-description Upload ID
     */
    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachId;

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID.  Used in combination with <b>subpart-name}</b>
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private Integer id;

    /**
     * @zm-api-field-tag subpart-name
     * @zm-api-field-description Subpart Name
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    // See ParsedContact.FieldDelta.Op - values "+" or "-"
    /**
     * @zm-api-field-tag operation-+|-
     * @zm-api-field-description Operation - <b>+|-</b>
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=false)
    private String operation;

    /**
     * @zm-api-field-tag attr-data
     * @zm-api-field-description Attribute data
     */
    @XmlValue
    private String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NewContactAttr() {
         this((String) null);
    }

    public NewContactAttr(String name) {
         this.name = name;
    }

    public void setName(String name) { this.name = name; }
    public void setAttachId(String attachId) { this.attachId = attachId; }
    public void setId(Integer id) { this.id = id; }
    public void setPart(String part) { this.part = part; }
    public void setOperation(String operation) { this.operation = operation; }
    public void setValue(String value) { this.value = value; }
    public String getName() { return name; }
    public String getAttachId() { return attachId; }
    public Integer getId() { return id; }
    public String getPart() { return part; }
    public String getOperation() { return operation; }
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("attachId", attachId)
            .add("id", id)
            .add("part", part)
            .add("operation", operation)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
