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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class SyncDeletedInfo {

    @XmlAttribute(name=MailConstants.A_IDS, required=true)
    private final String ids;

    @XmlElements({
        @XmlElement(name=MailConstants.E_FOLDER,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_SEARCH,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_MOUNT,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_TAG,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_CONV,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_CHAT,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_MSG,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_CONTACT,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_APPOINTMENT,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_TASK,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_NOTES,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_WIKIWORD,
            type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_DOC,
            type=IdsAttr.class)
    })
    private List<IdsAttr> types = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SyncDeletedInfo() {
        this((String) null);
    }

    public SyncDeletedInfo(String ids) {
        this.ids = ids;
    }

    public void setTypes(Iterable <IdsAttr> types) {
        this.types.clear();
        if (types != null) {
            Iterables.addAll(this.types,types);
        }
    }

    public SyncDeletedInfo addTyp(IdsAttr typ) {
        this.types.add(typ);
        return this;
    }

    public String getIds() { return ids; }
    public List<IdsAttr> getTypes() {
        return Collections.unmodifiableList(types);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("ids", ids)
            .add("types", types)
            .toString();
    }
}
