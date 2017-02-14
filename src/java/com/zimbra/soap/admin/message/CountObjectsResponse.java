/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlRootElement(name = AdminConstants.E_COUNT_OBJECTS_RESPONSE)
public class CountObjectsResponse {

    /**
     * @zm-api-field-tag num-objects
     * @zm-api-field-description Number of objects of the requested type
     */
    @XmlAttribute(name = AdminConstants.A_NUM, required = true)
    private long num;

    @XmlAttribute(name = AdminConstants.A_TYPE, required = true)
    private String type;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public CountObjectsResponse() {
        this(0,"");
    }

    public CountObjectsResponse(long num, String type) {
        this.num = num;
        this.type = type;
    }

    public long getNum() {
        return num;
    }

    public String getType() {
        return type;
    }
}
