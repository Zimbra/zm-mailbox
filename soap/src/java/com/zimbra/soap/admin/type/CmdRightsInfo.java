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
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
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
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_RIGHTS /* rights */, required=true)
    @XmlElement(name=AdminConstants.E_RIGHT /* right */, required=false)
    private List <NamedElement> rights = Lists.newArrayList();

    /**
     * @zm-api-field-description Notes
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_DESC /* desc */, required=true)
    @XmlElement(name=AdminConstants.E_NOTE /* note */, required=false)
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
