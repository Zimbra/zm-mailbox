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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.InfoForSessionType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DUMP_SESSIONS_RESPONSE)
@XmlType(propOrder = {"soapSessions", "imapSessions", "adminSessions",
                "wikiSessions", "synclistenerSessions", "waitsetSessions"})
public class DumpSessionsResponse {

    /**
     * @zm-api-field-tag total-active-session-count
     * @zm-api-field-description Count of active sessions
     */
    @XmlAttribute(name=AdminConstants.A_ACTIVE_SESSIONS, required=true)
    private final int totalActiveSessions;

    // Mapped from Session.Type.SOAP
    /**
     * @zm-api-field-description Information about SOAP sessions
     */
    @XmlElement(name="soap", required=false)
    private InfoForSessionType soapSessions;

    // Mapped from Session.Type.IMAP
    /**
     * @zm-api-field-description Information about IMAP sessions
     */
    @XmlElement(name="imap", required=false)
    private InfoForSessionType imapSessions;

    // Mapped from Session.Type.ADMIN
    /**
     * @zm-api-field-description Information about ADMIn sessions
     */
    @XmlElement(name="admin", required=false)
    private InfoForSessionType adminSessions;

    // Mapped from Session.Type.WIKI
    /**
     * @zm-api-field-description Information about WIKI sessions
     */
    @XmlElement(name="wiki", required=false)
    private InfoForSessionType wikiSessions;

    // Mapped from Session.Type.SYNCLISTENER
    /**
     * @zm-api-field-description Information about SYNCLISTENER sessions
     */
    @XmlElement(name="synclistener", required=false)
    private InfoForSessionType synclistenerSessions;

    // Mapped from Session.Type.WAITSET
    /**
     * @zm-api-field-description Information about WaitSet sessions
     */
    @XmlElement(name="waitset", required=false)
    private InfoForSessionType waitsetSessions;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DumpSessionsResponse() {
        this(-1);
    }

    public DumpSessionsResponse(int totalActiveSessions) {
        this.totalActiveSessions = totalActiveSessions;
    }

    public void setSoapSessions(InfoForSessionType soapSessions) {
        this.soapSessions = soapSessions;
    }

    public void setImapSessions(InfoForSessionType imapSessions) {
        this.imapSessions = imapSessions;
    }

    public void setAdminSessions(InfoForSessionType adminSessions) {
        this.adminSessions = adminSessions;
    }

    public void setWikiSessions(InfoForSessionType wikiSessions) {
        this.wikiSessions = wikiSessions;
    }

    public void setSynclistenerSessions(
                    InfoForSessionType synclistenerSessions) {
        this.synclistenerSessions = synclistenerSessions;
    }

    public void setWaitsetSessions(InfoForSessionType waitsetSessions) {
        this.waitsetSessions = waitsetSessions;
    }

    public int getTotalActiveSessions() { return totalActiveSessions; }
    public InfoForSessionType getSoapSessions() { return soapSessions; }
    public InfoForSessionType getImapSessions() { return imapSessions; }
    public InfoForSessionType getAdminSessions() { return adminSessions; }
    public InfoForSessionType getWikiSessions() { return wikiSessions; }

    public InfoForSessionType getSynclistenerSessions() {
        return synclistenerSessions;
    }

    public InfoForSessionType getWaitsetSessions() { return waitsetSessions; }
}
