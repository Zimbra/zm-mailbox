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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.LDAPUtilsConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

// See ZimbraLDAPUtilsExtension/doc/soapadmin.txt
/**
 * @zm-api-command-description Create an LDAP entry
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=LDAPUtilsConstants.E_CREATE_LDAP_ENTRIY_REQUEST)
public class CreateLDAPEntryRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag LDAP-DN-string
     * @zm-api-field-description A valid LDAP DN String (RFC 2253) that describes the new DN to create
     */
    @XmlAttribute(name=LDAPUtilsConstants.E_DN /* dn */, required=true)
    private final String dn;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateLDAPEntryRequest() {
        this((String) null);
    }

    public CreateLDAPEntryRequest(String dn) {
        this.dn = dn;
    }

    public String getDn() { return dn; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("dn", dn);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
