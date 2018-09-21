/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlType(propOrder = {})
@GraphQLType(name=GqlConstants.CLASS_SIGNATURE_CONTENT, description="Content of the signature")
public class SignatureContent {

    /**
     * @zm-api-field-tag signature-content-type
     * @zm-api-field-description Content Type - <b>"text/plain"</b> or <b>"text/html"</b>
     */
    @XmlAttribute(name=AccountConstants.A_TYPE)
    private String contentType;

    /**
     * @zm-api-field-tag signature-value
     * @zm-api-field-description Signature value
     */
    @XmlValue private String content;

    public SignatureContent() {
    }

    public SignatureContent(String content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    @GraphQLQuery(name=GqlConstants.CONTENT_TYPE, description="Content Type - \"text/plain\" or \"text/html\"")
    public String getContentType() {
        return contentType;
    }

    @GraphQLInputField(name=GqlConstants.CONTENT_TYPE, description="Content Type - \"text/plain\" or \"text/html\"")
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @GraphQLQuery(name=GqlConstants.CONTENT, description="Signature value")
    public String getContent() {
        return content;
    }

    @GraphQLInputField(name=GqlConstants.CONTENT, description="Signature value")
    public void setContent(String content) {
        this.content = content;
    }
}
