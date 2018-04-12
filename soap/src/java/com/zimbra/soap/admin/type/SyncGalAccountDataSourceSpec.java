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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class SyncGalAccountDataSourceSpec {

    /**
     * @zm-api-field-tag datasource-by
     * @zm-api-field-description By - <b>id|name</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY /* by */, required=true)
    private String by;

    /**
     * @zm-api-field-tag full-sync
     * @zm-api-field-description If fullSync is set to <b>0 (false)</b> or unset the default behavior is trickle
     * sync which will pull in any new contacts or modified contacts since last sync.
     * <br />
     * If fullSync is set to <b>1 (true)</b>, then the server will go through all the contacts that appear in GAL,
     * and resolve deleted contacts in addition to new or modified ones.
     */
    @XmlAttribute(name=AdminConstants.A_FULLSYNC /* fullSync */, required=false)
    private ZmBoolean fullSync;

    /**
     * @zm-api-field-tag reset flag
     * @zm-api-field-description Reset flag.  If set, then all the contacts will be populated again, regardless of
     * the status since last sync.  Reset needs to be done when there is a significant change in the configuration,
     * such as filter, attribute map, or search base.
     */
    @XmlAttribute(name=AdminConstants.A_RESET /* reset */, required=false)
    private ZmBoolean reset;

    /**
     * @zm-api-field-tag key
     * @zm-api-field-description Key - meaning determined by <b>{datasource-by}</b>
     */
    @XmlValue
    private String value;

    public SyncGalAccountDataSourceSpec() {
    }

    private SyncGalAccountDataSourceSpec(String by, String value) {
        setBy(by);
        setValue(value);
    }

    public static SyncGalAccountDataSourceSpec createForByAndValue(String by, String value) {
        return new SyncGalAccountDataSourceSpec(by, value);
    }

    public void setBy(String by) { this.by = by; }
    public void setFullSync(Boolean fullSync) { this.fullSync = ZmBoolean.fromBool(fullSync); }
    public void setReset(Boolean reset) { this.reset = ZmBoolean.fromBool(reset); }
    public void setValue(String value) { this.value = value; }
    public String getBy() { return by; }
    public Boolean getFullSync() { return ZmBoolean.toBool(fullSync); }
    public Boolean getReset() { return ZmBoolean.toBool(reset); }
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("by", by)
            .add("fullSync", fullSync)
            .add("reset", reset)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
