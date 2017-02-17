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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class QueueQueryField {

    /**
     * @zm-api-field-tag field-name
     * @zm-api-field-description Field name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-description Match specification
     */
    @XmlElement(name=AdminConstants.E_MATCH, required=false)
    private List<ValueAttrib> matches = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QueueQueryField() {
        this((String) null);
    }

    public QueueQueryField(String name) {
        this.name = name;
    }

    public void setMatches(Iterable <ValueAttrib> matches) {
        this.matches.clear();
        if (matches != null) {
            Iterables.addAll(this.matches,matches);
        }
    }

    public QueueQueryField addMatch(ValueAttrib match) {
        this.matches.add(match);
        return this;
    }

    public String getName() { return name; }
    public List<ValueAttrib> getMatches() {
        return Collections.unmodifiableList(matches);
    }
}
