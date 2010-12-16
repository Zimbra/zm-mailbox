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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

public class DomainSelector {
    // From enum com.zimbra.cs.account.Provisioning.DomainBy;
    @XmlEnum
    public enum DomainBy { id, name, virtualHostname, krb5Realm, foreignName }

    @XmlValue private String key;
    @XmlAttribute(name=AdminConstants.A_BY) private DomainBy domainBy;

    public DomainSelector() {
    }

    public DomainSelector(DomainBy by, String key) {
        setBy(by);
        setKey(key);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setBy(DomainBy by) {
        this.domainBy = by;
    }

    public DomainBy getBy() {
        return domainBy;
    }
}
