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

@XmlAccessorType(XmlAccessType.NONE)
public class SessionForWaitSet {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=MailConstants.A_ACCOUNT /* account */, required=true)
    private final String account;

    /**
     * @zm-api-field-tag interests-types
     * @zm-api-field-description Interest types - Either <b>all</b> or some combination of the letters: <b>mcatd</b>
     * Which stand for Message, Contact, Appointment, Task and Document respectively
     */
    @XmlAttribute(name=MailConstants.A_TYPES /* types */, required=true)
    private final String interests;

    /**
     * @zm-api-field-tag last-known-sync-token
     * @zm-api-field-description Last known sync token
     */
    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    /**
     * @zm-api-field-tag mailbox-sync-token
     * @zm-api-field-description Mailbox sync token
     */
    @XmlAttribute(name="mboxSyncToken", required=false)
    private Integer mboxSyncToken;

    /**
     * @zm-api-field-tag mboxSyncTokenDiff
     * @zm-api-field-description mboxSyncTokenDiff
     */
    @XmlAttribute(name="mboxSyncTokenDiff", required=false)
    private Integer mboxSyncTokenDiff;

    /**
     * @zm-api-field-description WaitSet session Information
     */
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
