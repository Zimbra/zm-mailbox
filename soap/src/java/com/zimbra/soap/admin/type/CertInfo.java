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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CertInfo {

    /**
     * @zm-api-field-tag server-name
     * @zm-api-field-description Server name
     */
    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private final String server;

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description type - 1 of <b>mta|ldap|mailboxd|proxy|staged</b>
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private final String type;

    /**
     * @zm-api-field-tag subject
     * @zm-api-field-description C, ST, L, O, OU, CN of current cert
     */
    @XmlElement(name="subject", required=false)
    private String subject;

    /**
     * @zm-api-field-tag issuer
     * @zm-api-field-description C, ST, L, O, OU, CN of issuer cert
     */
    @XmlElement(name="issuer", required=false)
    private String issuer;

    /**
     * @zm-api-field-tag notBefore
     * @zm-api-field-description Certificate validation start time
     */
    @XmlElement(name="notBefore", required=false)
    private String notBefore;

    /**
     * @zm-api-field-tag notAfter
     * @zm-api-field-description Certificate validation end time
     */
    @XmlElement(name="notAfter", required=false)
    private String notAfter;

    /**
     * @zm-api-field-tag SubjectAltName
     * @zm-api-field-description Current cert's subject alternative name (as x509v3 Extension)
     */
    @XmlElement(name="SubjectAltName", required=false)
    private String SubjectAltName;

    // Expect elements with text content only
    /**
     * @zm-api-field-description Any other information found in the certificate
     */
    @XmlAnyElement
    private List<org.w3c.dom.Element> certInfos = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CertInfo() {
        this((String) null, (String) null);
    }

    public CertInfo(String server, String type) {
        this.server = server;
        this.type = type;
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public void setNotBefore(String notBefore) { this.notBefore = notBefore; }
    public void setNotAfter(String notAfter) { this.notAfter = notAfter; }
    public void setSubjectAltName(String SubjectAltName) { this.SubjectAltName = SubjectAltName; }
    public void setCertInfos(Iterable <org.w3c.dom.Element> certInfos) {
        this.certInfos.clear();
        if (certInfos != null) {
            Iterables.addAll(this.certInfos,certInfos);
        }
    }

    public void addCertInfo(org.w3c.dom.Element certInfo) {
        this.certInfos.add(certInfo);
    }

    public String getServer() { return server; }
    public String getType() { return type; }
    public String getSubject() { return subject; }
    public String getIssuer() { return issuer; }
    public String getNotBefore() { return notBefore; }
    public String getNotAfter() { return notAfter; }
    public String getSubjectAltName() { return SubjectAltName; }
    public List<org.w3c.dom.Element> getCertInfos() {
        return Collections.unmodifiableList(certInfos);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type)
            .add("subject", subject)
            .add("issuer", issuer)
            .add("notBefore", notBefore)
            .add("notAfter", notAfter)
            .add("SubjectAltName", SubjectAltName)
            .add("certInfos", certInfos);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
