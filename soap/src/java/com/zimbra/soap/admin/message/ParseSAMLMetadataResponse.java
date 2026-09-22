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

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.SAMLIdpCertificateInfo;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Fields extracted from a parsed SAML IdP metadata document.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_PARSE_SAML_METADATA_RESPONSE)
@XmlType(propOrder = { "entityID", "ssoURL", "sloURL", "nameIdFormat", "certificates" })
public class ParseSAMLMetadataResponse {

    /**
     * @zm-api-field-description IdP Entity ID ({@code EntityDescriptor/@entityID}).
     */
    @XmlElement(name = AdminConstants.E_SAML_ENTITY_ID, required = false)
    private String entityID;

    /**
     * @zm-api-field-description All IdP Single Sign-On (SSO) service endpoints advertised in the metadata, one
     *         element per binding, each formatted as {@code Binding=Location}.
     */
    @XmlElement(name = AdminConstants.E_SAML_SSO_URL, required = false)
    private List<String> ssoURL = Lists.newArrayList();

    /**
     * @zm-api-field-description All IdP Single Logout (SLO) service endpoints advertised in the metadata, one
     *         element per binding, each formatted as {@code Binding=Location}.
     */
    @XmlElement(name = AdminConstants.E_SAML_SLO_URL, required = false)
    private List<String> sloURL = Lists.newArrayList();

    /**
     * @zm-api-field-description All NameID formats advertised by the IdP, one element each.
     */
    @XmlElement(name = AdminConstants.E_SAML_NAME_ID_FORMAT, required = false)
    private List<String> nameIdFormat = Lists.newArrayList();

    /**
     * @zm-api-field-description X.509 certificates declared in the IdP metadata (signing / encryption).
     */
    @XmlElement(name = AdminConstants.E_SAML_CERTIFICATE, required = false)
    private List<SAMLIdpCertificateInfo> certificates = Lists.newArrayList();

    public ParseSAMLMetadataResponse() {
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }

    public List<String> getSsoURL() {
        return ssoURL;
    }

    public void setSsoURL(List<String> ssoURL) {
        this.ssoURL = (ssoURL == null) ? new ArrayList<String>() : ssoURL;
    }

    public void addSsoURL(String value) {
        if (value != null) {
            ssoURL.add(value);
        }
    }

    public List<String> getSloURL() {
        return sloURL;
    }

    public void setSloURL(List<String> sloURL) {
        this.sloURL = (sloURL == null) ? new ArrayList<String>() : sloURL;
    }

    public void addSloURL(String value) {
        if (value != null) {
            sloURL.add(value);
        }
    }

    public List<String> getNameIdFormat() {
        return nameIdFormat;
    }

    public void setNameIdFormat(List<String> nameIdFormat) {
        this.nameIdFormat = (nameIdFormat == null) ? new ArrayList<String>() : nameIdFormat;
    }

    public void addNameIdFormat(String value) {
        if (value != null) {
            nameIdFormat.add(value);
        }
    }

    public List<SAMLIdpCertificateInfo> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<SAMLIdpCertificateInfo> certificates) {
        this.certificates = (certificates == null) ? new ArrayList<SAMLIdpCertificateInfo>() : certificates;
    }

    public void addCertificate(SAMLIdpCertificateInfo cert) {
        if (cert != null) {
            certificates.add(cert);
        }
    }
}
