/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.LicenseStatus;

@XmlAccessorType(XmlAccessType.NONE)
public class LicenseInfo {

    /**
     * @zm-api-field-description Status
     */
    @XmlAttribute(name=AccountConstants.A_STATUS /* status */, required=true)
    private LicenseStatus status;

    /**
     * @zm-api-field-description License attributes
     */
    @XmlElement(name=AccountConstants.E_ATTR /* attr */, required=false)
    private List<LicenseAttr> attrs;

    public LicenseInfo() {
    }

    public void setStatus(LicenseStatus status) { this.status = status; }
    public void setAttrs(Iterable <LicenseAttr> attrs) {
        if (attrs == null) {
            this.attrs = null;
        } else {
            this.attrs = Lists.newArrayList();
            Iterables.addAll(this.attrs, attrs);
        }
    }

    public void addAttr(LicenseAttr attr) {
        if (this.attrs == null) {
            this.attrs = Lists.newArrayList();
        }
        this.attrs.add(attr);
    }

    public LicenseStatus getStatus() { return status; }
    public List<LicenseAttr> getAttrs() {
        return attrs;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("status", status)
            .add("attrs", attrs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
