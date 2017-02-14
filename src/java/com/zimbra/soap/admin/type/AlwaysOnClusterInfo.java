/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ALWAYSONCLUSTER)
@XmlType(propOrder = {})
public class AlwaysOnClusterInfo extends AdminObjectInfo {

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AlwaysOnClusterInfo() {
        this(null, null, null);
    }

    public AlwaysOnClusterInfo(String id, String name) {
        this(id, name, null);
    }

    public AlwaysOnClusterInfo(String id, String name, Collection <Attr> attrs) {
        super(id, name, attrs);
    }
}