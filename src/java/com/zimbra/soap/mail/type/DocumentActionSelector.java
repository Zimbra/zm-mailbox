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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DocumentActionSelector
extends ActionSelector {

    // Used for "!grant" operation
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    // Used for "grant" operation
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
    public void setGrant(DocumentActionGrant grant) { this.grant = grant; }
    public String getZimbraId() { return zimbraId; }
    public DocumentActionGrant getGrant() { return grant; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("zimbraId", zimbraId)
            .add("grant", grant);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
