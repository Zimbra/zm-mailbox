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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.CertMgrConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CommCert {

    /**
     * @zm-api-field-description Certificate information
     */
    @XmlElement(name=CertMgrConstants.E_cert /* cert */, required=false)
    private AidAndFilename cert;

    /**
     * @zm-api-field-description rootCA information
     */
    @XmlElement(name=CertMgrConstants.E_rootCA /* rootCA */, required=false)
    private AidAndFilename rootCA;

    /**
     * @zm-api-field-description intermediateCA information
     */
    @XmlElement(name=CertMgrConstants.E_intermediateCA /* intermediateCA */, required=false)
    private List<AidAndFilename> intermediateCAs = Lists.newArrayList();

    public CommCert() {
    }

    public void setCert(AidAndFilename cert) { this.cert = cert; }
    public void setRootCA(AidAndFilename rootCA) { this.rootCA = rootCA; }
    public void setIntermediateCAs(Iterable <AidAndFilename> intermediateCAs) {
        this.intermediateCAs.clear();
        if (intermediateCAs != null) {
            Iterables.addAll(this.intermediateCAs,intermediateCAs);
        }
    }

    public void addIntermediateCA(AidAndFilename intermediateCA) {
        this.intermediateCAs.add(intermediateCA);
    }

    public AidAndFilename getCert() { return cert; }
    public AidAndFilename getRootCA() { return rootCA; }
    public List<AidAndFilename> getIntermediateCAs() {
        return Collections.unmodifiableList(intermediateCAs);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("cert", cert)
            .add("rootCA", rootCA)
            .add("intermediateCAs", intermediateCAs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
