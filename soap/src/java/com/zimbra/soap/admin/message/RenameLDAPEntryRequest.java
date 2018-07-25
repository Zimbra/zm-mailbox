/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.LDAPUtilsConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("dn", dn)
            .add("newDn", newDn);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
