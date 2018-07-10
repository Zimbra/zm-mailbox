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
import com.zimbra.soap.base.CertSubjectAttrs;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Request a certificate signing request (CSR)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=CertMgrConstants.E_GEN_CSR_REQUEST)
@XmlType(propOrder = {"c", "st", "l", "o", "ou", "cn", "subjectAltNames"})
public class GenCSRRequest implements CertSubjectAttrs {

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
     * @zm-api-field-tag digest
     * @zm-api-field-description digest. Default value: "SHA256"
     */
    @XmlAttribute(name=CertMgrConstants.E_DIGEST /* digest */, required=false)
    private String digest;

    /**
     * @zm-api-field-tag key-size
     * @zm-api-field-description Key size. Default value: 2048. Minimum allowed value: 2048.
     */
    @XmlAttribute(name = CertMgrConstants.E_KEYSIZE /* keysize */, required = false)
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
    private final List<String> subjectAltNames = Lists.newArrayList();

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
    public void setDigest(String digest) { this.digest = digest; }
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
    public String getDigest() { return digest; }
    public String getKeySize() { return keySize; }
    @Override
    public String getC() { return c; }
    @Override
    public String getSt() { return st; }
    @Override
    public String getL() { return l; }
    @Override
    public String getO() { return o; }
    @Override
    public String getOu() { return ou; }
    @Override
    public String getCn() { return cn; }
    public List<String> getSubjectAltNames() {
        return Collections.unmodifiableList(subjectAltNames);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("newCSR", newCSR)
            .add("type", type)
            .add("digest", digest)
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
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
