/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import org.json.JSONException;
import org.json.JSONObject;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;


import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
abstract class BaseExternalVolume {
    /**
     * @zm-api-field-description Set to 1 for Internal and 2 for External.
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_STORAGE_TYPE /* storageType */, required=false)
    private String storageType;

    public void setStorageType(String value) {
        storageType = value;
    }

    public String getStorageType() {
        return storageType;
    }

    public abstract JSONObject toJSON(VolumeInfo volumeInfo) throws JSONException;

}
