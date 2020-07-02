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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("LDAPentries", LDAPentries);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
