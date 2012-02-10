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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

@XmlAccessorType(XmlAccessType.NONE)
public class DataSourceSpecifier extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag data-source-type
     * @zm-api-field-description Data source type
     */
    @XmlAttribute(name=AccountConstants.A_TYPE, required=true)
    private final DataSourceType type;

    /**
     * @zm-api-field-tag data-source-name
     * @zm-api-field-description Data source name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DataSourceSpecifier() {
        this((DataSourceType) null, (String) null);
    }

    public DataSourceSpecifier(DataSourceType type, String name) {
        this.type = type;
        this.name = name;
    }

    public DataSourceType getType() { return type; }
    public String getName() { return name; }
}
