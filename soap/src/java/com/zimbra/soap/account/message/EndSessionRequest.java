/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description End the current session, removing it from all caches.  Called when the browser app
 * (or other session-using app) shuts down.  Has no effect if called in a &lt;nosession> context.
 */

@XmlRootElement(name=AccountConstants.E_END_SESSION_REQUEST)
public class EndSessionRequest {
    /**
     * @zm-api-field-tag need-can-expand
     * @zm-api-field-description flag whether the <b>{exp}</b> flag is needed in the response for group entries.<br />
     *     default is 0 (false)
     */
    @XmlAttribute(name=AccountConstants.A_LOG_OFF /* logoff */, required=false)
    private ZmBoolean logoff;

    /**
     * @zm-api-field-description flag to clear all web sessions of the user
     *     default is 0 (false)
     */
    @XmlAttribute(name=AccountConstants.A_CLEAR_ALL_SOAP_SESSIONS /* all */, required=false)
    private ZmBoolean clearAllSoapSessions;

    /**
     * @zm-api-field-description flag to decide current session will be cleared or not
     *     default is 0 (false)
     */
    @XmlAttribute(name=AccountConstants.A_EXCLUDE_CURRENT_SESSION /* excludeCurrent */, required=false)
    private ZmBoolean excludeCurrentSession;

    /**
     * @zm-api-field-tag sessionId
     * @zm-api-field-description end session for given session id
     */
    @XmlAttribute(name=AccountConstants.A_SESSION_ID /* sessionId */, required=false)
    private String sessionId;

    /**
     * @zm-api-field-tag refresh-token
     * @zm-api-field-description ZCS-20285: native refresh token to revoke at logout, so it
     *     cannot be used to mint a new access token after this point. Optional - if omitted,
     *     logout proceeds as before and the refresh token (if any) remains valid until its
     *     own natural expiry.
     */
    @XmlElement(name=AccountConstants.E_REFRESH_TOKEN, required=false)
    private String refreshToken;

    /**
     * @zm-api-field-tag refresh-token-salt
     * @zm-api-field-description ZCS-20285: salt for refreshToken - required alongside it for
     *     revocation to be possible.
     */
    @XmlElement(name=AccountConstants.E_REFRESH_TOKEN_SALT, required=false)
    private String refreshTokenSalt;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshTokenSalt() {
        return refreshTokenSalt;
    }

    public void setRefreshTokenSalt(String refreshTokenSalt) {
        this.refreshTokenSalt = refreshTokenSalt;
    }

    public void setLogOff (boolean logoff) {
        this.logoff = ZmBoolean.fromBool(logoff);
    }

    public boolean isClearAllSoapSessions() {
        return ZmBoolean.toBool(clearAllSoapSessions, false);
    }

    public boolean isExcludeCurrentSession() {
        return ZmBoolean.toBool(excludeCurrentSession, false);
    }

    public boolean isLogOff() {
        return ZmBoolean.toBool(this.logoff, false);
    }

    public void setClearAllSoapSessions(boolean clearAllSoapSessions) {
        this.clearAllSoapSessions = ZmBoolean.fromBool(clearAllSoapSessions);
    }

    public void setExcludeCurrentSession(boolean excludeCurrentSession) {
        this.excludeCurrentSession = ZmBoolean.fromBool(excludeCurrentSession);
    }

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @param sessionId the sessionId to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

}
