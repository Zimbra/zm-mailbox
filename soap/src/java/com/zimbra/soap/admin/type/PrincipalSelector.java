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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AutoProvPrincipalBy;

@XmlAccessorType(XmlAccessType.NONE)
public class PrincipalSelector {

    /**
     * @zm-api-field-tag principal-selector-by
     * @zm-api-field-description Select the meaning of <b>{principal-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY /* by */, required=true)
    private AutoProvPrincipalBy by;

    /**
     * @zm-api-field-tag principal-selector-key
     * @zm-api-field-description The key used to identify the principal.
     * Meaning determined by <b>{principal-selector-by}</b>
     */
    @XmlValue
    private String key;

    private PrincipalSelector() {
    }

    private PrincipalSelector(AutoProvPrincipalBy by, String key) {
        setBy(by);
        setKey(key);
    }

    public static PrincipalSelector create(AutoProvPrincipalBy by, String key) {
        return new PrincipalSelector(by, key);
    }

    public void setBy(AutoProvPrincipalBy by) { this.by = by; }
    public void setKey(String key) { this.key = key; }
    public AutoProvPrincipalBy getBy() { return by; }
    public String getKey() { return key; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("by", by)
            .add("key", key);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
