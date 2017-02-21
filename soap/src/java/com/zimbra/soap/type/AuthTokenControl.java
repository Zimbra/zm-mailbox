/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.HeaderConstants;

@XmlAccessorType(XmlAccessType.NONE)
public final class AuthTokenControl {

    /**
     * @zm-api-field-tag voidOnExpired
     * @zm-api-field-description if set to true, expired authToken in the header will be ignored
     */
    @XmlAttribute(name=HeaderConstants.A_VOID_ON_EXPIRED/* voidOnExpired */, required=false)
    private ZmBoolean voidOnExpired;

    public AuthTokenControl() {
        voidOnExpired = ZmBoolean.FALSE;
    }

    public AuthTokenControl(Boolean voidExpired) {
        voidOnExpired = ZmBoolean.fromBool(voidExpired);
    }

    public void setVoidOnExpired(Boolean voidExpired) {
        voidOnExpired = ZmBoolean.fromBool(voidExpired);
    }

    public Boolean isVoidOnExpired() {
        return ZmBoolean.toBool(voidOnExpired);
    }
}
