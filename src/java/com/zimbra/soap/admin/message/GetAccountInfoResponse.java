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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_ACCOUNT_INFO_RESPONSE)
@XmlType(propOrder = {"name","attrList", "cos", "soapURLList", "adminSoapURL", "publicMailURL"})
public class GetAccountInfoResponse {

    @XmlElement(name=AdminConstants.E_NAME, required=true)
    private String name;
    @XmlElement(name=AdminConstants.E_A)
    private List<Attr> attrList = new ArrayList<Attr>();
    @XmlElement(name=AdminConstants.E_COS)
    private CosInfo cos;
    @XmlElement(name=AdminConstants.E_SOAP_URL)
    private List<String> soapURLList = new ArrayList<String>();
    @XmlElement(name=AdminConstants.E_ADMIN_SOAP_URL, required=false)
    private String adminSoapURL;
    @XmlElement(name=AdminConstants.E_PUBLIC_MAIL_URL, required=false)
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
