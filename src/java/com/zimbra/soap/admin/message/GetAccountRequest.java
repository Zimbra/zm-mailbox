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
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Joiner;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Account;
import com.zimbra.soap.admin.type.RequestAttr;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ACCOUNT_REQUEST)
@XmlType(propOrder = {AdminConstants.E_ACCOUNT})
public class GetAccountRequest {

    private static Joiner COMMA_JOINER = Joiner.on(",");
    private List<RequestAttr> attrs = new ArrayList<RequestAttr>();
    
    @XmlAttribute(name=AdminConstants.A_APPLY_COS, required=false) private boolean applyCos = true;
    @XmlElement(name=AdminConstants.E_ACCOUNT)
    private Account account;

    public GetAccountRequest() {
    }

    public void setAccount(Account account) {
        this.account = account;
    }
    public Account getAccount() {
        return account;
    }

    public void setApplyCos(boolean applyCos) {
        this.applyCos = applyCos;
    }

    public boolean isApplyCos() {
        return applyCos;
    }
    @XmlAttribute(name=AdminConstants.A_ATTRS) public String getAttrs() {
        return COMMA_JOINER.join(attrs);
    }
    
    public GetAccountRequest setAttrs(String attrs)
    throws ServiceException {
        this.attrs.clear();
        if (attrs != null) {
            addAttrs(attrs.split(","));
        }
        return this;
    }
    
    public GetAccountRequest addAttrs(String sectionName)
    throws ServiceException {
        addAttrs(RequestAttr.fromString(sectionName));
        return this;
    }
    
    public GetAccountRequest addAttrs(RequestAttr attr) {
        attrs.add(attr);
        return this;
    }
    
    public GetAccountRequest addAttrs(String ... attrNames)
    throws ServiceException {
        for (String attrName : attrNames) {
            addAttrs(attrName);
        }
        return this;
    }

    public GetAccountRequest addAttrs(Iterable<RequestAttr> attrs) {
        if (attrs != null) {
            for (RequestAttr attr : attrs) {
                addAttrs(attr);
            }
        }
        return this;
    }

}
