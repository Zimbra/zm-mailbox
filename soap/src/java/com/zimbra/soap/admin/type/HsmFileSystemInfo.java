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

import com.zimbra.common.soap.HsmConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class HsmFileSystemInfo {

    /**
     * @zm-api-field-tag size
     * @zm-api-field-description Size
     */
    @XmlAttribute(name=HsmConstants.A_SIZE /* size */, required=true)
    private final String size;

    /**
     * @zm-api-field-tag filesystem
     * @zm-api-field-description Filesystem
     */
    @XmlValue
    private String fileSystem;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private HsmFileSystemInfo() {
        this((String) null);
    }

    public HsmFileSystemInfo(String size) {
        this.size = size;
    }

    public void setFileSystem(String fileSystem) { this.fileSystem = fileSystem; }
    public String getSize() { return size; }
    public String getFileSystem() { return fileSystem; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("size", size)
            .add("fileSystem", fileSystem);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
