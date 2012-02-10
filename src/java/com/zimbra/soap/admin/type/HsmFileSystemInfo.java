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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("size", size)
            .add("fileSystem", fileSystem);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
