/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class PhoneSpec {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag prefs-spec
     * @zm-api-field-description Preferences specification
     * <br />
     * If no <b>&lt;pref></b> elements are provided, all known prefs for the requested phone are returned in the response.
     * If <b>&lt;pref></b> elements are provided, only those prefs are returned in the response.
     */
    @XmlElement(name=AccountConstants.E_PREF /* pref */, required=false)
    private List<PrefSpec> prefs = Lists.newArrayList();

    public PhoneSpec() {
    }

    public void setName(String name) { this.name = name; }
    public void setPrefs(Iterable <PrefSpec> prefs) {
        this.prefs.clear();
        if (prefs != null) {
            Iterables.addAll(this.prefs, prefs);
        }
    }

    public void addPref(PrefSpec pref) {
        this.prefs.add(pref);
    }

    public String getName() { return name; }
    public List<PrefSpec> getPrefs() {
        return prefs;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("prefs", prefs);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
