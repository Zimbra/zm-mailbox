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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.LDAPUtilsConstants;
import com.zimbra.soap.admin.type.LDAPEntryInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=LDAPUtilsConstants.E_GET_LDAP_ENTRIES_RESPONSE)
@XmlType(propOrder = {})
public class GetLDAPEntriesResponse {

    /**
     * @zm-api-field-description LDAP entries
     */
    @XmlElement(name=LDAPUtilsConstants.E_LDAPEntry /* LDAPEntry */, required=false)
    private List<LDAPEntryInfo> LDAPentries = Lists.newArrayList();

    public GetLDAPEntriesResponse() {
    }

    public void setLDAPentries(Iterable <LDAPEntryInfo> LDAPentries) {
        this.LDAPentries.clear();
        if (LDAPentries != null) {
            Iterables.addAll(this.LDAPentries,LDAPentries);
        }
    }

    public void addLDAPentry(LDAPEntryInfo LDAPentry) {
        this.LDAPentries.add(LDAPentry);
    }

    public List<LDAPEntryInfo> getLDAPentries() {
        return Collections.unmodifiableList(LDAPentries);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("LDAPentries", LDAPentries);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
