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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZimletContext {

    @XmlAttribute(name=AccountConstants.A_ZIMLET_BASE_URL, required=true)
    private final String baseUrl;
    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRIORITY, required=false)
    private final Integer priority;
    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRESENCE, required=true)
    private final String presence;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ZimletContext() {
        this((String) null, (Integer) null, (String) null);
    }

    public ZimletContext(String baseUrl, String presence) {
        this(baseUrl, (Integer) null, presence);
    }

    public ZimletContext(String baseUrl, Integer priority, String presence) {
        this.baseUrl = baseUrl;
        this.priority = priority;
        this.presence = presence;
    }

    public String getBaseUrl() { return baseUrl; }
    public Integer getPriority() { return priority; }
    public String getPresence() { return presence; }
}
