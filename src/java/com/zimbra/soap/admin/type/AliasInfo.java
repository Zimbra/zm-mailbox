/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
public class AliasInfo extends AdminObjectInfo {

    /**
     * @zm-api-field-tag target-name
     * @zm-api-field-description Target name
     */
    @XmlAttribute(name=AdminConstants.A_TARGETNAME /* targetName */, required=true)
    private final String targetName;

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=false)
    private final TargetType targetType;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AliasInfo() {
        this(null, null, null, null, null);
    }

    public AliasInfo(String id, String name) {
        this(id, name, null, null, null);
    }

    public AliasInfo(String id, String name, Collection <Attr> attrs) {
        this(id, name, null, null, attrs);
    }

    public AliasInfo(String id, String name, String targetName, Collection <Attr> attrs) {
        this(id, name, targetName, null, attrs);
    }

    public AliasInfo(String id, String name, String targetName, TargetType targetType, Collection <Attr> attrs) {
        super(id, name, attrs);
        this.targetName = targetName;
        this.targetType = targetType;
    }

    public String getTargetName() { return targetName; }
    public TargetType getTargetType() { return targetType; }
}
