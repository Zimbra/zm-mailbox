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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class VersionCheckInfo {

    @XmlAttribute(name=AdminConstants.A_VERSION_CHECK_STATUS, required=false)
    private ZmBoolean status;

    @XmlElementWrapper(name=AdminConstants.E_UPDATES, required=false)
    @XmlElement(name=AdminConstants.E_UPDATE, required=false)
    private List<VersionCheckUpdateInfo> updates = Lists.newArrayList();

    public VersionCheckInfo() {
    }

    public void setStatus(Boolean status) { this.status = ZmBoolean.fromBool(status); }
    public void setUpdates(Iterable <VersionCheckUpdateInfo> updates) {
        this.updates.clear();
        if (updates != null) {
            Iterables.addAll(this.updates,updates);
        }
    }

    public void addUpdate(VersionCheckUpdateInfo update) {
        this.updates.add(update);
    }

    public Boolean getStatus() { return ZmBoolean.toBool(status); }
    public List<VersionCheckUpdateInfo> getUpdates() {
        return Collections.unmodifiableList(updates);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("status", status)
            .add("updates", updates);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
