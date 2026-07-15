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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Fetch and parse a SAML Identity Provider (IdP) metadata document and return the
 *         extracted fields (IdP Entity ID, SSO URL, SLO URL, NameID format and signing/encryption X.509 certificates
 *         with their thumbprint and validity). <br /> The metadata source is supplied either as a remote URL (fetched
 *         server-side) via the {@code url} attribute, or as the raw metadata XML in the {@code &lt;content&gt;} element
 *         (e.g. an uploaded metadata file). Exactly one of the two must be provided. An optional {@code &lt;domain&gt;}
 *         scopes the authorization check to a specific domain.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_PARSE_SAML_METADATA_REQUEST)
public class ParseSAMLMetadataRequest {

    /**
     * @zm-api-field-description Domain the parsed metadata is intended for. When present, the caller must have
     *         rights to modify this domain. When absent, the request is available to any authorized administrator.
     */
    @XmlElement(name = AdminConstants.E_DOMAIN, required = false)
    private DomainSelector domain;

    /**
     * @zm-api-field-tag idp-metadata-url
     * @zm-api-field-description URL of the IdP metadata document to fetch and parse server-side. Only
     *         {@code http} and {@code https} URLs are supported. Mutually exclusive with {@code &lt;content&gt;}.
     */
    @XmlAttribute(name = AdminConstants.A_URL /* url */, required = false)
    private String url;

    /**
     * @zm-api-field-tag idp-metadata-xml
     * @zm-api-field-description Raw IdP metadata XML content to parse (e.g. from an uploaded file). Mutually
     *         exclusive with the {@code url} attribute.
     */
    @XmlElement(name = AdminConstants.E_CONTENT /* content */, required = false)
    private String content;

    public ParseSAMLMetadataRequest() {
    }

    public DomainSelector getDomain() {
        return domain;
    }

    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
