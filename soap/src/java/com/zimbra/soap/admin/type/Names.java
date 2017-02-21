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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class Names {

    private static Splitter COMMA_SPLITTER =
        Splitter.on(",").trimResults().omitEmptyStrings();

    private static Joiner COMMA_JOINER = Joiner.on(",");

    /**
     * @zm-api-field-tag comma-sep-names
     * @zm-api-field-description Comma separated list of names
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String names;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private Names() {
        this((String) null);
    }

    public Names(String names) {
        this.names = names;
    }

    public Names(List<String> names) {
        if (names == null)
            this.names = null;
        else
            this.names = COMMA_JOINER.join(names);

    }

    public String getNames() { return names; }
    public Iterable<String> getListOfNames() {
        return COMMA_SPLITTER.split(Strings.nullToEmpty(names));
    }
}
