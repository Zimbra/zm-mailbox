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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class GranteeChooser {

    //See ACL.stringToType for valid (case insensitive) grantee types
    /**
     * @zm-api-field-tag grantee-type
     * @zm-api-field-description If specified, filters the result by the specified grantee type.
     */
    @XmlAttribute(name=AdminConstants.A_TYPE, required=false)
    private final String type;
    /**
     * @zm-api-field-tag grantee-id
     * @zm-api-field-description If specified, filters the result by the specified grantee ID.
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=false)
    private final String id;
    /**
     * @zm-api-field-tag grantee-name
     * @zm-api-field-description If specified, filters the result by the specified grantee name.
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=false)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GranteeChooser() {
        this(null, null, null);
    }

    public GranteeChooser(String type, String id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public String getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
}
