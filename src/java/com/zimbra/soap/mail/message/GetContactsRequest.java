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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_GET_CONTACTS_REQUEST)
public class GetContactsRequest {

    @XmlAttribute(name=MailConstants.A_SYNC /* sync */, required=false)
    private Boolean sync;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    // Valid values are case insensitive "names" from enum:
    //     com.zimbra.cs.index.SortBy
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    // TODO:need methods to get/set lists of attribs and contacts
    // The Server side handler copes with mixed order of attibutes and contacts

    @XmlElements({
        @XmlElement(name=MailConstants.E_ATTRIBUTE /* a */,
            type=AttributeName.class),
        @XmlElement(name=MailConstants.E_CONTACT /* cn */,
            type=Id.class)
    })
    private List<Object> elements = Lists.newArrayList();

    public GetContactsRequest() {
    }

    public void setSync(Boolean sync) { this.sync = sync; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setElements(Iterable <Object> elements) {
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements,elements);
        }
    }

    public GetContactsRequest addElement(Object element) {
        this.elements.add(element);
        return this;
    }

    public Boolean getSync() { return sync; }
    public String getFolderId() { return folderId; }
    public String getSortBy() { return sortBy; }
    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sync", sync)
            .add("folderId", folderId)
            .add("sortBy", sortBy)
            .add("elements", elements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
