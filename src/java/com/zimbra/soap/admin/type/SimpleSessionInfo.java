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

@XmlAccessorType(XmlAccessType.FIELD)
public class SimpleSessionInfo {

    @XmlAttribute(name=AdminConstants.A_ZIMBRA_ID, required=true)
    private final String zimbraId;

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    @XmlAttribute(name=AdminConstants.A_SESSION_ID, required=true)
    private final String sessionId;

    @XmlAttribute(name=AdminConstants.A_CREATED_DATE, required=true)
    private final long createdDate;

    @XmlAttribute(name=AdminConstants.A_LAST_ACCESSED_DATE, required=true)
    private final long lastAccessedDate;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SimpleSessionInfo() {
        this((String) null, (String) null, (String) null, -1L, -1L);
    }

    public SimpleSessionInfo(String zimbraId, String name, String sessionId,
                    long createdDate, long lastAccessedDate) {
        this.zimbraId = zimbraId;
        this.name = name;
        this.sessionId = sessionId;
        this.createdDate = createdDate;
        this.lastAccessedDate = lastAccessedDate;
    }

    public String getZimbraId() { return zimbraId; }
    public String getName() { return name; }
    public String getSessionId() { return sessionId; }
    public long getCreatedDate() { return createdDate; }
    public long getLastAccessedDate() { return lastAccessedDate; }
}
