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
