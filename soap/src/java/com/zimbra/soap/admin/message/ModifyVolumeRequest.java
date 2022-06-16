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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.VolumeInfo;
import com.zimbra.soap.admin.type.VolumeExternalInfo;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Modify volume
 */
@XmlRootElement(name=AdminConstants.E_MODIFY_VOLUME_REQUEST)
public class ModifyVolumeRequest {

    /**
     * @zm-api-field-tag volume-id
     * @zm-api-field-description Volume ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private short id;

    /**
     * @zm-api-field-description Volume information
     */
    @XmlElement(name=AdminConstants.E_VOLUME, required=true)
    private VolumeInfo volume;

    /**
     * @zm-api-field-description Volume external information
     */
    @XmlElement(name=AdminConstants.E_VOLUME_EXT, required=true)
    private VolumeExternalInfo volumeExternal;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ModifyVolumeRequest() {
        this((short)-1, (VolumeInfo)null, (VolumeExternalInfo)null);
    }

    public ModifyVolumeRequest(short id, VolumeInfo volume, VolumeExternalInfo volumeExternal) {
        this.id = id;
        this.volume = volume;
        this.volumeExternal = volumeExternal;
    }

    public short getId() { return id; }
    public VolumeInfo getVolume() { return volume; }
    public VolumeExternalInfo getVolumeExternal() { return volumeExternal; }

}
