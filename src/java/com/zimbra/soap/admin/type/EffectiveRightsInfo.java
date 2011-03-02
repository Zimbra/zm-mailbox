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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.RightWithName;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"rights", "setAttrs", "getAttrs"})
public class EffectiveRightsInfo {
    @XmlElement(name=AdminConstants.E_RIGHT, required=false)
    private List <RightWithName> rights = Lists.newArrayList();
    @XmlElement(name=AdminConstants.E_SET_ATTRS, required=true)
    private final EffectiveAttrsInfo setAttrs;
    @XmlElement(name=AdminConstants.E_GET_ATTRS, required=true)
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
