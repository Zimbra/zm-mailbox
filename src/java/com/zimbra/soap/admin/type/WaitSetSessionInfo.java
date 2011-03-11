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
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class WaitSetSessionInfo {

    @XmlAttribute(name="interestMask", required=true)
    private final String interestMask;

    @XmlAttribute(name="highestChangeId", required=true)
    private final int highestChangeId;

    @XmlAttribute(name="lastAccessTime", required=true)
    private final long lastAccessTime;

    @XmlAttribute(name="creationTime", required=true)
    private final long creationTime;

    @XmlAttribute(name="sessionId", required=true)
    private final String sessionId;

    @XmlAttribute(name=MailConstants.A_TOKEN, required=false)
    private String token;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WaitSetSessionInfo() {
        this((String) null, -1, -1L, -1L, (String) null);
    }

    public WaitSetSessionInfo(String interestMask, int highestChangeId,
                long lastAccessTime, long creationTime, String sessionId) {
        this.interestMask = interestMask;
        this.highestChangeId = highestChangeId;
        this.lastAccessTime = lastAccessTime;
        this.creationTime = creationTime;
        this.sessionId = sessionId;
    }

    public void setToken(String token) { this.token = token; }
    public String getInterestMask() { return interestMask; }
    public int getHighestChangeId() { return highestChangeId; }
    public long getLastAccessTime() { return lastAccessTime; }
    public long getCreationTime() { return creationTime; }
    public String getSessionId() { return sessionId; }
    public String getToken() { return token; }
}
