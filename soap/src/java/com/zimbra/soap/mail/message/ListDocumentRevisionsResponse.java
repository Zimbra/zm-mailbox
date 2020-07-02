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
import com.zimbra.soap.mail.type.IdEmailName;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_LIST_DOCUMENT_REVISIONS_RESPONSE)
@XmlType(propOrder = { "revisions", "users" })
public class ListDocumentRevisionsResponse {

    /**
     * @zm-api-field-description Document revision information
     */
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=false)
    private List<DocumentInfo> revisions = Lists.newArrayList();

    /**
     * @zm-api-field-description User information
     */
    @XmlElement(name=MailConstants.A_USER /* user */, required=false)
    private List<IdEmailName> users = Lists.newArrayList();

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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("revisions", revisions)
            .add("users", users);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
