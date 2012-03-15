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
import com.zimbra.soap.mail.type.ListDocumentRevisionsSpec;

/**
 * @zm-api-command-description Returns <b>{num}</b> number of revisions starting from <b>{version}</b> of the
 * requested document.  <b>{num}</b> defaults to 1.  <b>{version}</b> defaults to the current version.
 * <br />
 * Documents that have multiple revisions have the flag "/", which indicates that the document is versioned.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_LIST_DOCUMENT_REVISIONS_REQUEST)
public class ListDocumentRevisionsRequest {

    /**
     * @zm-api-field-description Specification for the list of document revisions
     */
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=true)
    private final ListDocumentRevisionsSpec doc;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ListDocumentRevisionsRequest() {
        this((ListDocumentRevisionsSpec) null);
    }

    public ListDocumentRevisionsRequest(ListDocumentRevisionsSpec doc) {
        this.doc = doc;
    }

    public ListDocumentRevisionsSpec getDoc() { return doc; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
