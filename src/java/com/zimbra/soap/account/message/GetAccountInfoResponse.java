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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AccountConstants.E_GET_ACCOUNT_INFO_RESPONSE)
@XmlType(propOrder = {"name", "attrs", "soapURLs", "publicURL",
                        "changePasswordURL"})
public class GetAccountInfoResponse {

    @XmlElement(name=AccountConstants.A_NAME, required=true)
    private final String name;

    @XmlElement(name=AccountConstants.E_ATTR, required=true)
    private List<NamedValue> attrs = Lists.newArrayList();

    @XmlElement(name=AccountConstants.E_SOAP_URL, required=false)
    private List<NamedValue> soapURLs = Lists.newArrayList();

    @XmlElement(name=AccountConstants.E_PUBLIC_URL, required=false)
    private NamedValue publicURL;

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
