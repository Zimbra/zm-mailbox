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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class SyncGalAccountDataSourceSpec {

    @XmlAttribute(name=AdminConstants.A_BY /* by */, required=true)
    private String by;

    @XmlAttribute(name=AdminConstants.A_FULLSYNC /* fullSync */, required=false)
    private ZmBoolean fullSync;

    @XmlAttribute(name=AdminConstants.A_RESET /* reset */, required=false)
    private ZmBoolean reset;

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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("by", by)
            .add("fullSync", fullSync)
            .add("reset", reset)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
