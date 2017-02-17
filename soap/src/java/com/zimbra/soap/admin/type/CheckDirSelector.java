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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class CheckDirSelector {

    /**
     * @zm-api-field-tag full-path
     * @zm-api-field-description Full path to the directory
     */
    @XmlAttribute(name=AdminConstants.A_PATH, required=true)
    private final String path;

    /**
     * @zm-api-field-tag create-if-nec-flag
     * @zm-api-field-description Whether to create the directory or not if it doesn't exist
     */
    @XmlAttribute(name=AdminConstants.A_CREATE, required=false)
    private final ZmBoolean create;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckDirSelector() {
        this((String)null, (Boolean) null);
    }

    public CheckDirSelector(String path) {
        this(path, (Boolean) null);
    }

    public CheckDirSelector(String path, Boolean create) {
        this.path = path;
        this.create = ZmBoolean.fromBool(create);
    }

    public String getPath() { return path; }
    public Boolean isCreate() { return ZmBoolean.toBool(create); }
}
