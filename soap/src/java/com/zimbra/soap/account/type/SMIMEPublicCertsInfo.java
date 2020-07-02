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

package com.zimbra.soap.account.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
@GraphQLType(name=GqlConstants.CLASS_SMIME_PUBLIC_CERTS_INFO, description="SMIMEPublicCertsInfo")
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

    @GraphQLQuery(name=GqlConstants.EMAIL, description="email")
    public String getEmail() { return email; }
    @GraphQLQuery(name=GqlConstants.CERTIFICATES, description="SMIME certificates")
    public List<SMIMEPublicCertInfo> getCerts() {
        return Collections.unmodifiableList(certs);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("email", email)
            .add("certs", certs);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
