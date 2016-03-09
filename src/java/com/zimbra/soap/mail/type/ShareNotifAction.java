/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.zimbra.common.service.ServiceException;

@XmlEnum
public enum ShareNotifAction {
    @XmlEnumValue("edit") edit("edit"),
    @XmlEnumValue("revoke") revoke("revoke"),
    @XmlEnumValue("expire") expire("expire");
    private final String name;

    private ShareNotifAction(String name) {
        this.name = name;
    }

    public static ShareNotifAction fromString(String value) throws ServiceException {
        if (value == null) {
            return null;
        }
        try {
            return ShareNotifAction.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(ShareNotifAction.values()), null);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
