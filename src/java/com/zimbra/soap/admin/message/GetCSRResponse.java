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

import org.w3c.dom.Element;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.CertMgrConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_GET_CSR_RESPONSE)
@XmlType(propOrder = { "subjectDSNelements", "subjectAltNames" })
public class GetCSRResponse {

    @XmlAttribute(name=CertMgrConstants.A_csr_exists /* csr_exists */, required=true)
    private String csrExists;

    @XmlAttribute(name=CertMgrConstants.A_isComm /* isComm */, required=true)
    private String isComm;

    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private String server;

    @XmlElement(name=CertMgrConstants.E_SUBJECT_ALT_NAME /* SubjectAltName */, required=false)
    private List<String> subjectAltNames = Lists.newArrayList();

    // Expect text content only
    @XmlAnyElement
    private List<org.w3c.dom.Element> subjectDSNelements = Lists.newArrayList();

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

    public void setSubjectDSNelements(Iterable <org.w3c.dom.Element> subjectDSNelements) {
        this.subjectDSNelements.clear();
        if (subjectDSNelements != null) {
            Iterables.addAll(this.subjectDSNelements,subjectDSNelements);
        }
    }

    public void addSubjectDSNelement(org.w3c.dom.Element subjectDSNelement) {
        this.subjectDSNelements.add(subjectDSNelement);
    }

    public String getCsrExists() { return csrExists; }
    public String getIsComm() { return isComm; }
    public String getServer() { return server; }
    public List<String> getSubjectAltNames() {
        return Collections.unmodifiableList(subjectAltNames);
    }
    public List<org.w3c.dom.Element> getSubjectDSNelements() {
        return Collections.unmodifiableList(subjectDSNelements);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("csrExists", csrExists)
            .add("isComm", isComm)
            .add("server", server)
            .add("subjectAltNames", subjectAltNames)
            .add("subjectDSNelements", subjectDSNelements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
