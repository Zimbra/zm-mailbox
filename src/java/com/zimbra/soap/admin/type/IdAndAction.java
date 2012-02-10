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
public class IdAndAction {

    /**
     * @zm-api-field-tag zimbra-id-of-account
     * @zm-api-field-description Zimbra ID of account
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-tag wiki|contactGroup
     * @zm-api-field-description wiki or contactGroup
     */
    @XmlAttribute(name=AdminConstants.A_ACTION, required=true)
    private final String action;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private IdAndAction() {
        this((String) null, (String) null);
    }

    public IdAndAction(String id, String action) {
        this.id = id;
        this.action = action;
    }

    public String getId() { return id; }
    public String getAction() { return action; }
}
