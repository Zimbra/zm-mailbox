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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ComboRightInfo {

    /**
     * @zm-api-field-tag right-name
     * @zm-api-field-description Right name
     */
    @XmlAttribute(name=AdminConstants.A_N /* n */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description Type
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private final RightInfo.RightType type;

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=AdminConstants.A_TARGET_TYPE /* targetType */, required=false)
    private final String targetType;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ComboRightInfo() {
        this((String)null, (RightInfo.RightType) null, (String)null);
    }

    public ComboRightInfo(String name, RightInfo.RightType type,
            String targetType) {
        this.name = name;
        this.type = type;
        this.targetType = targetType;
    }

    public String getName() { return name; }
    public RightInfo.RightType getType() { return type; }
    public String getTargetType() { return targetType; }
}
