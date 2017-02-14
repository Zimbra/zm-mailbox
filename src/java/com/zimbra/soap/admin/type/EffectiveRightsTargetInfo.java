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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
public class EffectiveRightsTargetInfo extends EffectiveRightsInfo {

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private final TargetType type;

    /**
     * @zm-api-field-tag target-id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag target-name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveRightsTargetInfo() {
        this((TargetType) null, (String) null,
                (String) null, (Iterable <RightWithName>) null,
                (EffectiveAttrsInfo) null, (EffectiveAttrsInfo) null);
    }

    public EffectiveRightsTargetInfo(TargetType type,
            String id, String name,
            EffectiveAttrsInfo setAttrs, EffectiveAttrsInfo getAttrs) {
        this(type, id, name, (Iterable <RightWithName>) null,
                setAttrs, getAttrs);
    }

    public EffectiveRightsTargetInfo(TargetType type,
            String id, String name,
            Iterable <RightWithName> rights,
            EffectiveAttrsInfo setAttrs, EffectiveAttrsInfo getAttrs) {
        super(rights, setAttrs, getAttrs);
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public TargetType getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
}
