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

package com.zimbra.soap.admin.type;

import com.zimbra.common.soap.AdminConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * An X.509 certificate extracted from a SAML IdP metadata {@code <KeyDescriptor>} element, together with the metadata
 * extracted from the certificate itself (thumbprint and validity window).
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SAMLIdpCertificateInfo {

    /**
     * @zm-api-field-tag cert-use
     * @zm-api-field-description Intended use of the key as declared in the metadata {@code KeyDescriptor/@use}
     *         (typically {@code signing} or {@code encryption}). Empty when the metadata does not specify a use.
     */
    @XmlAttribute(name = AdminConstants.A_SAML_CERT_USE /* use */, required = false)
    private String use;

    /**
     * @zm-api-field-tag cert-thumbprint
     * @zm-api-field-description SHA-256 thumbprint (fingerprint) of the DER-encoded certificate, as an
     *         upper-case hex string.
     */
    @XmlAttribute(name = AdminConstants.A_SAML_THUMBPRINT /* thumbprint */, required = false)
    private String thumbprint;

    /**
     * @zm-api-field-tag cert-not-before
     * @zm-api-field-description Start of the certificate validity window (ISO-8601, UTC).
     */
    @XmlAttribute(name = AdminConstants.A_SAML_NOT_BEFORE /* notBefore */, required = false)
    private String notBefore;

    /**
     * @zm-api-field-tag cert-not-after
     * @zm-api-field-description End of the certificate validity window / expiry (ISO-8601, UTC).
     */
    @XmlAttribute(name = AdminConstants.A_SAML_NOT_AFTER /* notAfter */, required = false)
    private String notAfter;

    /**
     * @zm-api-field-tag cert-subject-dn
     * @zm-api-field-description Certificate subject distinguished name.
     */
    @XmlAttribute(name = AdminConstants.A_SAML_SUBJECT_DN /* subjectDN */, required = false)
    private String subjectDN;

    /**
     * @zm-api-field-tag cert-issuer-dn
     * @zm-api-field-description Certificate issuer distinguished name.
     */
    @XmlAttribute(name = AdminConstants.A_SAML_ISSUER_DN /* issuerDN */, required = false)
    private String issuerDN;

    /**
     * @zm-api-field-tag cert-value
     * @zm-api-field-description Base64-encoded DER X.509 certificate, exactly as it appears in the metadata
     *         {@code X509Certificate} element (without PEM header/footer).
     */
    @XmlValue
    private String value;

    public SAMLIdpCertificateInfo() {
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public String getThumbprint() {
        return thumbprint;
    }

    public void setThumbprint(String thumbprint) {
        this.thumbprint = thumbprint;
    }

    public String getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(String notBefore) {
        this.notBefore = notBefore;
    }

    public String getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(String notAfter) {
        this.notAfter = notAfter;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
