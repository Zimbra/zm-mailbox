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

/**
 * @zm-api-command-description Rename LDAP Entry
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=LDAPUtilsConstants.E_RENAME_LDAP_ENTRIY_REQUEST)
public class RenameLDAPEntryRequest {

    /**
     * @zm-api-field-tag current-dn
     * @zm-api-field-description A valid LDAP DN String (RFC 2253) that identifies the LDAP object
     */
    @XmlAttribute(name=LDAPUtilsConstants.E_DN /* dn */, required=true)
    private final String dn;

    /**
     * @zm-api-field-tag new-dn
     * @zm-api-field-description New DN - a valid LDAP DN String (RFC 2253) that describes the new DN to be given to
     * the LDAP object
     */
    @XmlAttribute(name=LDAPUtilsConstants.E_NEW_DN /* new_dn */, required=true)
    private final String newDn;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RenameLDAPEntryRequest() {
        this((String) null, (String) null);
    }

    public RenameLDAPEntryRequest(String dn, String newDn) {
        this.dn = dn;
        this.newDn = newDn;
    }

    public String getDn() { return dn; }
    public String getNewDn() { return newDn; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("dn", dn)
            .add("newDn", newDn);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
