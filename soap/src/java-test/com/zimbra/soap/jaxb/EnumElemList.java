/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/** Test JAXB class with an XmlElement list of enums */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="enum-elem-list")
public class EnumElemList {
    @XmlElement(name="enum-entry", required=false)
    private List<ViewEnum> entries = Lists.newArrayList();

    public EnumElemList() { }

    public void setEntries(Iterable <ViewEnum> entries) {
        this.entries.clear();
        if (entries != null) {
            Iterables.addAll(this.entries,entries);
        }
    }

    public void addEntry(ViewEnum entry) { this.entries.add(entry); }
    public List<ViewEnum> getEntries() { return entries; }
}
