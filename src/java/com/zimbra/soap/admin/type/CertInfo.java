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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class CertInfo {

    @XmlAttribute(name=AdminConstants.A_SERVER /* server */, required=true)
    private final String server;

    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private final String type;

    // Expect elements with text content only
    @XmlAnyElement
    private List<org.w3c.dom.Element> certs = Lists.newArrayList();

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

    public void setCerts(Iterable <org.w3c.dom.Element> certs) {
        this.certs.clear();
        if (certs != null) {
            Iterables.addAll(this.certs,certs);
        }
    }

    public void addCert(org.w3c.dom.Element cert) {
        this.certs.add(cert);
    }

    public String getServer() { return server; }
    public String getType() { return type; }
    public List<org.w3c.dom.Element> getCerts() {
        return Collections.unmodifiableList(certs);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("server", server)
            .add("type", type)
            .add("certs", certs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
