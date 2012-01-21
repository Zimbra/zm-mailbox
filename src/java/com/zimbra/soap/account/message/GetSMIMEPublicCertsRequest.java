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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.SMIMEPublicCertsStoreSpec;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-description Get SMIME Public Certificates
 * Stores specified in <b>&lt;store></b> will be attempted in the order they appear in the comma separated list.
 * <br />
 * e.g.
 * <ul>
 * <li>   <b>&lt;store>CONTACT&lt;/store></b>     - lookup certs in user's address books
 * <li>   <b>&lt;store>CONTACT,GAL&lt;/store></b> - lookup in user's address books, then lookup in GAL
 * <li>   <b>&lt;store>LDAP&lt;/store></b>        - lookup certs in external LDAP
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_SMIME_PUBLIC_CERTS_REQUEST)
public class GetSMIMEPublicCertsRequest {

    /**
     * @zm-api-field-description Information on public certificate stores
     */
    @XmlElement(name=AccountConstants.E_STORE /* store */, required=true)
    private final SMIMEPublicCertsStoreSpec store;

    /**
     * @zm-api-field-tag email-address
     * @zm-api-field-description List of email addresses
     */
    @XmlElement(name=AccountConstants.E_EMAIL /* email */, required=false)
    private List<String> emails = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetSMIMEPublicCertsRequest() {
        this((SMIMEPublicCertsStoreSpec) null);
    }

    public GetSMIMEPublicCertsRequest(SMIMEPublicCertsStoreSpec store) {
        this.store = store;
    }

    public void setEmails(Iterable <String> emails) {
        this.emails.clear();
        if (emails != null) {
            Iterables.addAll(this.emails,emails);
        }
    }

    public void addEmail(String email) {
        this.emails.add(email);
    }

    public SMIMEPublicCertsStoreSpec getStore() { return store; }
    public List<String> getEmails() {
        return Collections.unmodifiableList(emails);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("store", store)
            .add("emails", emails);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
