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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class EffectiveRightsTargetInfo extends EffectiveRightsInfo {

    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final EffectiveRightsTarget.TargetType type;

    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveRightsTargetInfo() {
        this((EffectiveRightsTarget.TargetType) null, (String) null,
                (String) null, (Iterable <RightWithName>) null,
                (EffectiveAttrsInfo) null, (EffectiveAttrsInfo) null);
    }

    public EffectiveRightsTargetInfo(EffectiveRightsTarget.TargetType type,
            String id, String name,
            EffectiveAttrsInfo setAttrs, EffectiveAttrsInfo getAttrs) {
        this(type, id, name, (Iterable <RightWithName>) null,
                setAttrs, getAttrs);
    }

    public EffectiveRightsTargetInfo(EffectiveRightsTarget.TargetType type,
            String id, String name,
            Iterable <RightWithName> rights,
            EffectiveAttrsInfo setAttrs, EffectiveAttrsInfo getAttrs) {
        super(rights, setAttrs, getAttrs);
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public EffectiveRightsTarget.TargetType getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
}
