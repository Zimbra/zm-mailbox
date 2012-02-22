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
public class SyncDeletedInfo {

    /**
     * @zm-api-field-tag deleted-ids
     * @zm-api-field-description IDs of deleted items
     */
    @XmlAttribute(name=MailConstants.A_IDS /* ids */, required=true)
    private final String ids;

    /**
     * @zm-api-field-description Details of deletes broken down by item type (present if "typed" was specified
     * in the request)
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_FOLDER /* folder */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_SEARCH /* search */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_MOUNT /* link */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_TAG /* tag */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_CONV /* c */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_CHAT /* chat */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_MSG /* m */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_CONTACT /* cn */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_NOTES /* notes */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_WIKIWORD /* w */, type=IdsAttr.class),
        @XmlElement(name=MailConstants.E_DOC /* doc */, type=IdsAttr.class)
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

    public SyncDeletedInfo addType(IdsAttr type) {
        this.types.add(type);
        return this;
    }

    public String getIds() { return ids; }
    public List<IdsAttr> getTypes() {
        return Collections.unmodifiableList(types);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("ids", ids)
            .add("types", types);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)) .toString();
    }
}
