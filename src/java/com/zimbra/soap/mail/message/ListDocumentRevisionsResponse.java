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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.DocumentInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_LIST_DOCUMENT_REVISIONS_RESPONSE)
@XmlType(propOrder = {})
public class ListDocumentRevisionsResponse {

    @XmlElement(name=MailConstants.E_DOC /* doc */, required=false)
    private List<DocumentInfo> revisions = Lists.newArrayList();

    public ListDocumentRevisionsResponse() {
    }

    public void setRevisions(Iterable <DocumentInfo> revisions) {
        this.revisions.clear();
        if (revisions != null) {
            Iterables.addAll(this.revisions,revisions);
        }
    }

    public ListDocumentRevisionsResponse addRevision(DocumentInfo revision) {
        this.revisions.add(revision);
        return this;
    }

    public List<DocumentInfo> getRevisions() {
        return Collections.unmodifiableList(revisions);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("revisions", revisions);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
