/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.header;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.soap.type.AuthTokenControl;

@XmlRootElement(namespace="urn:zimbra", name="context")
@XmlType(namespace="urn:zimbra", name="HeaderContext", propOrder = {})
public class HeaderContext {
    @XmlElement(name="authToken", required=false) private String authToken;
    @XmlElement(name="session", required=false) private String sessionId;
    @XmlElement(name="account", required=false) private String account;
    @XmlElement(name="change", required=false) private String change;
    @XmlElement(name="targetServer", required=false) private String targetServer;
    @XmlElement(name="userAgent", required=false) private String userAgent;
    @XmlElement(name="authTokenControl", required=false) private AuthTokenControl authTokenControl;

    public String getAuthToken() {
        return authToken;
    }
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    public String getAccount() {
        return account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getChange() {
        return change;
    }
    public void setChange(String change) {
        this.change = change;
    }
    public String getTargetServer() {
        return targetServer;
    }
    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }
    public String getUserAgent() {
        return userAgent;
    }
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    public void setAuthTokenControlVoidOnExpired(boolean voidExpired) {
        authTokenControl = new AuthTokenControl(voidExpired);
    }
    public AuthTokenControl getAuthTokenControl() {
        return authTokenControl;
    }
}
