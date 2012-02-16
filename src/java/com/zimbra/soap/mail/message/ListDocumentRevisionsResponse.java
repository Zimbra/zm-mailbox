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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.DocumentInfo;
import com.zimbra.soap.mail.type.IdEmailName;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_LIST_DOCUMENT_REVISIONS_RESPONSE)
public class ListDocumentRevisionsResponse {

    /**
     * @zm-api-field-description User information
     */
    @XmlElement(name=MailConstants.A_USER /* user */, required=false)
    private List<IdEmailName> users = Lists.newArrayList();

    /**
     * @zm-api-field-description Document revision information
     */
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

    public void addRevision(DocumentInfo revision) {
        this.revisions.add(revision);
    }

    public void setUsers(Iterable <IdEmailName> users) {
        this.users.clear();
        if (users != null) {
            Iterables.addAll(this.users,users);
        }
    }

    public void addUser(IdEmailName user) {
        this.users.add(user);
    }

    public List<DocumentInfo> getRevisions() {
        return Collections.unmodifiableList(revisions);
    }
    public List<IdEmailName> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("revisions", revisions)
            .add("users", users);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
