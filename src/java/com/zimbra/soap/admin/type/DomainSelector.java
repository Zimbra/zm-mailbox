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
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class DomainSelector {
    // TODO: Change com.zimbra.cs.account.Provisioning.DomainBy to use this
    @XmlEnum
    public enum DomainBy { id, name, virtualHostname, krb5Realm, foreignName }

    @XmlValue private final String key;
    @XmlAttribute(name=AdminConstants.A_BY) private final DomainBy domainBy;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DomainSelector() {
        this.domainBy = null;
        this.key = null;
    }

    public DomainSelector(DomainBy by, String key) {
        this.domainBy = by;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public DomainBy getBy() {
        return domainBy;
    }
}
