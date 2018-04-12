/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.header;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.type.AuthTokenControl;

@XmlRootElement(namespace="urn:zimbra", name="context")
@XmlType(namespace="urn:zimbra", name="HeaderContext", propOrder = {})
public class HeaderContext {

    // Default value 0
    /**
     * @zm-api-field-tag hop-count
     * @zm-api-field-description Number of times this request has been proxied
     */
    @XmlAttribute(name=HeaderConstants.A_HOPCOUNT /* hops */, required=false)
    private Integer hopCount;

    /**
     * @zm-api-field-tag auth-token
     * @zm-api-field-description Auth token
     */
    @XmlElement(name=HeaderConstants.E_AUTH_TOKEN /* authToken */, required=false)
    private String authToken;

    /**
     * @zm-api-field-tag session-info
     * @zm-api-field-description Session information
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_SESSION /* session */, required=false)
    private HeaderSessionInfo session;

    /**
     * @zm-api-field-tag legacy-session-id
     * @zm-api-field-description Deprecated - old way of specifying session information
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_SESSION_ID /* sessionId */, required=false)
    private HeaderSessionInfo legacySessionId;

    /**
     * @zm-api-field-tag no-session-flag
     * @zm-api-field-description Specify that no session is required.  No particular value required.
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_NO_SESSION /* nosession */, required=false)
    private String noSession;

    /**
     * @zm-api-field-tag account-info
     * @zm-api-field-description Account information
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_ACCOUNT /* account */, required=false)
    private HeaderAccountInfo account;

    /**
     * @zm-api-field-tag change-info
     * @zm-api-field-description Change information
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_CHANGE /* change */, required=false)
    private HeaderChangeInfo change;

    /**
     * @zm-api-field-tag proxy-target-server-id}
     * @zm-api-field-description Proxy target server ID
     */
    @XmlElement(name=HeaderConstants.E_TARGET_SERVER /* targetServer */, required=false)
    private String targetServer;

    /**
     * @zm-api-field-tag user-agent-info
     * @zm-api-field-description Information about the user agent
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_USER_AGENT /* userAgent */, required=false)
    private HeaderUserAgentInfo userAgent;

    /**
     * @zm-api-field-tag auth-token-control-info
     * @zm-api-field-description Auth Token control information.
     */
    @XmlElement(name=HeaderConstants.E_AUTH_TOKEN_CONTROL /* authTokenControl */, required=false)
    private AuthTokenControl authTokenControl;

    /**
     * @zm-api-field-tag response-format-info
     * @zm-api-field-description Desired response format information
     */
    @XmlElement(name=HeaderConstants.E_FORMAT /* format */, required=false)
    private HeaderFormatInfo format;

    /**
     * @zm-api-field-tag notify-info
     * @zm-api-field-description Information about which notifications have already been received
     */
    @XmlElement(name=HeaderConstants.E_NOTIFY /* notify */, required=false)
    private HeaderNotifyInfo notify;

    /**
     * @zm-api-field-tag no-notify-flag
     * @zm-api-field-description Specify that no notifications are required.  No particular value needed.
     * <p>Note that a session will not return any notifications if &lt;nonotify/> was ever specified in a
     * &lt;context> request element involving the session.)</p>
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_NO_NOTIFY /* nonotify */, required=false)
    private String noNotify;

    /**
     * @zm-api-field-tag no-qualify-tag
     * @zm-api-field-description Flag that item IDs in responses should not be qualified.  No particular value needed.
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_NO_QUALIFY /* noqualify */, required=false)
    private String noQualify;

    /**
     * @zm-api-field-tag via
     * @zm-api-field-description Information on where the request has come from.
     * <p>format something like: {request-ip(user-agent)[,...]}</p>
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_VIA /* via */, required=false)
    private String via;

    /**
     * @zm-api-field-tag soap-request-id
     * @zm-api-field-description SOAP request ID.
     */
    @ZimbraUniqueElement
    @XmlElement(name=HeaderConstants.E_SOAP_ID /* soapId */, required=false)
    private String soapRequestId;

    /**
     * @zm-api-field-tag csrf-token
     * @zm-api-field-description CSRF token
     */
    @XmlElement(name=HeaderConstants.E_CSRFTOKEN /* csrfToken */, required=false)
    private String csrfToken;

    public HeaderContext() {
    }

    public void setHopCount(Integer hopCount) { this.hopCount = hopCount; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }
    public void setSession(HeaderSessionInfo session) { this.session = session; }
    @Deprecated
    public void setLegacySessionId(HeaderSessionInfo legacySessionId) { this.legacySessionId = legacySessionId; }
    public void setNoSession(String noSession) { this.noSession = noSession; }
    public void setAccount(HeaderAccountInfo account) { this.account = account; }
    public void setChange(HeaderChangeInfo change) { this.change = change; }
    public void setTargetServer(String targetServer) { this.targetServer = targetServer; }
    public void setUserAgent(HeaderUserAgentInfo userAgent) { this.userAgent = userAgent; }
    public void setAuthTokenControl(AuthTokenControl authTokenControl) { this.authTokenControl = authTokenControl; }
    public void setFormat(HeaderFormatInfo format) { this.format = format; }
    public void setNotify(HeaderNotifyInfo notify) { this.notify = notify; }
    public void setNoNotify(String noNotify) { this.noNotify = noNotify; }
    public void setNoQualify(String noQualify) { this.noQualify = noQualify; }
    public void setVia(String via) { this.via = via; }
    public void setSoapRequestId(String soapRequestId) { this.soapRequestId = soapRequestId; }
    public void setCsrfToken(String csrfToken) { this.csrfToken = csrfToken; }
    public Integer getHopCount() { return hopCount; }
    public String getAuthToken() { return authToken; }
    public HeaderSessionInfo getSession() { return session; }
    public HeaderSessionInfo getLegacySessionId() { return legacySessionId; }
    public String getNoSession() { return noSession; }
    public HeaderAccountInfo getAccount() { return account; }
    public HeaderChangeInfo getChange() { return change; }
    public String getTargetServer() { return targetServer; }
    public HeaderUserAgentInfo getUserAgent() { return userAgent; }
    public AuthTokenControl getAuthTokenControl() { return authTokenControl; }
    public HeaderFormatInfo getFormat() { return format; }
    public HeaderNotifyInfo getNotify() { return notify; }
    public String getNoNotify() { return noNotify; }
    public String getNoQualify() { return noQualify; }
    public String getVia() { return via; }
    public String getSoapRequestId() { return soapRequestId; }
    public String getCsrfToken() { return csrfToken; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("hopCount", hopCount)
            .add("authToken", authToken)
            .add("session", session)
            .add("legacySessionId", legacySessionId)
            .add("noSession", noSession)
            .add("account", account)
            .add("change", change)
            .add("targetServer", targetServer)
            .add("userAgent", userAgent)
            .add("authTokenControl", authTokenControl)
            .add("format", format)
            .add("notify", notify)
            .add("noNotify", noNotify)
            .add("noQualify", noQualify)
            .add("via", via)
            .add("soapRequestId", soapRequestId)
            .add("csrfToken", csrfToken);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
