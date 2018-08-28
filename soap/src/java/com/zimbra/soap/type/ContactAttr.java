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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_CONTACT_ATTRIBUTE, description="Contact attribute")
public class ContactAttr extends KeyValuePair {

    // part/contentType/size/contentFilename are required when
    // encoding attachments

    /**
     * @zm-api-field-tag contact-part-id
     * @zm-api-field-description Part ID.
     * <br />
     * Can only specify a <b>{contact-part-id}</b> when modifying an existent contact
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    /**
     * @zm-api-field-tag contact-content-type
     * @zm-api-field-description Content type
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    /**
     * @zm-api-field-tag contact-size
     * @zm-api-field-description Size
     */
    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Integer size;

    /**
     * @zm-api-field-tag contact-content-filename
     * @zm-api-field-description Content filename
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_FILENAME /* filename */, required=false)
    private String contentFilename;

    public ContactAttr() {
    }

    public void setPart(String part) { this.part = part; }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(Integer size) { this.size = size; }
    public void setContentFilename(String contentFilename) {
        this.contentFilename = contentFilename;
    }

    @GraphQLQuery(name=GqlConstants.PART, description="Part ID")
    public String getPart() { return part; }
    @GraphQLQuery(name=GqlConstants.CONTENT_TYPE, description="Content type")
    public String getContentType() { return contentType; }
    @GraphQLQuery(name=GqlConstants.SIZE, description="Size")
    public Integer getSize() { return size; }
    @GraphQLQuery(name=GqlConstants.CONTENT_FILENAME, description="Content filename")
    public String getContentFilename() { return contentFilename; }
}
