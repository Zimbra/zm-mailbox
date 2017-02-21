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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.RightWithName;

@XmlAccessorType(XmlAccessType.NONE)
public class EffectiveRightsInfo {
    /**
     * @zm-api-field-description Rights
     */
    @XmlElement(name=AdminConstants.E_RIGHT /* right */, required=false)
    private List <RightWithName> rights = Lists.newArrayList();

    /**
     * @zm-api-field-description All attributes that can be set
     */
    @XmlElement(name=AdminConstants.E_SET_ATTRS /* setAttrs */, required=true)
    private final EffectiveAttrsInfo setAttrs;

    /**
     * @zm-api-field-description All attributes that can be got
     */
    @XmlElement(name=AdminConstants.E_GET_ATTRS /* getAttrs */, required=true)
    private final EffectiveAttrsInfo getAttrs;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveRightsInfo() {
        this(null, null, null);
    }

    public EffectiveRightsInfo(EffectiveAttrsInfo setAttrs,
            EffectiveAttrsInfo getAttrs) {
        this(null, setAttrs, getAttrs);
    }

    public EffectiveRightsInfo(Iterable <RightWithName> rights,
            EffectiveAttrsInfo setAttrs, EffectiveAttrsInfo getAttrs) {
        setRights(rights);
        this.setAttrs = setAttrs;
        this.getAttrs = getAttrs;
    }

    public EffectiveRightsInfo setRights(Iterable <RightWithName> rights) {
        this.rights.clear();
        if (rights != null) {
            Iterables.addAll(this.rights,rights);
        }
        return this;
    }

    public EffectiveRightsInfo addRight(RightWithName right) {
        rights.add(right);
        return this;
    }

    public List <RightWithName> getRights() {
        return Collections.unmodifiableList(rights);
    }

    public EffectiveAttrsInfo getSetAttrs() { return setAttrs; }
    public EffectiveAttrsInfo getGetAttrs() { return getAttrs; }
}
