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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.IdVersionName;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SAVE_DOCUMENT_RESPONSE)
public class SaveDocumentResponse {

    /**
     * @zm-api-field-description Details of saved document revision
     */
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=false)
    private IdVersionName doc;

    public SaveDocumentResponse() {
    }

    public void setDoc(IdVersionName doc) { this.doc = doc; }
    public IdVersionName getDoc() { return doc; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
