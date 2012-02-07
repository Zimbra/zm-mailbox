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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-description Undeploy Zimlet
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_UNDEPLOY_ZIMLET_REQUEST)
public class UndeployZimletRequest {

    /**
     * @zm-api-field-tag zimlet-name
     * @zm-api-field-description Zimlet name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-description Action
     */
    @XmlAttribute(name=AdminConstants.A_ACTION, required=false)
    private final String action;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private UndeployZimletRequest() {
        this((String) null, (String) null);
    }

    public UndeployZimletRequest(String name, String action) {
        this.name = name;
        this.action = action;
    }

    public String getName() { return name; }
    public String getAction() { return action; }
}
