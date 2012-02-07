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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.CertMgrConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_GET_CSR_RESPONSE)
public class GetCSRResponse {

    /**
     * @zm-api-field-tag csr-exists-flag
     * @zm-api-field-description Flag whether CSR exists or not - <b>"1"</b> means true, <b>"0"</b> means false.
     */
    @XmlAttribute(name=CertMgrConstants.A_csr_exists /* csr_exists */, required=true)
    private String csrExists;

    /**
     * @zm-api-field-tag is-commercial
     * @zm-api-field-description Flag whether CSR is commercial or not - <b>"1"</b> means true, <b>"0"</b> means false.
     * <br />
     * Currently not working/used
     */
    @XmlAttribute(name=CertMgrConstants.A_isComm /* isComm */, required=true)
    private String isComm;

    /**
     * @zm-api-field-tag server-name
     * @zm-api-field-description Server name
     */
    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private String server;

    /**
     * @zm-api-field-description Subject DSN "C" value
     */
    @XmlElement(name="C", required=false)
    private String CsubjectDSN;

    /**
     * @zm-api-field-description Subject DSN "ST" value
     */
    @XmlElement(name="ST", required=false)
    private String STsubjectDSN;

    /**
     * @zm-api-field-description Subject DSN "L" value
     */
    @XmlElement(name="L", required=false)
    private String LsubjectDSN;

    /**
     * @zm-api-field-description Subject DSN "O" value
     */
    @XmlElement(name="O", required=false)
    private String OsubjectDSN;

    /**
     * @zm-api-field-description Subject DSN "OU" value
     */
    @XmlElement(name="OU", required=false)
    private String OUsubjectDSN;

    /**
     * @zm-api-field-description Subject DSN "CN" value
     */
    @XmlElement(name="CN", required=false)
    private String CNsubjectDSN;

    /**
     * @zm-api-field-description SubjectAltNames
     */
    @XmlElement(name=CertMgrConstants.E_SUBJECT_ALT_NAME /* SubjectAltName */, required=false)
    private List<String> subjectAltNames = Lists.newArrayList();

    public GetCSRResponse() {
    }

    public void setCsrExists(String csrExists) { this.csrExists = csrExists; }
    public void setIsComm(String isComm) { this.isComm = isComm; }
    public void setServer(String server) { this.server = server; }
    public void setSubjectAltNames(Iterable <String> subjectAltNames) {
        this.subjectAltNames.clear();
        if (subjectAltNames != null) {
            Iterables.addAll(this.subjectAltNames,subjectAltNames);
        }
    }

    public void addSubjectAltName(String subjectAltName) {
        this.subjectAltNames.add(subjectAltName);
    }

    public String getCsrExists() { return csrExists; }
    public String getIsComm() { return isComm; }
    public String getServer() { return server; }

    public String getCsubjectDSN() { return CsubjectDSN; }
    public void setCsubjectDSN(String csubjectDSN) { CsubjectDSN = csubjectDSN; }

    public String getSTsubjectDSN() { return STsubjectDSN; }
    public void setSTsubjectDSN(String sTsubjectDSN) { STsubjectDSN = sTsubjectDSN; }

    public String getLsubjectDSN() { return LsubjectDSN; }
    public void setLsubjectDSN(String lsubjectDSN) { LsubjectDSN = lsubjectDSN; }

    public String getOsubjectDSN() { return OsubjectDSN; }
    public void setOsubjectDSN(String osubjectDSN) { OsubjectDSN = osubjectDSN; }

    public String getOUsubjectDSN() { return OUsubjectDSN; }
    public void setOUsubjectDSN(String oUsubjectDSN) { OUsubjectDSN = oUsubjectDSN; }

    public String getCNsubjectDSN() { return CNsubjectDSN; }
    public void setCNsubjectDSN(String cNsubjectDSN) { CNsubjectDSN = cNsubjectDSN; }

    public List<String> getSubjectAltNames() {
        return Collections.unmodifiableList(subjectAltNames);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("csrExists", csrExists)
            .add("isComm", isComm)
            .add("server", server)
            .add("CsubjectDSN", CsubjectDSN)
            .add("STsubjectDSN", STsubjectDSN)
            .add("LsubjectDSN", LsubjectDSN)
            .add("OsubjectDSN", OsubjectDSN)
            .add("OUsubjectDSN", OUsubjectDSN)
            .add("CNsubjectDSN", CNsubjectDSN)
            .add("subjectAltNames", subjectAltNames);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
