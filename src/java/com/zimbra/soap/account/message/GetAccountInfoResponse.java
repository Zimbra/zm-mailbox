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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.NamedValue;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_ACCOUNT_INFO_RESPONSE)
@XmlType(propOrder = {"name", "attrs", "soapURLs", "publicURL",
                        "changePasswordURL"})
public class GetAccountInfoResponse {

    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Account name - an email address (user@domain)
     */
    @XmlElement(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-description Account attributes Currently only two attrs are returned:
     * <ol>
     * <li> <b>zimbraId</b>       - the unique UUID of the zimbra account
     * <li> <b>zimbraMailHost</b> - the server on which this user's mail resides
     * </ol>
     */
    @XmlElement(name=AccountConstants.E_ATTR, required=true)
    private List<NamedValue> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-tag soap-url
     * @zm-api-field-description URL to talk to for soap service for this account. i.e:
     * <br />
     * http://server:7070/service/soap/
     * <br />
     * <br />
     * Multiple URLs can be returned if both http and https (SSL) are enabled. If only one of the two is enabled,
       then only one URL will be returned.
     */
    @XmlElement(name=AccountConstants.E_SOAP_URL, required=false)
    private List<NamedValue> soapURLs = Lists.newArrayList();

    /**
     * @zm-api-field-tag account-base-public-url
     * @zm-api-field-description Base public URL for the requested account
     */
    @XmlElement(name=AccountConstants.E_PUBLIC_URL, required=false)
    private NamedValue publicURL;

    /**
     * @zm-api-field-tag change-password-url
     * @zm-api-field-description URL to talk to in order to change a password.  Not returned if not configured
     * via domain attribute <b>zimbraChangePasswordURL</b>
     */
    @XmlElement(name=AccountConstants.E_CHANGE_PASSWORD_URL, required=false)
    private NamedValue changePasswordURL;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetAccountInfoResponse() {
        this((String) null);
    }

    public GetAccountInfoResponse(String name) {
        this.name = name;
    }

    public void setAttrs(Iterable <NamedValue> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public GetAccountInfoResponse addAttr(NamedValue attr) {
        this.attrs.add(attr);
        return this;
    }

    public void setSoapURLs(Iterable <NamedValue> soapURLs) {
        this.soapURLs.clear();
        if (soapURLs != null) {
            Iterables.addAll(this.soapURLs,soapURLs);
        }
    }

    public GetAccountInfoResponse addSoapURL(NamedValue soapURL) {
        this.soapURLs.add(soapURL);
        return this;
    }

    public void setPublicURL(NamedValue publicURL) {
        this.publicURL = publicURL;
    }

    public void setChangePasswordURL(NamedValue changePasswordURL) {
        this.changePasswordURL = changePasswordURL;
    }

    public String getName() { return name; }
    public List<NamedValue> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    public List<NamedValue> getSoapURLs() {
        return Collections.unmodifiableList(soapURLs);
    }

    public NamedValue getPublicURL() { return publicURL; }
    public NamedValue getChangePasswordURL() { return changePasswordURL; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("attrs", attrs)
            .add("soapURLs", soapURLs)
            .add("publicURL", publicURL)
            .add("changePasswordURL", changePasswordURL)
            .toString();
    }
}
