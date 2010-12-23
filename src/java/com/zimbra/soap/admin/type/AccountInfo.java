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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_ACCOUNT)
@XmlType(propOrder = {AdminConstants.E_A})
public class AccountInfo {

    // Relates to "com.zimbra.cs.account.Account"
    @XmlAttribute(name=AccountConstants.A_NAME, required=true) private String name;
    @XmlAttribute(name=AccountConstants.A_ID, required=true) private String id;
    @XmlAttribute(name=AccountConstants.A_isExternal, required=false) private boolean isExternal;

    @XmlElement(name=AdminConstants.E_A)
    private List<Attr> a = new ArrayList<Attr>();

    public AccountInfo() {
    }
    
    public AccountInfo setA(Collection<Attr> attrs) {
        this.a.clear();
        if (attrs != null) {
            this.a.addAll(attrs);
        }
        return this;
    }

    public AccountInfo addAttr(Attr attr) {
        a.add(attr);
        return this;
    }

    public List<Attr> getA() {
        return Collections.unmodifiableList(a);
    }
}
