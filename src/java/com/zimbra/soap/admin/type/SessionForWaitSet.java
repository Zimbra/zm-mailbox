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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class SessionForWaitSet {

    @XmlAttribute(name=MailConstants.A_ACCOUNT, required=true)
    private final String account;

    @XmlAttribute(name=MailConstants.A_TYPES, required=true)
    private final String interests;

    @XmlAttribute(name=MailConstants.A_TOKEN, required=false)
    private String token;

    @XmlAttribute(name="mboxSyncToken", required=false)
    private Integer mboxSyncToken;

    @XmlAttribute(name="mboxSyncTokenDiff", required=false)
    private Integer mboxSyncTokenDiff;

    @XmlElement(name="WaitSetSession", required=false)
    private WaitSetSessionInfo waitSetSession;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SessionForWaitSet() {
        this((String) null, (String) null);
    }

    public SessionForWaitSet(String account, String interests) {
        this.account = account;
        this.interests = interests;
    }

    public void setToken(String token) { this.token = token; }
    public void setMboxSyncToken(Integer mboxSyncToken) {
        this.mboxSyncToken = mboxSyncToken;
    }

    public void setMboxSyncTokenDiff(Integer mboxSyncTokenDiff) {
        this.mboxSyncTokenDiff = mboxSyncTokenDiff;
    }

    public void setWaitSetSession(WaitSetSessionInfo waitSetSession) {
        this.waitSetSession = waitSetSession;
    }

    public String getAccount() { return account; }
    public String getInterests() { return interests; }
    public String getToken() { return token; }
    public Integer getMboxSyncToken() { return mboxSyncToken; }
    public Integer getMboxSyncTokenDiff() { return mboxSyncTokenDiff; }
    public WaitSetSessionInfo getWaitSetSession() { return waitSetSession; }
}
