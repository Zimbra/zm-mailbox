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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.SyncConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class SyncStateInfo {
    /**
     * @zm-api-field-tag syncState
     * @zm-api-field-description SyncState
     */
    @XmlAttribute(name = SyncConstants.E_SYNCSTATE /* syncState */, required = true)
    private final String syncState;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SyncStateInfo() {
        this((String) null);
    }

    public SyncStateInfo(String syncState) {
        this.syncState = syncState;
    }

    public String getSyncState() {
        return syncState;
    }
}
