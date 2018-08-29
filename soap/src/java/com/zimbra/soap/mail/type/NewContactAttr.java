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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.MoreObjects;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_NEW_CONTACT_ATTRIBUTE, description="Input for creating a new contact attribute")
public class NewContactAttr {

    /**
     * @zm-api-field-tag attr-name
     * @zm-api-field-description Attribute name
     */
    @XmlAttribute(name=MailConstants.A_ATTRIBUTE_NAME /* n */, required=true)
    @GraphQLInputField(name=GqlConstants.NAME, description="Attribute name")
    private String name;

    /**
     * @zm-api-field-tag upload-id
     * @zm-api-field-description Upload ID
     */
    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    @GraphQLInputField(name=GqlConstants.ATTACHMENT_ID, description ="Upload ID")
    private String attachId;

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID.  Used in combination with <b>subpart-name</b>
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    @GraphQLInputField(name=GqlConstants.ID, description ="Item ID. Used in combination with part")
    private Integer id;

    /**
     * @zm-api-field-tag subpart-name
     * @zm-api-field-description Subpart Name
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    @GraphQLInputField(name=GqlConstants.PART, description ="Subpart name")
    private String part;

    /**
     * @zm-api-field-tag attr-data
     * @zm-api-field-description Attribute data
     * <br />Date related attributes like "birthday" and "anniversary" SHOULD use <b>"yyyy-MM-dd"</b> format or,
     * if the year isn't specified <b>"--MM-dd"</b> format
     */
    @XmlValue
    @GraphQLInputField(name=GqlConstants.VALUE, description ="Attribute value")
    private String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NewContactAttr() {
         this((String) null);
    }

    public NewContactAttr(@GraphQLNonNull @GraphQLInputField String name) {
         this.name = name;
    }

    public static NewContactAttr fromNameAndValue(String name, String value) {
        final NewContactAttr ncs = new NewContactAttr(name);
        ncs.setValue(value);
        return ncs;
    }

    @GraphQLIgnore
    public NewContactAttr setName(String name) { this.name = name; return this; }
    @GraphQLInputField(name=GqlConstants.ATTACHMENT_ID, description ="Upload ID")
    public NewContactAttr setAttachId(String attachId) { this.attachId = attachId; return this; }
    @GraphQLInputField(name=GqlConstants.ID, description ="Item ID. Used in combination with part")
    public NewContactAttr setId(Integer id) { this.id = id; return this; }
    @GraphQLInputField(name=GqlConstants.PART, description ="Subpart name")
    public NewContactAttr setPart(String part) { this.part = part; return this; }
    @GraphQLInputField(name=GqlConstants.VALUE, description ="Attribute value")
    public NewContactAttr setValue(String value) { this.value = value; return this; }

    public String getName() { return name; }
    public String getAttachId() { return attachId; }
    public Integer getId() { return id; }
    public String getPart() { return part; }
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("attachId", attachId)
            .add("id", id)
            .add("part", part)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
