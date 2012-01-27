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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class SMIMEPublicCertsInfo {

    /**
     * @zm-api-field-tag certs-email-address
     * @zm-api-field-description Email address
     */
    @XmlAttribute(name=AccountConstants.A_EMAIL /* email */, required=false)
    private String email;

    /**
     * @zm-api-field-description Certificates
     */
    @XmlElement(name=AccountConstants.E_CERT /* cert */, required=false)
    private List<SMIMEPublicCertInfo> certs = Lists.newArrayList();

    public SMIMEPublicCertsInfo() {
    }

    public void setEmail(String email) { this.email = email; }
    public void setCerts(Iterable <SMIMEPublicCertInfo> certs) {
        this.certs.clear();
        if (certs != null) {
            Iterables.addAll(this.certs,certs);
        }
    }

    public void addCert(SMIMEPublicCertInfo cert) {
        this.certs.add(cert);
    }

    public String getEmail() { return email; }
    public List<SMIMEPublicCertInfo> getCerts() {
        return Collections.unmodifiableList(certs);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("email", email)
            .add("certs", certs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
