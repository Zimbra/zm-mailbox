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
import javax.xml.bind.annotation.XmlElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedElement;
import com.zimbra.soap.admin.type.EffectiveRightsInfo;

@XmlAccessorType(XmlAccessType.NONE)
public class RightsEntriesInfo {

    /**
     * @zm-api-field-description Entries
     */
    @XmlElement(name=AdminConstants.E_ENTRY /* entry */, required=false)
    private List <NamedElement> entries = Lists.newArrayList();

    /**
     * @zm-api-field-description Effective rights
     */
    @XmlElement(name=AdminConstants.E_RIGHTS /* rights */, required=true)
    private final EffectiveRightsInfo rights;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RightsEntriesInfo() {
        this(null, null);
    }

    public RightsEntriesInfo(EffectiveRightsInfo rights) {
        this(null, rights);
    }

    public RightsEntriesInfo(Collection <NamedElement> entries,
            EffectiveRightsInfo rights) {
        this.rights = rights;
        setEntries(entries);
    }

    public RightsEntriesInfo setEntries(Collection <NamedElement> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
        return this;
    }

    public RightsEntriesInfo addEntry(NamedElement entry) {
        entries.add(entry);
        return this;
    }

    public List <NamedElement> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public EffectiveRightsInfo getRights() { return rights; }
}
