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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("dn", dn)
            .add("keys", keys);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
