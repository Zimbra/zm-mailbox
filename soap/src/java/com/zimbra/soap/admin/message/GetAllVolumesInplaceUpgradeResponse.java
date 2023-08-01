/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.VolumeInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_VOLUMES_INPLACE_UPGRADE_RESPONSE)
public final class GetAllVolumesInplaceUpgradeResponse {

    /**
     * @zm-api-field-description Information about volumes
     */
    @XmlElement(name=AdminConstants.E_VOLUME, required=true)
    private final List <VolumeInfo> volumes = Lists.newArrayList();

    public void setVolumes(Collection<VolumeInfo> list) {
        volumes.clear();
        if (list != null) {
            volumes.addAll(list);
        }
    }

    public void addVolume(VolumeInfo volume) {
        volumes.add(volume);
    }

    public List<VolumeInfo> getVolumes() {
        return Collections.unmodifiableList(volumes);
    }
}
