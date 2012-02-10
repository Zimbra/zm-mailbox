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
