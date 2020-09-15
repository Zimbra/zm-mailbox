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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DocumentActionSelector
extends ActionSelector {

    // Used for "!grant" operation
    /**
     * @zm-api-field-tag zimbra-id-of-grant-to-revoke
     * @zm-api-field-description Zimbra ID of the grant to revoke (Used for "!grant" operation)
     */
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    @XmlAttribute(name=MailConstants.A_ZIMBRA_USER_EMAIL /* zemail */, required=false)
    private String zimbraUserEmail;

    /**
     * @zm-api-field-description Used for "grant" operation
     */
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    private DocumentActionGrant grant;

    public DocumentActionSelector() {
        super();
    }

    public DocumentActionSelector(String ids, String operation) {
        super(ids, operation);
    }

    public static DocumentActionSelector createForIdsAndOperation(String ids, String operation) {
        return new DocumentActionSelector(ids, operation);
    }

    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setZimbraUserEmail(String zimbraUserEmail) { this.zimbraUserEmail = zimbraUserEmail; }
    public void setGrant(DocumentActionGrant grant) { this.grant = grant; }
    public String getZimbraId() { return zimbraId; }
    public String getZimbraUserEmail() { return zimbraUserEmail; }
    public DocumentActionGrant getGrant() { return grant; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("zimbraId", zimbraId)
            .add("zimbraUserEmail", zimbraUserEmail)
            .add("grant", grant);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
