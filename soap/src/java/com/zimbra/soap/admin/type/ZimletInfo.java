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

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ZimletInfo extends AdminObjectInfo {

    /**
     * @zm-api-field-tag keyword
     * @zm-api-field-description Keyword
     */
    @XmlAttribute(name=AdminConstants.A_HAS_KEYWORD /* hasKeyword */, required=false)
    private final String hasKeyword;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZimletInfo() {
        this((String) null, (String) null, (Collection <Attr>) null,
                (String) null);
    }

    public ZimletInfo(String id, String name) {
        this(id, name, null, (String) null);
    }

    public ZimletInfo(String id, String name, Collection <Attr> attrs) {
        this(id, name, attrs, (String) null);
    }

    public ZimletInfo(String id, String name, Collection <Attr> attrs,
                    String hasKeyword) {
        super(id, name, attrs);
        this.hasKeyword = hasKeyword;
    }

    public String getHasKeyword() { return hasKeyword; }
}
