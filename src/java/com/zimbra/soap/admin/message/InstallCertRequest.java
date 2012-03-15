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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.CertMgrConstants;
import com.zimbra.soap.admin.type.CommCert;
import com.zimbra.soap.admin.type.CSRSubject;

/**
 * @zm-api-command-description Ask server to install the certificates
 * @zm-api-command-network-edition
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_INSTALL_CERT_REQUEST)
@XmlType(propOrder = {"commCert", "validationDays",
    "subject", "subjectAltNames", "keySize"})
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
     * @zm-api-field-description Validation days: required.  Number of the validation days of the self signed
     * certificate,
     */
    @XmlElement(name=CertMgrConstants.E_VALIDATION_DAYS /* validation_days */, required=false)
    private String validationDays;

    /**
     * @zm-api-field-description Subject
     */
    @XmlElement(name=CertMgrConstants.E_SUBJECT /* subject */, required=false)
    private CSRSubject subject;

    /**
     * @zm-api-field-description subjectAltNames
     */
    @XmlElement(name=CertMgrConstants.E_SUBJECT_ALT_NAME /* SubjectAltName */, required=false)
    private List<String> subjectAltNames = Lists.newArrayList();

    /**
     * @zm-api-field-tag
     * @zm-api-field-description Key Size: 1024|2048, key length of the self-signed certificate
     */
    @XmlElement(name=CertMgrConstants.E_KEYSIZE /* keysize */, required=false)
    private String keySize;

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
    }

    public void setCommCert(CommCert commCert) { this.commCert = commCert; }
    public void setValidationDays(String validationDays) {
        this.validationDays = validationDays;
    }
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
    public CSRSubject getSubject() { return subject; }
    public List<String> getSubjectAltNames() {
        return Collections.unmodifiableList(subjectAltNames);
    }
    public String getKeySize() { return keySize; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type)
            .add("commCert", commCert)
            .add("validationDays", validationDays)
            .add("subject", subject)
            .add("subjectAltNames", subjectAltNames)
            .add("keySize", keySize);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
