/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.CosInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ACCOUNT_INFO_RESPONSE)
@XmlType(propOrder = {"name","attrList", "cos", "soapURLList", "adminSoapURL", "publicMailURL"})
public class GetAccountInfoResponse {

    /**
     * @zm-api-field-tag account-name
     * @zm-api-field-description Account name
     */
    @XmlElement(name=AdminConstants.E_NAME, required=true)
    private String name;

    /**
     * @zm-api-field-description Attributes
     */
    @XmlElement(name=AdminConstants.E_A)
    private List<Attr> attrList = new ArrayList<Attr>();

    /**
     * @zm-api-field-description Class of Service (COS) information for account
     */
    @XmlElement(name=AdminConstants.E_COS)
    private CosInfo cos;

    /**
     * @zm-api-field-tag acct-soap-url
     * @zm-api-field-description URL to talk to for SOAP service for this account. e.g.:
     * <pre>
     *     http://server:7070/service/soap/
     * </pre>
     * Multiple URLs can be returned if both http and https (SSL) are enabled. If only one of the two is enabled,
     * then only one URL will be returned.
     */
    @XmlElement(name=AdminConstants.E_SOAP_URL /* soapURL */, required=false)
    private List<String> soapURLList = new ArrayList<String>();

    /**
     * @zm-api-field-tag admin-soap-url
     * @zm-api-field-description URL for the Admin SOAP service
     * <br />
     * Note: Admin app only runs over SSL.
     */
    @XmlElement(name=AdminConstants.E_ADMIN_SOAP_URL /* adminSoapURL */, required=false)
    private String adminSoapURL;

    /**
     * @zm-api-field-tag web-mail-url
     * @zm-api-field-description URL for Web Mail application
     */
    @XmlElement(name=AdminConstants.E_PUBLIC_MAIL_URL /* publicMailURL */, required=false)
    private String publicMailURL;

    public GetAccountInfoResponse() {
    }

    public GetAccountInfoResponse setAttrList(Collection<Attr> attrs) {
        this.attrList.clear();
        if (attrs != null) {
            this.attrList.addAll(attrs);
        }
        return this;
    }

    public GetAccountInfoResponse addAttr(Attr attr) {
        attrList.add(attr);
        return this;
    }

    public List<Attr> getAttrList() {
        return Collections.unmodifiableList(attrList);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSoapURLList(List<String> soapURLList) {
        this.soapURLList = soapURLList;
    }
    public GetAccountInfoResponse addSoapURL(String soapUrl) {
        soapURLList.add(soapUrl);
        return this;
    }

    public List<String> getSoapURLList() {
        return Collections.unmodifiableList(soapURLList);
    }

    public void setAdminSoapURL(String adminSoapURL) {
        this.adminSoapURL = adminSoapURL;
    }

    public String getAdminSoapURL() {
        return adminSoapURL;
    }

    public void setPublicMailURL(String publicMailURL) {
        this.publicMailURL = publicMailURL;
    }

    public String getPublicMailURL() {
        return publicMailURL;
    }

    public void setCos(CosInfo cos) {
        this.cos = cos;
    }

    public CosInfo getCos() {
        return cos;
    }
}
