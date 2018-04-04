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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ListDocumentRevisionsSpec;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("doc", doc);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
