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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

@XmlAccessorType(XmlAccessType.NONE)
public class DataSourceSpecifier extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag data-source-type
     * @zm-api-field-description Data source type
     */
    @XmlAttribute(name=AccountConstants.A_TYPE, required=true)
    private final DataSourceType type;

    /**
     * @zm-api-field-tag data-source-name
     * @zm-api-field-description Data source name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DataSourceSpecifier() {
        this((DataSourceType) null, (String) null);
    }

    public DataSourceSpecifier(DataSourceType type, String name) {
        this.type = type;
        this.name = name;
    }

    public DataSourceType getType() { return type; }
    public String getName() { return name; }
}
