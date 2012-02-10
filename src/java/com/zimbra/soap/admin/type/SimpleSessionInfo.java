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
public class SimpleSessionInfo {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ZIMBRA_ID /* zid */, required=true)
    private final String zimbraId;

    /**
     * @zm-api-field-tag account-name
     * @zm-api-field-description Account name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag session-id
     * @zm-api-field-description Session ID
     */
    @XmlAttribute(name=AdminConstants.A_SESSION_ID /* sid */, required=true)
    private final String sessionId;

    /**
     * @zm-api-field-tag creation-date
     * @zm-api-field-description Creation date
     */
    @XmlAttribute(name=AdminConstants.A_CREATED_DATE /* cd */, required=true)
    private final long createdDate;

    /**
     * @zm-api-field-tag last-accessed-date
     * @zm-api-field-description Last accessed date
     */
    @XmlAttribute(name=AdminConstants.A_LAST_ACCESSED_DATE /* ld */, required=true)
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
