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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ZimletDeploymentStatus;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DEPLOY_ZIMLET_RESPONSE)
@XmlType(propOrder = {})
public class DeployZimletResponse {

    /**
     * @zm-api-field-description Progress information on deployment to servers
     */
    @XmlElement(name=AdminConstants.E_PROGRESS /* progress */, required=false)
    private List<ZimletDeploymentStatus> progresses = Lists.newArrayList();

    public DeployZimletResponse() {
    }

    public void setProgresses(Iterable <ZimletDeploymentStatus> progresses) {
        this.progresses.clear();
        if (progresses != null) {
            Iterables.addAll(this.progresses,progresses);
        }
    }

    public void addProgress(ZimletDeploymentStatus progress) {
        this.progresses.add(progress);
    }

    public List<ZimletDeploymentStatus> getProgresses() {
        return Collections.unmodifiableList(progresses);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("progresses", progresses);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
