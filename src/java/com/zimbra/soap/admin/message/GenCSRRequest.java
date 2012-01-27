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

/**
 * @zm-api-command-description Request a certificate signing request (CSR)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_GEN_CSR_REQUEST)
@XmlType(propOrder = {"c", "st", "l", "o", "ou", "cn", "subjectAltNames"})
public class GenCSRRequest {

    /**
     * @zm-api-field-tag server-id
     * @zm-api-field-description Server ID
     */
    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private final String server;

    // "1" means new
    /**
     * @zm-api-field-description If value is "1" then force to create a new CSR, the previous one will be overwrited
     */
    @XmlAttribute(name=CertMgrConstants.A_new /* new */, required=true)
    private String newCSR;

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description Type of CSR (required)
     * <table>
     * <tr> <td> <b>self</b> </td> <td> self-signed certificate </td> </tr>
     * <tr> <td> <b>comm</b> </td> <td> commercial certificate </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=false)
    private String type;

    /**
     * @zm-api-field-tag key-size
     * @zm-api-field-description Key size - 1024 or 2048
     */
    @XmlAttribute(name=CertMgrConstants.E_KEYSIZE /* keysize */, required=true)
    private String keySize;

    /**
     * @zm-api-field-tag subject-attr-C
     * @zm-api-field-description Subject attr C
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_C /* C */, required=false)
    private String c;

    /**
     * @zm-api-field-tag subject-attr-ST
     * @zm-api-field-description Subject attr ST
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_ST /* ST */, required=false)
    private String st;

    /**
     * @zm-api-field-tag subject-attr-L
     * @zm-api-field-description Subject attr L
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_L /* L */, required=false)
    private String l;

    /**
     * @zm-api-field-tag subject-attr-L
     * @zm-api-field-description Subject attr L
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_O /* O */, required=false)
    private String o;

    /**
     * @zm-api-field-tag subject-attr-OU
     * @zm-api-field-description Subject attr OU
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_OU /* OU */, required=false)
    private String ou;

    /**
     * @zm-api-field-tag subject-attr-CN
     * @zm-api-field-description Subject attr CN
     */
    @XmlElement(name=CertMgrConstants.E_subjectAttr_CN /* CN */, required=false)
    private String cn;

    /**
     * @zm-api-field-tag subject-alt-name
     * @zm-api-field-description Used to add the Subject Alt Name extension in the certificate, so multiple hosts can
     * be supported
     */
    @XmlElement(name=CertMgrConstants.E_SUBJECT_ALT_NAME /* SubjectAltName */, required=false)
    private List<String> subjectAltNames = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GenCSRRequest() {
        this((String) null);
    }

    public GenCSRRequest(String server) {
        this.server = server;
    }

    public void setNewCSR(String newCSR) { this.newCSR = newCSR; }
    public void setType(String type) { this.type = type; }
    public void setKeySize(String keySize) { this.keySize = keySize; }
    public void setC(String c) { this.c = c; }
    public void setSt(String st) { this.st = st; }
    public void setL(String l) { this.l = l; }
    public void setO(String o) { this.o = o; }
    public void setOu(String ou) { this.ou = ou; }
    public void setCn(String cn) { this.cn = cn; }
    public void setSubjectAltNames(Iterable <String> subjectAltNames) {
        this.subjectAltNames.clear();
        if (subjectAltNames != null) {
            Iterables.addAll(this.subjectAltNames,subjectAltNames);
        }
    }

    public void addSubjectAltName(String subjectAltName) {
        this.subjectAltNames.add(subjectAltName);
    }

    public String getServer() { return server; }
    public String getNewCSR() { return newCSR; }
    public String getType() { return type; }
    public String getKeySize() { return keySize; }
    public String getC() { return c; }
    public String getSt() { return st; }
    public String getL() { return l; }
    public String getO() { return o; }
    public String getOu() { return ou; }
    public String getCn() { return cn; }
    public List<String> getSubjectAltNames() {
        return Collections.unmodifiableList(subjectAltNames);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("newCSR", newCSR)
            .add("type", type)
            .add("keySize", keySize)
            .add("c", c)
            .add("st", st)
            .add("l", l)
            .add("o", o)
            .add("ou", ou)
            .add("cn", cn)
            .add("subjectAltNames", subjectAltNames);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
