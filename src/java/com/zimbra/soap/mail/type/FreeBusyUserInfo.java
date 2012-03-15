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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class FreeBusyUserInfo {

    /**
     * @zm-api-field-tag account-email
     * @zm-api-field-description "id" is always account email; it is not zimbraId as the attribute name may suggest
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-description Free/Busy slots
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_FREEBUSY_FREE /* f */, type=FreeBusyFREEslot.class),
        @XmlElement(name=MailConstants.E_FREEBUSY_BUSY /* b */, type=FreeBusyBUSYslot.class),
        @XmlElement(name=MailConstants.E_FREEBUSY_BUSY_TENTATIVE /* t */, type=FreeBusyBUSYTENTATIVEslot.class),
        @XmlElement(name=MailConstants.E_FREEBUSY_BUSY_UNAVAILABLE /* u */, type=FreeBusyBUSYUNAVAILABLEslot.class),
        @XmlElement(name=MailConstants.E_FREEBUSY_NODATA /* n */, type=FreeBusyNODATAslot.class)
    })
    private List<FreeBusySlot> elements = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FreeBusyUserInfo() {
        this((String) null);
    }

    public FreeBusyUserInfo(String id) {
        this.id = id;
    }

    public void setElements(Iterable <FreeBusySlot> elements) {
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements,elements);
        }
    }

    public FreeBusyUserInfo addElement(FreeBusySlot element) {
        this.elements.add(element);
        return this;
    }

    public String getId() { return id; }
    public List<FreeBusySlot> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("elements", elements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
