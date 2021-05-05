/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="DocumentActionResult", description="Document action response")
public class DocumentActionResult extends ActionResult {

    /**
     * @zm-api-field-tag grantee-zimbra-id
     * @zm-api-field-description Grantee Zimbra ID
     */
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    @GraphQLQuery(name="zimbraId", description="Grantee Zimbra Id")
    private String zimbraId;

    /**
     * @zm-api-field-tag display-name
     * @zm-api-field-description Display name
     */
    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    @GraphQLQuery(name="displayName", description="Display name")
    private String displayName;

    /**
     * @zm-api-field-tag access-key
     * @zm-api-field-description Access key (Password)
     */
    @XmlAttribute(name=MailConstants.A_ACCESSKEY /* key */, required=false)
    @GraphQLQuery(name="accessKey", description="Access key (password)")
    private String accessKey;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DocumentActionResult() {
        this((String) null, (String) null);
    }

    public DocumentActionResult(String id, String operation) {
        super(id, operation);
    }


    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    @GraphQLQuery(name="zimbraId", description="Grantee Zimbra Id")
    public String getZimbraId() { return zimbraId; }
    @GraphQLQuery(name="displayName", description="Display name")
    public String getDisplayName() { return displayName; }
    @GraphQLQuery(name="accessKey", description="Access key (password)")
    public String getAccessKey() { return accessKey; }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("zimbraId", zimbraId)
            .add("displayName", displayName)
            .add("accessKey", accessKey);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
