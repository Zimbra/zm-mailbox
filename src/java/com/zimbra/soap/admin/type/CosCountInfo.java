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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

/**
 * Used by {@CountAccountResponse}
 */
@XmlAccessorType(XmlAccessType.NONE)
public class CosCountInfo {

    /**
     * @zm-api-field-tag cos-name
     * @zm-api-field-description Class Of Service (COS) name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-tag cos-id
     * @zm-api-field-description Class Of Service (COS) ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-tag account-count
     * @zm-api-field-description Account count.  Note, it doesn't include any account with
     * <b>zimbraIsSystemResource=TRUE</b>, nor does it include any calendar resources.
     */
    @XmlValue
    private final long value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CosCountInfo() {
        this(null, null, 0);
    }

    public CosCountInfo(String id, String name, long value) {
        this.name = name;
        this.id = id;
        this.value = value;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public long getValue() { return value; }
}
