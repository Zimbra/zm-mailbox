/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.header;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class HeaderAccountInfo {

    /**
     * @zm-api-field-tag acct-selector-by
     * @zm-api-field-description Select the meaning of <b>{acct-selector-key}</b>
     */
    @XmlAttribute(name=HeaderConstants.A_BY /* by */, required=false)
    private String by;

    /**
     * @zm-api-field-tag mountpoint-traversed
     * @zm-api-field-description True if a mountpoint has been traversed.  Used to enforce the "cannot mount a
     * mountpoint" rule forbidding one link from pointing to another link.
     */
    @XmlAttribute(name=HeaderConstants.A_MOUNTPOINT /* link */, required=false)
    private ZmBoolean mountpointTraversed;

    /**
     * @zm-api-field-tag acct-selector-key
     * @zm-api-field-description The key used to identify the account. Meaning determined by <b>{acct-selector-by}</b>
     */
    @XmlValue
    private String value;

    public HeaderAccountInfo() {
    }

    public void setBy(String by) { this.by = by; }
    public void setMountpointTraversed(Boolean mountpointTraversed) { this.mountpointTraversed = ZmBoolean.fromBool(mountpointTraversed); }
    public void setValue(String value) { this.value = value; }
    public String getBy() { return by; }
    public Boolean getMountpointTraversed() { return ZmBoolean.toBool(mountpointTraversed); }
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("by", by)
            .add("mountpointTraversed", mountpointTraversed)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
