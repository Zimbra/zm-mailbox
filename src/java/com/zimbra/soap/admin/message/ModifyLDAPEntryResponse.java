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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.LDAPUtilsConstants;
import com.zimbra.soap.admin.type.LDAPEntryInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=LDAPUtilsConstants.E_MODIFY_LDAP_ENTRY_RESPONSE)
@XmlType(propOrder = {})
public class ModifyLDAPEntryResponse {

    /**
     * @zm-api-field-description Information about LDAP entry
     */
    @XmlElement(name=LDAPUtilsConstants.E_LDAPEntry /* LDAPEntry */, required=true)
    private final LDAPEntryInfo LDAPentry;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ModifyLDAPEntryResponse() {
        this((LDAPEntryInfo) null);
    }

    public ModifyLDAPEntryResponse(LDAPEntryInfo LDAPentry) {
        this.LDAPentry = LDAPentry;
    }

    public LDAPEntryInfo getLDAPentry() { return LDAPentry; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("LDAPentry", LDAPentry);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
