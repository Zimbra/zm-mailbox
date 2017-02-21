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
import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.EffectiveRightsInfo;
import com.zimbra.soap.admin.type.RightsEntriesInfo;
import com.zimbra.soap.admin.type.InDomainInfo;
import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
public class EffectiveRightsTarget {

    /**
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final TargetType type;
    /**
     * @zm-api-field-description Effective rights
     */
    @XmlElement(name=AdminConstants.E_ALL /* all */, required=false)
    private final EffectiveRightsInfo all;

    /**
     * @zm-api-field-description inDomains
     */
    @XmlElement(name=AdminConstants.E_IN_DOMAINS /* inDomains */, required=false)
    private List <InDomainInfo> inDomainLists = Lists.newArrayList();

    /**
     * @zm-api-field-description Entries
     */
    @XmlElement(name=AdminConstants.E_ENTRIES /* entries */, required=false)
    private List <RightsEntriesInfo> entriesLists = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveRightsTarget() {
        this(null, null);
    }

    public EffectiveRightsTarget(TargetType type,
            EffectiveRightsInfo all) {
        this.type = type;
        this.all = all;
    }

    public EffectiveRightsTarget setInDomainLists(
                    Collection <InDomainInfo> inDomainLists) {
        this.inDomainLists.clear();
        if (inDomainLists != null) {
            this.inDomainLists.addAll(inDomainLists);
        }
        return this;
    }

    public EffectiveRightsTarget addInDomainList(InDomainInfo inDomainList) {
        inDomainLists.add(inDomainList);
        return this;
    }

    public EffectiveRightsTarget setEntriesLists(
                    Collection <RightsEntriesInfo> entriesLists) {
        this.entriesLists.clear();
        if (entriesLists != null) {
            this.entriesLists.addAll(entriesLists);
        }
        return this;
    }

    public EffectiveRightsTarget addEntriesList(RightsEntriesInfo entriesList) {
        entriesLists.add(entriesList);
        return this;
    }

    public List <RightsEntriesInfo> getEntriesLists() {
        return Collections.unmodifiableList(entriesLists);
    }

    public List <InDomainInfo> getInDomainLists() {
        return Collections.unmodifiableList(inDomainLists);
    }

    public EffectiveRightsInfo getAll() {
        return all;
    }

    public TargetType getType() { return type; }
}
