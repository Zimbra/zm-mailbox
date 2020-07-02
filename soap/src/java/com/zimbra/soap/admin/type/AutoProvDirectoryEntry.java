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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class AutoProvDirectoryEntry extends AdminKeyValuePairs {

    /**
     * @zm-api-field-tag dn
     * @zm-api-field-description DN
     */
    @XmlAttribute(name=AdminConstants.A_DN /* dn */, required=true)
    private String dn;

    /**
     * @zm-api-field-description Keys
     */
    @XmlElement(name=AdminConstants.E_KEY /* key */, required=false)
    private List<String> keys = Lists.newArrayList();

    public AutoProvDirectoryEntry() {
    }

    public void setDn(String dn) { this.dn = dn; }
    public void setKeys(Iterable <String> keys) {
        this.keys.clear();
        if (keys != null) {
            Iterables.addAll(this.keys,keys);
        }
    }

    public void addKey(String key) {
        this.keys.add(key);
    }

    public String getDn() { return dn; }
    public List<String> getKeys() {
        return Collections.unmodifiableList(keys);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("dn", dn)
            .add("keys", keys);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
