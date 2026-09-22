/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GENERATE_SAML_TEST_RESPONSE)
public class GenerateSamlTestResponse {

    /**
     * @zm-api-field-tag url
     * @zm-api-field-description Pop-up URL that the admin console opens to begin the test SSO flow,
     * of the form <b>&lt;testOrigin&gt;/service/extension/samllogin?RelayState=&lt;base64&gt;</b>.
     */
    @XmlAttribute(name=AdminConstants.A_URL, required=true)
    private String url;

    /**
     * @zm-api-field-tag relayState
     * @zm-api-field-description The base64 RelayState value embedded in the URL, echoed for convenience.
     */
    @XmlAttribute(name=AdminConstants.A_RELAY_STATE, required=false)
    private String relayState;

    @SuppressWarnings("unused")
    private GenerateSamlTestResponse() {
        this(null, null);
    }

    public GenerateSamlTestResponse(String url, String relayState) {
        this.url = url;
        this.relayState = relayState;
    }

    public void setUrl(String url) { this.url = url; }
    public void setRelayState(String relayState) { this.relayState = relayState; }

    public String getUrl() { return url; }
    public String getRelayState() { return relayState; }
}
