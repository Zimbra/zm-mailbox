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

package com.zimbra.soap.admin.type;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedElement;

@XmlAccessorType(XmlAccessType.NONE)
public class CmdRightsInfo {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=false)
    private final String name;

    /**
     * @zm-api-field-description Rights
     */
    @XmlElementWrapper(name=AdminConstants.E_RIGHTS, required=true)
    @XmlElement(name=AdminConstants.E_RIGHT, required=false)
    private List <NamedElement> rights = Lists.newArrayList();

    /**
     * @zm-api-field-description Notes
     */
    @XmlElementWrapper(name=AdminConstants.E_DESC, required=true)
    @XmlElement(name=AdminConstants.E_NOTE, required=false)
    private List <String> notes = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CmdRightsInfo() { this(null); }

    public CmdRightsInfo(String name) { this.name = name; }

    public CmdRightsInfo setRights(Collection <NamedElement> rights) {
        this.rights.clear();
        if (rights != null) {
            this.rights.addAll(rights);
        }
        return this;
    }

    public CmdRightsInfo addRight(NamedElement right) {
        rights.add(right);
        return this;
    }

    public List<NamedElement> getRights() {
        return Collections.unmodifiableList(rights);
    }

    public CmdRightsInfo setNotes(Collection <String> notes) {
        this.notes.clear();
        if (notes != null) {
            this.notes.addAll(notes);
        }
        return this;
    }

    public CmdRightsInfo addNote(String note) {
        notes.add(note);
        return this;
    }

    public List <String> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public String getName() { return name; }
}
