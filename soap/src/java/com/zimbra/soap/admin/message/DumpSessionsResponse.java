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
