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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Copy Class of service (COS)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_COPY_COS_REQUEST)
@XmlType(propOrder = {})
public class CopyCosRequest {

    /**
     * @zm-api-field-tag dest-cos-name
     * @zm-api-field-description Destination name for COS
     */
    @XmlElement(name=AdminConstants.E_NAME)
    private String newName;

    /**
     * @zm-api-field-description Source COS
     */
    @XmlElement(name=AdminConstants.E_COS)
    private CosSelector cos;

    public CopyCosRequest() {
    }

    public CopyCosRequest(CosSelector cos, String newName) {
        this.newName = newName;
        this.cos = cos;
    }

    public void setNewName(String name) {
        this.newName = name;
    }

    public void setCos(CosSelector cos) {
        this.cos = cos;
    }

    public String getNewName() { return newName; }
    public CosSelector getCos() { return cos; }
}
