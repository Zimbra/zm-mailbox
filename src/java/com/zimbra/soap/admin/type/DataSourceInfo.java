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
public class DataSourceInfo extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag data-source-name
     * @zm-api-field-description Data source name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-tag data-source-id
     * @zm-api-field-description Data source id
     */
    @XmlAttribute(name=AccountConstants.A_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-tag data-source-type
     * @zm-api-field-description Data source type
     */
    @XmlAttribute(name=AccountConstants.A_TYPE, required=true)
    private final DataSourceType type;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DataSourceInfo() {
        this((String) null, (String) null, (DataSourceType) null);
    }

    public DataSourceInfo(String name, String id, DataSourceType type) {
        this.name = name;
        this.id = id;
        this.type = type;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public DataSourceType getType() { return type; }
}
