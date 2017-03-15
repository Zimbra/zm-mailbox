/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

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
     * @zm-api-field-tag acct-id-error
     * @zm-api-field-description Account ID stored in WaitSetAccount object.  Differs from <b>account</b> value.
     */
    @XmlAttribute(name="acctIdError", required=false)
    private String acctIdError;

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

    public String getAcctIdError() { return acctIdError; }
    public void setAcctIdError(String acctIdError) { this.acctIdError = acctIdError; }
}
