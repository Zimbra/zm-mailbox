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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class VersionCheckInfo {

    /**
     * @zm-api-field-tag version-check-status
     * @zm-api-field-description Version check status
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_CHECK_STATUS /* status */, required=false)
    private ZmBoolean status;

    /**
     * @zm-api-field-description Version check update information
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_UPDATES /* updates */, required=false)
    @XmlElement(name=AdminConstants.E_UPDATE /* update */, required=false)
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("status", status)
            .add("updates", updates);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
