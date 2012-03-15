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

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ALIAS)
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
