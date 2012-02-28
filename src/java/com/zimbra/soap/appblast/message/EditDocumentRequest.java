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

package com.zimbra.soap.appblast.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AppBlastConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.IdVersion;

/**
 * @zm-api-command-description Edit a document
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AppBlastConstants.E_EDIT_REQUEST)
public class EditDocumentRequest {

    /**
     * @zm-api-field-description Specifies the document to edit
     */
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=true)
    private final IdVersion doc;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EditDocumentRequest() {
        this((IdVersion) null);
    }

    public EditDocumentRequest(IdVersion doc) {
        this.doc = doc;
    }

    public IdVersion getDoc() { return doc; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
