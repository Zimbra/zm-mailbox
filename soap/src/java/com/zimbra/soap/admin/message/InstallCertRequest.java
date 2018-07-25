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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.CertMgrConstants;
import com.zimbra.soap.admin.type.CSRSubject;
import com.zimbra.soap.admin.type.CommCert;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Ask server to install the certificates
 * @zm-api-command-network-edition
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_INSTALL_CERT_REQUEST)
@XmlType(propOrder = {"commCert", "validationDays", "digest", "subject", "subjectAltNames", "keySize"})
public class InstallCertRequest {

    /**
     * @zm-api-field-tag server-id
     * @zm-api-field-description Server ID
     */
    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private final String server;

    /**
     * @zm-api-field-description Certificate type
     * <br />
     * Could be "self" (self-signed cert) or "comm" (commerical cert)
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private final String type;

    /**
     * @zm-api-field-description Commercial certificate
     */
    @XmlElement(name=CertMgrConstants.E_comm_cert /* comm_cert */, required=false)
    private CommCert commCert;

    /**
     * @zm-api-field-tag validation-days
     * @zm-api-field-description Validation days: required. Number of the validation days of the self signed certificate
     */
    @XmlElement(name=CertMgrConstants.E_VALIDATION_DAYS /* validation_days */, required=false)
    private String validationDays;

    /**
     * @zm-api-field-tag digest
     * @zm-api-field-description digest. Default value: "SHA256"
     */
    @XmlElement(name=CertMgrConstants.E_DIGEST /* digest */, required=false)
    private String digest;

    /**
     * @zm-api-field-description Subject
     */
    @XmlElement(name=CertMgrConstants.E_SUBJECT /* subject */, required=false)
    private CSRSubject subject;

    /**
     * @zm-api-field-description subjectAltNames
     */
    @XmlElement(name=CertMgrConstants.E_SUBJECT_ALT_NAME /* SubjectAltName */, required=false)
    private final List<String> subjectAltNames = Lists.newArrayList();

    /**
     * @zm-api-field-tag
     * @zm-api-field-description Key length of the self-signed certificate. Default value: 2048. Minimum allowed value: 2048
     */
    @XmlElement(name=CertMgrConstants.E_KEYSIZE /* keysize */, required=false)
    private String keySize;

    /**
     * @zm-api-field-tag skipCleanup
     * @zm-api-field-description Flag whether or not to delete temporary files. This should be used only for testing and troubleshooting.
     */
    @XmlAttribute(name = CertMgrConstants.E_SKIP_CLEANUP /* skipCleanup */, required = false)
    private ZmBoolean skipCleanup;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private InstallCertRequest() {
        this((String) null, (String) null);
    }

    public InstallCertRequest(String server, String type) {
        this.server = server;
        this.type = type;
        this.skipCleanup = ZmBoolean.FALSE;
    }

    public void setCommCert(CommCert commCert) { this.commCert = commCert; }
    public void setValidationDays(String validationDays) {
        this.validationDays = validationDays;
    }
    public void setDigest(String digest) { this.digest = digest; }
    public void setSubject(CSRSubject subject) { this.subject = subject; }
    public void setSubjectAltNames(Iterable <String> subjectAltNames) {
        this.subjectAltNames.clear();
        if (subjectAltNames != null) {
            Iterables.addAll(this.subjectAltNames,subjectAltNames);
        }
    }

    public void addSubjectAltName(String subjectAltName) {
        this.subjectAltNames.add(subjectAltName);
    }

    public void setKeySize(String keySize) { this.keySize = keySize; }
    public String getServer() { return server; }
    public String getType() { return type; }
    public CommCert getCommCert() { return commCert; }
    public String getValidationDays() { return validationDays; }
    public String getDigest() { return digest; }
    public CSRSubject getSubject() { return subject; }
    public List<String> getSubjectAltNames() {
        return Collections.unmodifiableList(subjectAltNames);
    }
    public String getKeySize() { return keySize; }

    public void setSkipCleanup(Boolean val) {
        this.skipCleanup = ZmBoolean.fromBool(val);
    }

    public Boolean getSkipCleanup() {
        return ZmBoolean.toBool(skipCleanup);
    }
    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type)
            .add("commCert", commCert)
            .add("validationDays", validationDays)
            .add("digest", digest)
            .add("subject", subject)
            .add("subjectAltNames", subjectAltNames)
            .add("keySize", keySize);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
