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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_A)
@XmlType(propOrder = {})
public class Attr
{
    @XmlAttribute(name=AdminConstants.A_N, required=true)
    private String n;
    @XmlAttribute(name=AdminConstants.A_C, required=false)
    private Boolean isCosAttr;
    @XmlValue private String value;

    public Attr() {
    }

    public Attr(String n, String value) {
        this.n = n;
        this.setValue(value);
    }

    public Attr(String value) {
        this.setValue(value);
    }

    public void setN(String n) {
        this.n = n;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setIsCosAttr(Boolean isCosAttr) {
        this.isCosAttr = isCosAttr;
    }

    public String getN() { return n; }
    public String getValue() { return value; }
    public Boolean getIsCosAttr() { return isCosAttr; }
}
